package com.example.examplefeature.ui;

import com.example.examplefeature.enitities.Task;
import com.example.examplefeature.enitities.Worklog;
import com.example.examplefeature.services.WorkLogService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializableRunnable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class WorkLogPanel extends VerticalLayout {

    private final WorkLogService workLogService;
    private final Task task;
    private final SerializableRunnable onChange;

    final DatePicker workDate = new DatePicker("Work Date");
    final IntegerField minutes = new IntegerField("Minutes");
    final TextField comment = new TextField("Comment");
    final Button logWorkButton = new Button("Log Work");

    final Grid<Worklog> grid = new Grid<>();
    private Grid.Column<Worklog> durationCol;

    // Poseban Binder samo za unos vremena, odvojen od Binder<Task> u dijalogu.
    // Ovde NE koristimo bindInstanceFields, nego rucno forField(...) za svako polje.
    private final Binder<Worklog> binder = new Binder<>();

    /**
     * @param onChange poziva se posle unosa ili brisanja, da glavni grid osvezi kolonu "Logged"
     */
    WorkLogPanel(WorkLogService workLogService, Task task, SerializableRunnable onChange) {
        this.workLogService = workLogService;
        this.task = task;
        this.onChange = onChange;

        setPadding(false);

        configureFields();
        bindFields();
        configureGrid();

        var entryRow = new HorizontalLayout(workDate, minutes, comment, logWorkButton);
        entryRow.setWidthFull();
        entryRow.setFlexGrow(1, comment);
        entryRow.setWrap(true);

        entryRow.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        add(entryRow, grid);
        refresh();
    }

    private void configureFields() {
        workDate.setValue(LocalDate.now());
        workDate.setMax(LocalDate.now());
        workDate.setWidth("10em");

        minutes.setMin(1);
        minutes.setMax(24 * 60);
        minutes.setWidth("8em");

        comment.setPlaceholder("What did you do?");

        logWorkButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        logWorkButton.addClickListener(e -> logWork());
        logWorkButton.addClickShortcut(Key.ENTER).listenOn(minutes, comment);
    }

    private void bindFields() {
        binder.forField(workDate)
                .asRequired("Pick a date")
                .withValidator(d -> !d.isAfter(LocalDate.now()), "Date cannot be in the future")
                .bind(Worklog::getWorkDate, Worklog::setWorkDate);

        binder.forField(minutes)
                .asRequired("Enter minutes")
                .withValidator(m -> m >= 1 && m <= 24 * 60, "Minutes must be between 1 and 1440")
                .bind(Worklog::getMinutes, Worklog::setMinutes);

        binder.forField(comment)
                .withNullRepresentation("")
                .bind(Worklog::getComment, Worklog::setComment);
    }

    private void configureGrid() {
        var dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(getLocale());

        grid.addColumn(w -> dateFormatter.format(w.getWorkDate()))
                .setHeader("Date")
                .setFooter("Total")
                .setWidth("9em")
                .setFlexGrow(0);

        durationCol = grid.addColumn(w -> formatMinutes(w.getMinutes()))
                .setHeader("Time")
                .setWidth("7em")
                .setFlexGrow(0);

        grid.addColumn(w -> w.getComment() == null ? "" : w.getComment())
                .setHeader("Comment")
                .setFlexGrow(1);

        grid.addComponentColumn(w -> {
            var delete = new Button(VaadinIcon.TRASH.create(), e -> deleteEntry(w));
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            delete.setAriaLabel("Delete work log entry");
            return delete;
        })
                .setWidth("4em")
                .setFlexGrow(0);

        grid.setEmptyStateText("No work log entries yet");
        grid.setHeight("15em");
    }

    private void logWork() {
        var entry = workLogService.newWorkLog();
        if (!binder.writeBeanIfValid(entry)) {
            return;
        }
        workLogService.logWork(task, entry.getWorkDate(), entry.getMinutes(), entry.getComment());
        minutes.clear();
        comment.clear();

        minutes.setInvalid(false);
        minutes.focus();

        Notification.show("Logged", 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        refresh();
        onChange.run();
    }

    private void deleteEntry(Worklog entry) {
        workLogService.deleteWorkLog(entry);
        refresh();
        onChange.run();
    }

    private void refresh() {
        grid.setItems(workLogService.listForTask(task));
        durationCol.setFooter(formatMinutes(workLogService.getTotalLoggedMinutes(task)));
    }

    static String formatMinutes(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        if (h == 0) {
            return m + "min";
        }
        return m == 0 ? h + "h" : h + "h " + m + "min";
    }
}
