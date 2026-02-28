USE bus_ticket;

ALTER TABLE buses
    ADD COLUMN IF NOT EXISTS bus_name VARCHAR(120) NULL AFTER bus_number;

UPDATE buses
SET bus_name = bus_number
WHERE bus_name IS NULL OR TRIM(bus_name) = '';

ALTER TABLE buses
    MODIFY COLUMN bus_name VARCHAR(120) NOT NULL;
