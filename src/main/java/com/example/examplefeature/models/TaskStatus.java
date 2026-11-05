package com.example.examplefeature.models;

import com.example.examplefeature.enitities.Task;

/**
 * Status taska, kao kolone na Jira tabli.
 * <p>
 * U bazi se cuva kao tekst (vidi {@code @Enumerated(EnumType.STRING)} u {@link Task}), pa redosled konstanti mozes
 * menjati bez kvarenja postojecih podataka.
 */
public enum TaskStatus {

    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    /**
     * Citljiv naziv za UI. Koristimo ga u {@code ComboBox.setItemLabelGenerator(TaskStatus::getLabel)}, inace bi
     * Vaadin prikazao {@code toString()}, tj. "IN_PROGRESS".
     */
    public String getLabel() {
        return label;
    }
}
