package com.example.examplefeature.services;

import com.example.examplefeature.enitities.Task;
import com.example.examplefeature.enitities.Worklog;
import com.example.examplefeature.repositories.WorkLogRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class WorkLogService {

    private final WorkLogRepository workLogRepository;

    public WorkLogService(WorkLogRepository workLogRepository) {
        this.workLogRepository = workLogRepository;
    }

    public Worklog newWorkLog() {
        return new Worklog();
    }

    @Transactional
    public Worklog logWork(Task task, LocalDate date, Integer minutes, String comment) {
        var worklog = new Worklog();
        worklog.setTask(task);
        worklog.setWorkDate(date);
        worklog.setMinutes(minutes);
        worklog.setComment(comment);
        return workLogRepository.saveAndFlush(worklog);
    }

    public List<Worklog> listForTask(Task task) {
        return workLogRepository.findByTaskOrderByWorkDateDesc(task);
    }

    public void deleteWorkLog(Worklog worklog) {
        if (worklog.getId() != null) {
            workLogRepository.deleteById(worklog.getId());
        }
    }

    @Query("SELECT coalesce(sum(w.minutes), 0) FROM Worklog w WHERE w.task = :task ")
    public Integer getTotalLoggedMinutes(Task task) {
        return workLogRepository.findByTaskOrderByWorkDateDesc(task).stream()
                .mapToInt(Worklog::getMinutes)
                .sum();
    }
}
