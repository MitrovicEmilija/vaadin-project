package com.example.examplefeature.repositories;

import com.example.examplefeature.enitities.Task;
import com.example.examplefeature.models.TaskFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.Locale;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // If you don't need a total row count, Slice is better than Page as it only performs a select query.
    // Page performs both a select and a count query.
    Slice<Task> findAllBy(Pageable pageable);

    /**
     * Pravi JPA {@link Specification} (dinamicki WHERE uslov) od filtera iz UI-ja.
     * <p>
     * Zasto Specification, a ne vise metoda tipa {@code findByStatusAndPriority...}? Zato sto je svaki kriterijum
     * opcion, pa bi kombinacija bilo previse. Ovde samo dodajemo uslove koji su zadati. Za to repozitorijum mora da
     * nasledi {@link JpaSpecificationExecutor} (vec nasledjuje).
     */
    static Specification<Task> matching(TaskFilter filter) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            var text = filter.searchText();
            if (text != null && !text.isBlank()) {
                var pattern = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("jiraKey")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("notes")), pattern)));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.priority()));
            }
            // and() bez argumenata je "uvek tacno", pa prazan filter vraca sve taskove.
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
