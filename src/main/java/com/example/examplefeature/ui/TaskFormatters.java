package com.example.examplefeature.ui;

import com.example.examplefeature.Task;
import com.example.examplefeature.TaskPriority;
import com.example.examplefeature.TaskStatus;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Pomocne metode za prikaz taskova: formatiranje datuma i obojeni {@link Badge}-ovi za status i prioritet.
 * <p>
 * Izdvojeno u posebnu klasu jer ih koriste i grid ({@link TaskListView}) i forma ({@link TaskEditorDialog}).
 * U Fazi 3 ce ih koristiti i Kanban kartice.
 */
final class TaskFormatters {

    private TaskFormatters() {
    }

    static String formatDue(Task task, Locale locale) {
        var due = task.getDueDate();
        return due == null ? "Never" : DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(due);
    }

    static String formatCreated(Task task, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(task.getCreationDate());
    }

    /**
     * {@link Badge} je Vaadin komponenta za kratke oznake. Boju biras <i>theme variant</i>-om
     * ({@code SUCCESS}, {@code WARNING}, {@code ERROR}, {@code CONTRAST}...), umesto da sama pises CSS.
     */
    static Badge statusBadge(TaskStatus status) {
        var badge = new Badge(status.getLabel());
        switch (status) {
            case TODO -> badge.addThemeVariants(BadgeVariant.CONTRAST);
            case IN_PROGRESS -> { /* podrazumevana (primarna) boja */ }
            case DONE -> badge.addThemeVariants(BadgeVariant.SUCCESS);
        }
        return badge;
    }

    static Badge priorityBadge(TaskPriority priority) {
        var badge = new Badge(priority.getLabel());
        switch (priority) {
            case LOW -> badge.addThemeVariants(BadgeVariant.CONTRAST);
            case MEDIUM -> { }
            case HIGH -> badge.addThemeVariants(BadgeVariant.WARNING);
            case CRITICAL -> badge.addThemeVariants(BadgeVariant.ERROR, BadgeVariant.FILLED);
        }
        return badge;
    }
}
