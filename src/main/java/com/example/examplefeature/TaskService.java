package com.example.examplefeature;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Poslovni sloj izmedju UI-ja i baze. Vaadin view-ovi nikad ne pricaju direktno sa repozitorijumom, nego idu preko
 * servisa. Ovde su transakcije, a kasnije i provera prava pristupa (Faza 6, Spring Security).
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /** Brzo dodavanje iz trake iznad grida: samo opis i rok, ostalo dobija podrazumevane vrednosti. */
    @Transactional
    public Task createTask(String description, @Nullable LocalDate dueDate) {
        var task = new Task(description, Instant.now());
        task.setDueDate(dueDate);
        return taskRepository.saveAndFlush(task);
    }

    /** Prazan task za formu "New task". Jos nije u bazi; tamo ulazi tek kroz {@link #save(Task)}. */
    public Task newTask() {
        return new Task("", Instant.now());
    }

    /**
     * Cuva nov ili izmenjen task. {@code save} radi i INSERT i UPDATE: ako entitet ima id, Spring Data radi
     * {@code merge}. Vraca sacuvanu instancu (za nov task, sa dodeljenim id-jem).
     */
    @Transactional
    public Task save(Task task) {
        return taskRepository.saveAndFlush(task);
    }

    @Transactional
    public void delete(Task task) {
        if (task.getId() != null) {
            taskRepository.deleteById(task.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<Task> list(Pageable pageable) {
        return taskRepository.findAllBy(pageable).toList();
    }

    /**
     * Jedna "stranica" taskova koji odgovaraju filteru. Grid je zove sam, svaki put kad mu treba novi deo podataka
     * (skrolovanje, sortiranje, {@code refreshAll()}), i prosledi offset, limit i sortiranje kroz {@code Pageable}.
     */
    @Transactional(readOnly = true)
    public List<Task> list(Pageable pageable, TaskFilter filter) {
        return taskRepository.findAll(TaskRepository.matching(filter), pageable).toList();
    }
}
