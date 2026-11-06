package com.example.examplefeature.ui;

import com.example.examplefeature.enitities.Task;
import com.example.examplefeature.models.TaskPriority;
import com.example.examplefeature.services.TaskService;
import com.example.examplefeature.models.TaskStatus;
import com.example.examplefeature.services.WorkLogService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableRunnable;

import java.util.Locale;

class TaskEditorDialog extends Dialog {

    private final TaskService taskService;
    private final WorkLogService workLogService;
    private final Task task;
    private final SerializableRunnable onChange;

    // Imena polja su ista kao imena property-ja u Task-u (jiraKey -> getJiraKey/setJiraKey).
    // Zbog toga binder.bindInstanceFields(this) moze da ih poveze automatski, bez rucnog forField(...).bind(...).
    // Polja su package-private da bi testovi (TaskListViewTest) mogli da ih popune.
    final TextField jiraKey = new TextField("Jira key");
    final TextField description = new TextField("Description");
    final ComboBox<TaskStatus> status = new ComboBox<>("Status");
    final ComboBox<TaskPriority> priority = new ComboBox<>("Priority");
    final DatePicker dueDate = new DatePicker("Due date");
    final TextArea notes = new TextArea("Notes");

    final Button saveBtn = new Button("Save");
    final Button deleteBtn = new Button("Delete");
    final Button cancelBtn = new Button("Cancel");

    final Binder<Task> binder = new BeanValidationBinder<>(Task.class);

    final TabSheet tabs = new TabSheet();

    /**
     * @param task     task koji se menja; za nov task prosledi {@code taskService.newTask()}
     * @param onChange poziva se posle uspesnog cuvanja ili brisanja, da bi view osvezio grid
     */
    TaskEditorDialog(TaskService taskService, WorkLogService workLogService, Task task, SerializableRunnable onChange) {
        this.taskService = taskService;
        this.workLogService = workLogService;
        this.task = task;
        this.onChange = onChange;

        boolean isNew = task.getId() == null;
        setHeaderTitle(isNew ? "New task" : "Edit task");
        setWidth("min(40rem, 95vw)");

        configureFields();
        bindFields();

        add(createTabs(isNew));
        getFooter().add(createButtons(isNew));

        // readBean kopira vrednosti iz entiteta u polja. Entitet se NE menja dok ne kliknes Save,
        // pa Cancel ne mora nista da "vraca".
        binder.readBean(task);
    }

    private void configureFields() {
        jiraKey.setPlaceholder("PROJ-123");
        jiraKey.setMaxLength(Task.JIRA_KEY_MAX_LENGTH);
        jiraKey.setHelperText("Optional");
        // Validacija tek kad korisnik napusti polje (blur), a ne posle svakog slova.
        jiraKey.setValueChangeMode(ValueChangeMode.ON_CHANGE);

        description.setMaxLength(Task.DESCRIPTION_MAX_LENGTH);

        // ComboBox za enum: items su sve konstante, a ItemLabelGenerator odredjuje sta korisnik vidi.
        status.setItems(TaskStatus.values());
        status.setItemLabelGenerator(TaskStatus::getLabel);
        priority.setItems(TaskPriority.values());
        priority.setItemLabelGenerator(TaskPriority::getLabel);

        notes.setMaxLength(Task.NOTES_MAX_LENGTH);
        notes.setMinHeight("8rem");
        // Brojac karaktera ispod polja; azurira se dok kucas (EAGER salje svaku promenu serveru).
        notes.setValueChangeMode(ValueChangeMode.EAGER);
        notes.addValueChangeListener(e -> notes.setHelperText(e.getValue().length() + "/" + Task.NOTES_MAX_LENGTH));
    }

    private void bindFields() {
        // 1) Rucno povezivanje za polja kojima treba nesto posebno.
        //    TextField nikad ne vraca null, nego "", a u bazi zelimo NULL kad kljuc nije unet.
        //    withNullRepresentation("") prevodi "" <-> null, a converter pretvara "proj-1" u "PROJ-1".
        //    bind("jiraKey") po imenu property-ja (a ne Task::getJiraKey, Task::setJiraKey) zadrzava i
        //    bean-validation pravila sa entiteta (@Pattern).
        binder.forField(jiraKey)
                .withNullRepresentation("")
                .withConverter(
                        value -> value == null ? null : value.trim().toUpperCase(Locale.ROOT),
                        value -> value)
                .bind("jiraKey");

        binder.forField(notes)
                .withNullRepresentation("")
                .bind("notes");

        // 2) Sva ostala polja (description, status, priority, dueDate) povezuju se automatski po imenu.
        //    Vec rucno povezana polja se preskacu.
        binder.bindInstanceFields(this);
        // BeanValidationBinder sam dodaje zvezdicu za @NotNull (status, priority), ali ne i za @NotBlank,
        // pa je ovde dodajemo rucno. To je samo vizuelni indikator; pravu proveru i dalje radi @NotBlank.
        description.setRequiredIndicatorVisible(true);

        // Save je aktivan samo kad ima izmena i kad je forma ispravna. Lep UX, i dobro za razumevanje Binder stanja.
        // StatusChangeEvent se okida posle readBean(...) i posle svake promene u nekom polju.
        // PAZNJA: e.hasValidationErrors() gleda samo polje koje se upravo promenilo! Zato koristimo
        // binder.isValid(), koji tiho proveri SVA polja (bez crvenih poruka na netaknutim poljima).
        binder.addStatusChangeListener(e -> saveBtn.setEnabled(binder.hasChanges() && binder.isValid()));
    }

    private FormLayout createForm() {
        var form = new FormLayout(jiraKey, status, description, priority, dueDate, notes);
        // Ispod 30em jedna kolona, iznad dve.
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30em", 2));
        // Opis i beleske zauzimaju ceo red.
        form.setColspan(description, 2);
        form.setColspan(notes, 2);

        if (task.getId() != null) {
            var created = new Span("Created " + TaskFormatters.formatCreated(task, getLocale()));
            created.addClassName("task-meta");
            form.add(created, 2);
        }
        return form;
    }

    private TabSheet createTabs(boolean isNew) {
        // TabSheet = tabovi + sadrzaj. add("Naziv", komponenta) dodaje tab; sadrzaj se prikazuje kad je tab izabran.
        tabs.add("Details", createForm());

        // Nov task jos nema id u bazi, pa nema za sta da se veze unos vremena. Tab dodajemo samo za postojeci task.
        if (!isNew) {
            tabs.add("Time log", new WorkLogPanel(workLogService, task, onChange));
        }

        // Save i Delete se odnose na tab Details. Na tabu Time log ih sakrivamo da ne bi zbunjivali
        // (unos vremena se cuva odmah, klikom na "Log").
        tabs.addSelectedChangeListener(e -> {
            boolean details = tabs.getSelectedIndex() == 0;
            saveBtn.setVisible(details);
            deleteBtn.setVisible(details);
            cancelBtn.setText(details ? "Cancel" : "Close");
        });
        tabs.setWidthFull();
        return tabs;
    }

    private Button[] createButtons(boolean isNew) {
        saveBtn.addThemeVariants(ButtonVariant.PRIMARY);
        saveBtn.addClickListener(e -> save());
        // Ctrl+Enter cuva formu. addClickShortcut radi samo dok je dijalog otvoren.
        saveBtn.addClickShortcut(Key.ENTER, KeyModifier.CONTROL);

        cancelBtn.addClickListener(e -> close());

        deleteBtn.addThemeVariants(ButtonVariant.ERROR);
        deleteBtn.addClickListener(e -> confirmDelete());
        deleteBtn.setVisible(!isNew);
        // Gura Delete na levu stranu, a Cancel i Save ostaju desno.
        deleteBtn.getStyle().setMarginInlineEnd("auto");

        return new Button[] { deleteBtn, cancelBtn, saveBtn };
    }

    private void save() {
        // writeBeanIfValid: ako je forma ispravna, upisi vrednosti u entitet i vrati true.
        // Ako nije, nista ne menja, a greske su vec prikazane ispod polja.
        if (!binder.writeBeanIfValid(task)) {
            return;
        }
        taskService.save(task);
        Notification.show("Task saved", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.SUCCESS);
        onChange.run();
        close();
    }

    private void confirmDelete() {
        var confirm = new ConfirmDialog();
        confirm.setHeader("Delete task?");
        confirm.setText("\"" + task.getDescription() + "\" will be permanently deleted.");
        confirm.setCancelable(true);
        confirm.setConfirmText("Delete");
        confirm.setConfirmButtonTheme("error primary");
        confirm.addConfirmListener(e -> {
            taskService.delete(task);
            Notification.show("Task deleted", 3000, Notification.Position.BOTTOM_END);
            onChange.run();
            close();
        });
        confirm.open();
    }
}
