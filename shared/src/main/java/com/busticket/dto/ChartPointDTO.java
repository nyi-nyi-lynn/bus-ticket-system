package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;

public class ChartPointDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String label;
    private double value;

    public ChartPointDTO() {
    }

    public ChartPointDTO(String label, double value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
