package com.example.examplefeature.repositories;

import com.example.examplefeature.enitities.Task;
import com.example.examplefeature.enitities.Worklog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkLogRepository extends JpaRepository<Worklog, Long> {

    List<Worklog> findByTaskOrderByWorkDateDesc(Task task);
}
