package com.example.examplefeature;

import com.example.examplefeature.enitities.Task;
import com.example.examplefeature.models.TaskFilter;
import com.example.examplefeature.models.TaskPriority;
import com.example.examplefeature.models.TaskStatus;
import com.example.examplefeature.services.TaskService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class TaskServiceTest {

    @Autowired
    TaskService taskService;

    @Test
    public void tasks_are_stored_in_the_database_with_the_current_timestamp() {
        var now = Instant.now();
        var desc = "Do this";
        var due = LocalDate.of(2025, 2, 7);
        taskService.createTask(desc, due);

        var task = taskService.list(PageRequest.ofSize(1)).get(0);
        assertThat(task.getDescription()).isEqualTo(desc);
        assertThat(task.getDueDate()).isEqualTo(due);
        assertThat(task.getCreationDate()).isAfterOrEqualTo(now);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    public void tasks_are_validated_before_they_are_stored() {
        assertThatThrownBy(() -> taskService.createTask("X".repeat(Task.DESCRIPTION_MAX_LENGTH + 1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void bean_validation_rejects_invalid_jira_key_on_save() {
        // Ista @Pattern provera koja radi u formi stiti i bazu, ako neko zaobidje UI.
        var task = taskService.newTask();
        task.setDescription("Valid description");
        task.setJiraKey("bad key");
        assertThatThrownBy(() -> taskService.save(task)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    public void filter_combines_text_status_and_priority() {
        var a = taskService.newTask();
        a.setDescription("Deploy to staging");
        a.setJiraKey("OPS-1");
        a.setStatus(TaskStatus.IN_PROGRESS);
        a.setPriority(TaskPriority.HIGH);
        taskService.save(a);

        var b = taskService.newTask();
        b.setDescription("Deploy to production");
        b.setNotes("Needs approval");
        taskService.save(b);

        var page = PageRequest.ofSize(10);
        assertThat(taskService.list(page, TaskFilter.empty())).hasSize(2);
        assertThat(taskService.list(page, new TaskFilter("deploy", null, null))).hasSize(2);
        assertThat(taskService.list(page, new TaskFilter("ops-1", null, null))).hasSize(1);
        assertThat(taskService.list(page, new TaskFilter("approval", null, null))).hasSize(1);
        assertThat(taskService.list(page, new TaskFilter("deploy", TaskStatus.IN_PROGRESS, null))).hasSize(1);
        assertThat(taskService.list(page, new TaskFilter(null, null, TaskPriority.LOW))).isEmpty();
    }

    @Test
    public void delete_removes_the_task() {
        var task = taskService.createTask("Temporary", null);
        taskService.delete(task);
        assertThat(taskService.list(PageRequest.ofSize(10), TaskFilter.empty())).isEmpty();
    }
}
