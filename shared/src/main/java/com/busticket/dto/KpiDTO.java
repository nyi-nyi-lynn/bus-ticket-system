package com.busticket.dto;

import java.io.Serial;
import java.io.Serializable;

public class KpiDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String label;
    private String value;
    private String subtitle;

    public KpiDTO() {
    }

    public KpiDTO(String label, String value, String subtitle) {
        this.label = label;
        this.value = value;
        this.subtitle = subtitle;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
}
