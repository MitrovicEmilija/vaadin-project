package com.example.examplefeature.ui;

import com.example.examplefeature.models.TaskPriority;
import com.example.examplefeature.services.TaskService;
import com.example.examplefeature.models.TaskStatus;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.notification.Notification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Browserless testovi: Vaadin UI radi u istoj JVM kao test, bez pravog browser-a. {@code navigate(...)} otvara
 * view, {@code test(component)} daje "tester" koji klikce i unosi vrednosti kao korisnik, a {@code $(Klasa.class)}
 * trazi komponente na ekranu (npr. otvoren dijalog).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class TaskListViewTest extends SpringBrowserlessTest {

    @Autowired
    TaskService taskService;

    // --- Pomocne metode -------------------------------------------------------

    private String cell(TaskListView view, int row, String columnKey) {
        var grid = test(view.taskGrid);
        return grid.getCellText(row, grid.getColumnPosition(columnKey));
    }

    private String badgeText(TaskListView view, int row, String columnKey) {
        var badge = (Badge) test(view.taskGrid).getCellComponent(row, columnKey);
        return badge.getText();
    }

    private TaskEditorDialog openedEditor() {
        return $(TaskEditorDialog.class).single();
    }

    // --- Brzi unos (originalni testovi, prilagodjeni novim kolonama) ----------

    @Test
    void empty_grid_shows_no_tasks() {
        var view = navigate(TaskListView.class);
        assertThat(test(view.taskGrid).size()).isZero();
        assertThat(view.taskGrid.getEmptyStateText()).isEqualTo("You have no tasks to complete");
    }

    @Test
    void create_task_with_empty_description_does_nothing() {
        var view = navigate(TaskListView.class);

        test(view.createBtn).click();

        assertThat(test(view.taskGrid).size()).isZero();
        assertThat(view.description.isInvalid()).isTrue();
        assertThat($(Notification.class).exists()).isFalse();
    }

    @Test
    void create_task_without_due_date() {
        var view = navigate(TaskListView.class);

        test(view.description).setValue("Buy groceries");
        test(view.createBtn).click();

        assertThat(test(view.taskGrid).size()).isEqualTo(1);
        assertThat(cell(view, 0, TaskListView.COL_DESCRIPTION)).isEqualTo("Buy groceries");
        assertThat(cell(view, 0, TaskListView.COL_DUE_DATE)).isEqualTo("Never");
        assertThat(cell(view, 0, TaskListView.COL_CREATED)).isNotEmpty();
        // Brzi unos dobija podrazumevani status i prioritet.
        assertThat(badgeText(view, 0, TaskListView.COL_STATUS)).isEqualTo("To Do");
        assertThat(badgeText(view, 0, TaskListView.COL_PRIORITY)).isEqualTo("Medium");

        assertThat(view.description.getValue()).isEmpty();
        assertThat(view.dueDate.getValue()).isNull();

        var notification = $(Notification.class).single();
        assertThat(test(notification).getText()).contains("Task added");
    }

    @Test
    void create_task_with_due_date() {
        var view = navigate(TaskListView.class);

        test(view.description).setValue("File taxes");
        test(view.dueDate).setValue(LocalDate.of(2026, 3, 15));
        test(view.createBtn).click();

        assertThat(test(view.taskGrid).size()).isEqualTo(1);
        assertThat(cell(view, 0, TaskListView.COL_DESCRIPTION)).isEqualTo("File taxes");
        assertThat(cell(view, 0, TaskListView.COL_DUE_DATE)).isNotEqualTo("Never");
    }

    @Test
    void create_multiple_tasks() {
        var view = navigate(TaskListView.class);

        test(view.description).setValue("First task");
        test(view.createBtn).click();

        test(view.description).setValue("Second task");
        test(view.dueDate).setValue(LocalDate.of(2026, 6, 1));
        test(view.createBtn).click();

        assertThat(test(view.taskGrid).size()).isEqualTo(2);
    }

    // --- Forma za izmenu (TaskEditorDialog) ----------------------------------

    @Test
    void new_task_dialog_saves_all_fields() {
        var view = navigate(TaskListView.class);

        test(view.newTaskBtn).click();
        var dialog = openedEditor();
        assertThat(dialog.getHeaderTitle()).isEqualTo("New task");

        test(dialog.jiraKey).setValue("proj-42"); // malim slovima; forma ga pretvara u velika
        test(dialog.description).setValue("Fix login bug");
        test(dialog.status).selectItem("In Progress");
        test(dialog.priority).selectItem("High");
        test(dialog.notes).setValue("Happens only on Safari");
        test(dialog.saveBtn).click();

        assertThat(dialog.isOpened()).isFalse();
        assertThat(test(view.taskGrid).size()).isEqualTo(1);
        assertThat(cell(view, 0, TaskListView.COL_JIRA_KEY)).isEqualTo("PROJ-42");
        assertThat(cell(view, 0, TaskListView.COL_DESCRIPTION)).isEqualTo("Fix login bug");
        assertThat(badgeText(view, 0, TaskListView.COL_STATUS)).isEqualTo("In Progress");
        assertThat(badgeText(view, 0, TaskListView.COL_PRIORITY)).isEqualTo("High");
        assertThat(test(view.taskGrid).getRow(0).getNotes()).isEqualTo("Happens only on Safari");
    }

    @Test
    void invalid_jira_key_blocks_save() {
        var view = navigate(TaskListView.class);

        test(view.newTaskBtn).click();
        var dialog = openedEditor();
        test(dialog.jiraKey).setValue("not a key");
        test(dialog.description).setValue("Something");

        // Greska iz @Pattern na entitetu je prikazana na polju, a Save nije aktivan.
        assertThat(dialog.jiraKey.isInvalid()).isTrue();
        assertThat(dialog.saveBtn.isEnabled()).isFalse();

        // Kad se kljuc ispravi, moze da se sacuva.
        test(dialog.jiraKey).setValue("ABC-1");
        assertThat(dialog.saveBtn.isEnabled()).isTrue();
        test(dialog.saveBtn).click();
        assertThat(test(view.taskGrid).size()).isEqualTo(1);
    }

    @Test
    void empty_description_blocks_save() {
        var view = navigate(TaskListView.class);

        test(view.newTaskBtn).click();
        var dialog = openedEditor();
        // Prazna forma nema izmena, pa Save nije aktivan.
        assertThat(dialog.saveBtn.isEnabled()).isFalse();

        test(dialog.description).setValue("Something");
        assertThat(dialog.saveBtn.isEnabled()).isTrue();

        // Kad se opis obrise, @NotBlank prijavi gresku i Save se ponovo ugasi.
        test(dialog.description).setValue("");
        assertThat(dialog.description.isInvalid()).isTrue();
        assertThat(dialog.saveBtn.isEnabled()).isFalse();
        assertThat(test(view.taskGrid).size()).isZero();
    }

    @Test
    void clicking_a_row_edits_the_task() {
        taskService.createTask("Write report", null);
        var view = navigate(TaskListView.class);

        test(view.taskGrid).clickRow(0);
        var dialog = openedEditor();
        assertThat(dialog.getHeaderTitle()).isEqualTo("Edit task");
        assertThat(dialog.description.getValue()).isEqualTo("Write report");
        // Bez izmena nema sta da se sacuva.
        assertThat(dialog.saveBtn.isEnabled()).isFalse();

        test(dialog.status).selectItem("Done");
        assertThat(dialog.saveBtn.isEnabled()).isTrue();
        test(dialog.saveBtn).click();

        assertThat(badgeText(view, 0, TaskListView.COL_STATUS)).isEqualTo("Done");
    }

    @Test
    void cancel_discards_changes() {
        taskService.createTask("Original", null);
        var view = navigate(TaskListView.class);

        test(view.taskGrid).clickRow(0);
        var dialog = openedEditor();
        test(dialog.description).setValue("Changed");
        test(dialog.cancelBtn).click();

        assertThat(cell(view, 0, TaskListView.COL_DESCRIPTION)).isEqualTo("Original");
    }

    @Test
    void delete_asks_for_confirmation() {
        taskService.createTask("Obsolete task", null);
        var view = navigate(TaskListView.class);

        test(view.taskGrid).clickRow(0);
        test(openedEditor().deleteBtn).click();

        var confirm = $(ConfirmDialog.class).single();
        test(confirm).confirm();

        assertThat(test(view.taskGrid).size()).isZero();
    }

    // --- Filteri --------------------------------------------------------------

    @Test
    void filter_by_status_and_priority() {
        var done = taskService.createTask("Done task", null);
        done.setStatus(TaskStatus.DONE);
        taskService.save(done);
        var urgent = taskService.createTask("Urgent task", null);
        urgent.setPriority(TaskPriority.CRITICAL);
        taskService.save(urgent);
        taskService.createTask("Normal task", null);

        var view = navigate(TaskListView.class);
        assertThat(test(view.taskGrid).size()).isEqualTo(3);

        test(view.statusFilter).selectItem("Done");
        assertThat(test(view.taskGrid).size()).isEqualTo(1);
        assertThat(cell(view, 0, TaskListView.COL_DESCRIPTION)).isEqualTo("Done task");

        test(view.statusFilter).clickClearButton();
        test(view.priorityFilter).selectItem("Critical");
        assertThat(test(view.taskGrid).size()).isEqualTo(1);
        assertThat(cell(view, 0, TaskListView.COL_DESCRIPTION)).isEqualTo("Urgent task");
    }

    @Test
    void search_matches_jira_key_and_description() {
        var task = taskService.createTask("Refactor payment module", null);
        task.setJiraKey("PAY-7");
        taskService.save(task);
        taskService.createTask("Update docs", null);

        var view = navigate(TaskListView.class);

        test(view.searchField).setValue("pay-7");
        assertThat(test(view.taskGrid).size()).isEqualTo(1);

        test(view.searchField).setValue("DOCS");
        assertThat(test(view.taskGrid).size()).isEqualTo(1);
        assertThat(cell(view, 0, TaskListView.COL_DESCRIPTION)).isEqualTo("Update docs");

        test(view.searchField).clear();
        assertThat(test(view.taskGrid).size()).isEqualTo(2);
    }
}
