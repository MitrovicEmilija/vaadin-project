package com.example.examplefeature.enitities;

import com.example.examplefeature.models.TaskPriority;
import com.example.examplefeature.models.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA entitet za jedan radni task.
 * <p>
 * <b>Kako se ovo povezuje sa Vaadin-om:</b> anotacije iz {@code jakarta.validation} ({@code @NotBlank},
 * {@code @Size}, {@code @Pattern}) rade na dva mesta istovremeno:
 * <ol>
 * <li>u formi, jer {@code BeanValidationBinder} (vidi {@code TaskEditorDialog}) ih cita i prikazuje gresku ispod
 * polja cim korisnik unese nesto pogresno;</li>
 * <li>u bazi, jer Hibernate pre {@code INSERT}/{@code UPDATE} ponovo proverava ista pravila.</li>
 * </ol>
 * Pravila pises jednom, na entitetu, a vaze svuda.
 * <p>
 * Binder pristupa poljima preko getter/setter parova, pa svako polje koje zelis u formi mora imati oba.
 */
@Entity
@Table(name = "task")
public class Task {

    public static final int DESCRIPTION_MAX_LENGTH = 300;
    public static final int NOTES_MAX_LENGTH = 4000;
    public static final int JIRA_KEY_MAX_LENGTH = 32;

    /** Format Jira kljuca: veliko slovo, pa slova/cifre, crtica i broj. Npr. {@code PROJ-123} ili {@code AB2-7}. */
    public static final String JIRA_KEY_REGEX = "[A-Z][A-Z0-9]*-\\d+";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "task_id")
    private Long id;

    // @Pattern ne proverava null vrednost, pa je Jira kljuc opcion; ali ako je unet, mora biti u ispravnom formatu.
    @Pattern(regexp = JIRA_KEY_REGEX, message = "Use the Jira format, e.g. PROJ-123")
    @Size(max = JIRA_KEY_MAX_LENGTH)
    @Column(name = "jira_key", length = JIRA_KEY_MAX_LENGTH)
    @Nullable
    private String jiraKey;

    @NotBlank(message = "Description is required")
    @Size(max = DESCRIPTION_MAX_LENGTH)
    @Column(name = "description", nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description = "";

    @Size(max = NOTES_MAX_LENGTH)
    @Column(name = "notes", length = NOTES_MAX_LENGTH)
    @Nullable
    private String notes;

    // EnumType.STRING cuva "IN_PROGRESS" umesto broja 1. Mnogo je sigurnije ako kasnije dodas novu vrednost.
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "creation_date", nullable = false)
    private Instant creationDate;

    @Column(name = "due_date")
    @Nullable
    private LocalDate dueDate;

    protected Task() { // To keep Hibernate happy
    }

    public Task(String description, Instant creationDate) {
        setDescription(description);
        this.creationDate = creationDate;
    }

    public @Nullable Long getId() {
        return id;
    }

    public @Nullable String getJiraKey() {
        return jiraKey;
    }

    public void setJiraKey(@Nullable String jiraKey) {
        this.jiraKey = jiraKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("Description length exceeds " + DESCRIPTION_MAX_LENGTH);
        }
        this.description = description;
    }

    public @Nullable String getNotes() {
        return notes;
    }

    public void setNotes(@Nullable String notes) {
        this.notes = notes;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public @Nullable LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(@Nullable LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /** Rok je prosao, a task jos nije zavrsen. UI ovo koristi da oboji datum u crveno. */
    public boolean isOverdue(LocalDate today) {
        return dueDate != null && status != TaskStatus.DONE && dueDate.isBefore(today);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        Task other = (Task) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        // Hashcode should never change during the lifetime of an object. Because of
        // this we can't use getId() to calculate the hashcode. Unless you have sets
        // with lots of entities in them, returning the same hashcode should not be a
        // problem.
        return getClass().hashCode();
    }
}
