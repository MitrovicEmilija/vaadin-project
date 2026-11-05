package com.example.examplefeature.models;

/**
 * Prioritet taska. Redosled konstanti je od najnizeg ka najvisem, pa sortiranje po {@code ordinal()} ima smisla.
 */
public enum TaskPriority {

    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    private final String label;

    TaskPriority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
