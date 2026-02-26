package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReportResponseDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<KpiDTO> kpis = new ArrayList<>();
    private List<ChartPointDTO> chartData = new ArrayList<>();
    private List<?> tableRows = new ArrayList<>();
    private LocalDateTime generatedAt;

    public List<KpiDTO> getKpis() {
        return kpis;
    }

    public void setKpis(List<KpiDTO> kpis) {
        this.kpis = kpis == null ? new ArrayList<>() : new ArrayList<>(kpis);
    }

    public List<ChartPointDTO> getChartData() {
        return chartData;
    }

    public void setChartData(List<ChartPointDTO> chartData) {
        this.chartData = chartData == null ? new ArrayList<>() : new ArrayList<>(chartData);
    }

    public List<?> getTableRows() {
        return tableRows;
    }

    public void setTableRows(List<?> tableRows) {
        this.tableRows = tableRows == null ? new ArrayList<>() : new ArrayList<>(tableRows);
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
