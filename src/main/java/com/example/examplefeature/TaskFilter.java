package com.example.examplefeature;

import org.jspecify.annotations.Nullable;

/**
 * Kriterijumi pretrage iz filter-trake iznad grida. Svako polje je opciono: {@code null} znaci "ne filtriraj po
 * ovome".
 *
 * @param searchText trazi se u Jira kljucu, opisu i beleskama (bez obzira na velika/mala slova)
 * @param status     samo taskovi sa ovim statusom
 * @param priority   samo taskovi sa ovim prioritetom
 */
public record TaskFilter(@Nullable String searchText, @Nullable TaskStatus status, @Nullable TaskPriority priority) {

    /** Filter koji propusta sve taskove. */
    public static TaskFilter empty() {
        return new TaskFilter(null, null, null);
    }
}
