package com.busticket.service.impl;

import com.busticket.dao.BusDAO;
import com.busticket.dao.UserDAO;
import com.busticket.dao.impl.BusDAOImpl;
import com.busticket.dao.impl.UserDAOImpl;
import com.busticket.database.DatabaseConnection;
import com.busticket.dto.BusDTO;
import com.busticket.dto.CreateBusRequest;
import com.busticket.dto.UpdateBusRequest;
import com.busticket.enums.BusType;
import com.busticket.enums.Role;
import com.busticket.enums.UserStatus;
import com.busticket.exception.DuplicateResourceException;
import com.busticket.exception.ValidationException;
import com.busticket.model.Bus;
import com.busticket.model.User;
import com.busticket.service.BusService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class BusServiceImpl implements BusService {
    private static final Pattern BUS_NUMBER_PATTERN = Pattern.compile("^[A-Z0-9-]{3,20}$");

    private final BusDAO busDAO;
    private final UserDAO userDAO;

    public BusServiceImpl() {
        busDAO = new BusDAOImpl(DatabaseConnection.getConnection());
        userDAO = new UserDAOImpl(DatabaseConnection.getConnection());
    }

    @Override
    public BusDTO createBus(CreateBusRequest request) throws DuplicateResourceException, ValidationException {
        validateCreateRequest(request);
        ensureAdminActor(request.getRequestedByUserId());

        String busNumber = normalizeBusNumber(request.getBusNumber());
        BusType busType = parseTypeStrict(request.getBusType());
        boolean active = parseStatusStrictOrDefault(request.getStatus(), true);
        if (busDAO.existsByBusNumber(busNumber)) {
            throw new DuplicateResourceException("BUS_NUMBER_EXISTS");
        }

        Bus bus = new Bus();
        bus.setBusNumber(busNumber);
        bus.setBusName(normalizeText(request.getBusName()));
        bus.setType(busType);
        bus.setTotalSeats(request.getTotalSeats());
        bus.setActive(active);

        Bus created = busDAO.insert(bus);
        if (created == null) {
            throw new ValidationException("CREATE_BUS_FAILED");
        }
        return toDTO(created);
    }

    @Override
    public BusDTO updateBus(UpdateBusRequest request) throws DuplicateResourceException, ValidationException {
        validateUpdateRequest(request);
        ensureAdminActor(request.getRequestedByUserId());

        Bus existing = busDAO.findById(request.getBusId());
        if (existing == null) {
            throw new ValidationException("BUS_NOT_FOUND");
        }

        String busNumber = normalizeBusNumber(request.getBusNumber());
        BusType busType = parseTypeStrict(request.getBusType());
        boolean active = parseStatusStrictOrDefault(request.getStatus(), existing.isActive());
        if (busDAO.existsByBusNumberExceptId(busNumber, request.getBusId())) {
            throw new DuplicateResourceException("BUS_NUMBER_EXISTS");
        }

        Bus bus = new Bus();
        bus.setBusId(request.getBusId());
        bus.setBusNumber(busNumber);
        bus.setBusName(normalizeText(request.getBusName()));
        bus.setType(busType);
        bus.setTotalSeats(request.getTotalSeats());
        bus.setActive(active);

        Bus updated = busDAO.updateRecord(bus);
        if (updated == null) {
            throw new ValidationException("UPDATE_BUS_FAILED");
        }
        return toDTO(updated);
    }

    @Override
    public void deleteBus(Long busId) throws ValidationException {
        if (busId == null) {
            throw new ValidationException("BUS_ID_REQUIRED");
        }
        if (busDAO.findById(busId) == null) {
            throw new ValidationException("BUS_NOT_FOUND");
        }
        if (busDAO.hasTrips(busId)) {
            throw new ValidationException("BUS_IN_USE");
        }
        if (!busDAO.deleteById(busId)) {
            throw new ValidationException("DELETE_BUS_FAILED");
        }
    }

    @Override
    public List<BusDTO> getAllBuses() {
        return getAll();
    }

    @Override
    public boolean save(BusDTO dto) {
        if (dto == null) {
            return false;
        }
        String busNumber = normalizeBusNumber(dto.getBusNumber());
        if (busNumber.isBlank() || !BUS_NUMBER_PATTERN.matcher(busNumber).matches()) {
            return false;
        }
        if (dto.getTotalSeats() <= 0) {
            return false;
        }
        if (busDAO.existsByBusNumber(busNumber)) {
            return false;
        }
        Bus bus = toModelLegacy(dto);
        bus.setBusNumber(busNumber);
        return busDAO.save(bus);
    }

    @Override
    public boolean update(BusDTO dto) {
        if (dto == null || dto.getBusId() == null) {
            return false;
        }
        String busNumber = normalizeBusNumber(dto.getBusNumber());
        if (busNumber.isBlank() || !BUS_NUMBER_PATTERN.matcher(busNumber).matches()) {
            return false;
        }
        if (dto.getTotalSeats() <= 0) {
            return false;
        }
        if (busDAO.existsByBusNumberExceptId(busNumber, dto.getBusId())) {
            return false;
        }
        Bus bus = toModelLegacy(dto);
        bus.setBusNumber(busNumber);
        return busDAO.update(bus);
    }

    @Override
    public boolean deactivate(Long id) {
        return busDAO.deactivate(id);
    }

    @Override
    public List<BusDTO> getAll() {
        List<Bus> buses = busDAO.findAll();
        List<BusDTO> dtos = new ArrayList<>();
        for (Bus bus : buses) {
            dtos.add(toDTO(bus));
        }
        return dtos;
    }

    private Bus toModelLegacy(BusDTO dto) {
        Bus bus = new Bus();
        bus.setBusId(dto.getBusId());
        bus.setBusNumber(normalizeBusNumber(dto.getBusNumber()));
        bus.setBusName(normalizeText(dto.getBusName()));
        bus.setType(parseTypeOrDefault(dto.getType(), BusType.NORMAL));
        bus.setTotalSeats(dto.getTotalSeats());
        bus.setActive(!"INACTIVE".equalsIgnoreCase(normalizeText(dto.getStatus())));
        return bus;
    }

    private BusDTO toDTO(Bus bus) {
        BusDTO dto = new BusDTO();
        dto.setBusId(bus.getBusId());
        dto.setBusNumber(bus.getBusNumber());
        dto.setBusName(bus.getBusName() == null || bus.getBusName().isBlank() ? bus.getBusNumber() : bus.getBusName());
        dto.setType(bus.getType() == null ? null : bus.getType().name());
        dto.setStatus(bus.isActive() ? "ACTIVE" : "INACTIVE");
        dto.setTotalSeats(bus.getTotalSeats());
        return dto;
    }

    private BusType parseTypeStrict(String type) throws ValidationException {
        if (type == null || type.isBlank()) {
            throw new ValidationException("BUS_TYPE_REQUIRED");
        }
        try {
            return BusType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("INVALID_BUS_TYPE");
        }
    }

    private void validateCreateRequest(CreateBusRequest request) throws ValidationException {
        if (request == null) {
            throw new ValidationException("INVALID_REQUEST");
        }
        if (request.getRequestedByUserId() == null) {
            throw new ValidationException("MISSING_ADMIN_CONTEXT");
        }
        validateBusNumber(request.getBusNumber());
        validateBusName(request.getBusName());
        validateSeats(request.getTotalSeats());
        parseTypeStrict(request.getBusType());
        parseStatusStrictOrDefault(request.getStatus(), true);
    }

    private void validateUpdateRequest(UpdateBusRequest request) throws ValidationException {
        if (request == null) {
            throw new ValidationException("INVALID_REQUEST");
        }
        if (request.getBusId() == null) {
            throw new ValidationException("BUS_ID_REQUIRED");
        }
        if (request.getRequestedByUserId() == null) {
            throw new ValidationException("MISSING_ADMIN_CONTEXT");
        }
        validateBusNumber(request.getBusNumber());
        validateBusName(request.getBusName());
        validateSeats(request.getTotalSeats());
        parseTypeStrict(request.getBusType());
        parseStatusStrictOrDefault(request.getStatus(), true);
    }

    private void validateBusNumber(String busNumber) throws ValidationException {
        String normalized = normalizeBusNumber(busNumber);
        if (normalized.isBlank()) {
            throw new ValidationException("BUS_NUMBER_REQUIRED");
        }
        if (!BUS_NUMBER_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("INVALID_BUS_NUMBER_FORMAT");
        }
    }

    private void validateBusName(String busName) throws ValidationException {
        if (normalizeText(busName).isBlank()) {
            throw new ValidationException("BUS_NAME_REQUIRED");
        }
    }

    private void validateSeats(Integer totalSeats) throws ValidationException {
        if (totalSeats == null || totalSeats <= 0) {
            throw new ValidationException("INVALID_TOTAL_SEATS");
        }
    }

    private void ensureAdminActor(Long requestedByUserId) throws ValidationException {
        User actor = userDAO.findById(requestedByUserId);
        if (actor == null || actor.getRole() != Role.ADMIN || actor.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException("FORBIDDEN_ONLY_ADMIN");
        }
    }

    private boolean parseStatusStrictOrDefault(String status, boolean fallback) throws ValidationException {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ACTIVE", "1" -> true;
            case "INACTIVE", "0" -> false;
            default -> throw new ValidationException("INVALID_STATUS");
        };
    }

    private BusType parseTypeOrDefault(String type, BusType fallback) {
        if (type == null || type.isBlank()) {
            return fallback;
        }
        try {
            return BusType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private String normalizeBusNumber(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
