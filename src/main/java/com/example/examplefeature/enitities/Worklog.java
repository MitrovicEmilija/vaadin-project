package com.example.examplefeature.enitities;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "worklog")
public class Worklog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "worklog_id")
    private Long id;

    @ManyToOne(optional = false)
    private Task task;

    @Column(name = "work_date")
    private LocalDate workDate;

    @Column(name = "minutes")
    private Integer minutes;

    @Column(name = "comment")
    private String comment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
