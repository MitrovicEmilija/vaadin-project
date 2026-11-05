package com.example.examplefeature.ui;

import com.example.base.ui.ViewTitle;
import com.example.examplefeature.enitities.Task;
import com.example.examplefeature.models.TaskFilter;
import com.example.examplefeature.models.TaskPriority;
import com.example.examplefeature.services.TaskService;
import com.example.examplefeature.models.TaskStatus;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

/**
 * Glavni ekran: lista taskova sa brzim unosom, filterima i formom za izmenu.
 * <p>
 * <b>Vaadin koncepti u ovoj klasi:</b>
 * <ul>
 * <li><b>{@code @Route}, {@code @Menu}:</b> URL ove stranice i stavka u bocnom meniju. {@code MainLayout} sam
 * pravi meni od svih {@code @Menu} view-ova.</li>
 * <li><b>Lazy {@link Grid}:</b> {@code setItems(query -> ...)} ne ucitava sve taskove odjednom. Grid trazi deo po
 * deo, dok skrolujes.</li>
 * <li><b>Sortiranje:</b> {@code setSortProperty("dueDate")} kaze grid-u koji JPA property da posalje u
 * {@code Pageable}, kad korisnik klikne na zaglavlje kolone.</li>
 * <li><b>Component kolone:</b> {@code addComponentColumn} prikazuje pravu komponentu ({@code Badge}) u celiji.</li>
 * <li><b>Part names:</b> CSS "kuke" za stilizovanje pojedinacnih celija (crveni datum kad je rok prosao).</li>
 * <li><b>Filteri:</b> {@code ValueChangeMode.LAZY} salje vrednost tek kad prestanes da kucas, pa se baza ne
 * pretrazuje posle svakog slova.</li>
 * </ul>
 */
@Route(value = "")
@PageTitle("Task List")
@Menu(order = 0, icon = "icons/clipboard-check.svg", title = "Task List")
class TaskListView extends VerticalLayout {

    // Kljucevi kolona: preko njih testovi nalaze kolonu (getColumnPosition), a mogu i da sakriju kolonu.
    static final String COL_JIRA_KEY = "jiraKey";
    static final String COL_DESCRIPTION = "description";
    static final String COL_STATUS = "status";
    static final String COL_PRIORITY = "priority";
    static final String COL_DUE_DATE = "dueDate";
    static final String COL_CREATED = "creationDate";

    private final TaskService taskService;

    /** Trenutni filter. Grid ga cita pri svakom upitu, pa je dovoljno da ga zamenis i pozoves refreshAll(). */
    private TaskFilter filter = TaskFilter.empty();

    // Brzi unos
    final TextField description;
    final DatePicker dueDate;
    final Button createBtn;
    final Button newTaskBtn;

    // Filteri
    final TextField searchField;
    final ComboBox<TaskStatus> statusFilter;
    final ComboBox<TaskPriority> priorityFilter;

    final Grid<Task> taskGrid;

    TaskListView(TaskService taskService) {
        this.taskService = taskService;

        // --- Brzi unos (kao u originalnom primeru): opis + rok + Enter ---
        description = new TextField();
        description.setPlaceholder("What do you want to do?");
        description.setAriaLabel("Task description");
        description.setMaxLength(Task.DESCRIPTION_MAX_LENGTH);
        description.setMinWidth("15em");

        dueDate = new DatePicker();
        dueDate.setPlaceholder("Due date");
        dueDate.setAriaLabel("Due date");

        createBtn = new Button("Create", event -> createTask());
        createBtn.addThemeVariants(ButtonVariant.PRIMARY);
        // Enter u polju za opis = klik na Create. Shortcut je vezan za description, pa ne smeta ostalim poljima.
        createBtn.addClickShortcut(Key.ENTER).listenOn(description);

        // Za sva polja (Jira kljuc, status, prioritet, beleske) otvara se puna forma.
        newTaskBtn = new Button("New task", VaadinIcon.PLUS.create(), e -> openEditor(taskService.newTask()));

        var toolbar = new HorizontalLayout(new ViewTitle("Task List"), description, dueDate, createBtn, newTaskBtn);
        toolbar.setFlexGrow(1, description, dueDate);
        toolbar.setWrap(true);
        toolbar.setWidthFull();
        toolbar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        // --- Filteri ---
        searchField = new TextField();
        searchField.setPlaceholder("Search key, description, notes...");
        searchField.setAriaLabel("Search");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY); // ceka ~400ms posle poslednjeg slova
        searchField.setMinWidth("15em");
        searchField.addValueChangeListener(e -> updateFilter());

        statusFilter = new ComboBox<>();
        statusFilter.setPlaceholder("All statuses");
        statusFilter.setAriaLabel("Filter by status");
        statusFilter.setItems(TaskStatus.values());
        statusFilter.setItemLabelGenerator(TaskStatus::getLabel);
        statusFilter.setClearButtonVisible(true); // klik na X vraca null, tj. "sve"
        statusFilter.addValueChangeListener(e -> updateFilter());

        priorityFilter = new ComboBox<>();
        priorityFilter.setPlaceholder("All priorities");
        priorityFilter.setAriaLabel("Filter by priority");
        priorityFilter.setItems(TaskPriority.values());
        priorityFilter.setItemLabelGenerator(TaskPriority::getLabel);
        priorityFilter.setClearButtonVisible(true);
        priorityFilter.addValueChangeListener(e -> updateFilter());

        var filterBar = new HorizontalLayout(searchField, statusFilter, priorityFilter);
        filterBar.setFlexGrow(1, searchField);
        filterBar.setWrap(true);
        filterBar.setWidthFull();

        // --- Grid ---
        taskGrid = createGrid();

        setSizeFull();
        add(toolbar, filterBar, taskGrid);
    }

    private Grid<Task> createGrid() {
        var grid = new Grid<Task>();
        grid.addClassName("task-grid");

        // Lazy data provider: Vaadin prosledjuje Query (offset, limit, sortiranje), a VaadinSpringDataHelpers
        // ga pretvara u Spring Pageable. Lambda svaki put cita trenutnu vrednost polja `filter`.
        grid.setItems(query -> taskService.list(toSpringPageRequest(query), filter).stream());

        // Fiksne sirine za uske kolone. setAutoWidth(true) racuna sirinu samo od prvih ucitanih redova, pa bi
        // se kasnije duzi tekst (npr. "In Progress") secao.
        grid.addColumn(task -> Objects.requireNonNullElse(task.getJiraKey(), ""))
                .setKey(COL_JIRA_KEY)
                .setHeader("Key")
                .setSortProperty("jiraKey")
                .setWidth("8em")
                .setFlexGrow(0);

        grid.addColumn(Task::getDescription)
                .setKey(COL_DESCRIPTION)
                .setHeader("Description")
                .setSortProperty("description")
                .setFlexGrow(1);

        // Component kolona: za svaki red se pravi nova Badge komponenta. Za par stotina redova je to u redu,
        // a za hiljade bi bio bolji LitRenderer, jer je laksi za browser.
        grid.addComponentColumn(task -> TaskFormatters.statusBadge(task.getStatus()))
                .setKey(COL_STATUS)
                .setHeader("Status")
                .setSortProperty("status")
                .setWidth("9.5em")
                .setFlexGrow(0);

        // VEZBA: sortiranje po prioritetu bi islo po TEKSTU ("CRITICAL" < "HIGH" < "LOW" < "MEDIUM"), a ne po
        // vaznosti. Zato ova kolona nije sortabilna. Probaj da to resis, npr. dodatnom int kolonom u bazi.
        grid.addComponentColumn(task -> TaskFormatters.priorityBadge(task.getPriority()))
                .setKey(COL_PRIORITY)
                .setHeader("Priority")
                .setWidth("7.5em")
                .setFlexGrow(0);

        var dueCol = grid.addColumn(task -> TaskFormatters.formatDue(task, getLocale()))
                .setKey(COL_DUE_DATE)
                .setHeader("Due Date")
                .setSortProperty("dueDate")
                .setWidth("9em")
                .setFlexGrow(0);
        // Part name: celija dobija CSS part "overdue", a styles.css je boji u crveno.
        dueCol.setPartNameGenerator(task -> task.isOverdue(LocalDate.now()) ? "overdue" : null);

        var createdCol = grid.addColumn(task -> TaskFormatters.formatCreated(task, getLocale()))
                .setKey(COL_CREATED)
                .setHeader("Created")
                .setSortProperty("creationDate")
                .setWidth("13em")
                .setFlexGrow(0);

        // Podrazumevano sortiranje: najnoviji taskovi na vrhu.
        grid.sort(List.of(new GridSortOrder<>(createdCol, SortDirection.DESCENDING)));

        // Klik na red otvara formu za izmenu.
        grid.addItemClickListener(e -> openEditor(e.getItem()));

        grid.setEmptyStateText("You have no tasks to complete");
        grid.setSizeFull();
        return grid;
    }

    private void updateFilter() {
        filter = new TaskFilter(searchField.getValue(), statusFilter.getValue(), priorityFilter.getValue());
        // refreshAll() odbacuje kesirane redove; grid ponovo poziva lambdu iz setItems sa novim filterom.
        taskGrid.getDataProvider().refreshAll();
    }

    private void openEditor(Task task) {
        new TaskEditorDialog(taskService, task, taskGrid.getDataProvider()::refreshAll).open();
    }

    private void createTask() {
        if (description.getValue().isBlank()) {
            description.setInvalid(true);
            description.setErrorMessage("Description is required");
            return;
        }
        taskService.createTask(description.getValue(), dueDate.getValue());
        taskGrid.getDataProvider().refreshAll();
        description.clear();
        description.setInvalid(false);
        dueDate.clear();
        Notification.show("Task added", 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.SUCCESS);
    }
}
