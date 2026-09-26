package pe.edu.cacaoselva.desktop.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pe.edu.cacaoselva.application.dto.FiltroLotes;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.application.dto.PaginaLotes;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;
import pe.edu.cacaoselva.domain.model.Socio;

public final class LotesWindow extends BorderPane implements LotesView {
    private static final int PAGE_SIZE = 20;
    private final Button consultar = new Button("Consultar");
    private final Button guardar = new Button("Guardar");
    private final Button eliminar = new Button("Eliminar");
    private final Button nuevo = new Button("Nuevo lote");
    private final Button anterior = new Button("Anterior");
    private final Button siguiente = new Button("Siguiente");
    private final TableView<Lote> tabla = new TableView<>();
    private final Label estado = new Label("Estado: Listo");
    private final Label ultimaConsulta = new Label("Sin consultas realizadas");
    private final Label visibles = new Label("0 lotes visibles");
    private final Label pagina = new Label("Página 0 de 0");
    private final Label saludApi = new Label("API: sin comprobar");
    private final TextField filtroSocio = new TextField();
    private final ComboBox<String> filtroEstado = new ComboBox<>();
    private final ComboBox<Socio> socio = new ComboBox<>();
    private final ObservableList<Socio> socios = FXCollections.observableArrayList();
    private final TextField peso = new TextField();
    private final ComboBox<EstadoLote> estadoLote = new ComboBox<>();
    private final Label tituloFormulario = new Label("Nuevo lote");
    private final BooleanProperty ocupado = new SimpleBooleanProperty();
    private final ObservableList<Lote> registros = FXCollections.observableArrayList();
    private Integer selectedId;
    private Integer selectedVersion;
    private int currentPage;
    private int totalPages;
    private Consumer<FiltroLotes> buscarAction;

    public LotesWindow() {
        getStyleClass().add("lotes-window");
        setPadding(new Insets(24));
        configurarIdentificadores();
        configurarTabla();
        setTop(crearCabecera());
        setCenter(tabla);
        setRight(crearFormulario());
        VBox footer = new VBox(5, estado, ultimaConsulta, visibles, saludApi);
        footer.setPadding(new Insets(14, 0, 0, 0));
        setBottom(footer);
        consultar.disableProperty().bind(ocupado);
        guardar.disableProperty().bind(ocupado);
        nuevo.disableProperty().bind(ocupado);
        anterior.disableProperty().bind(ocupado.or(paginaAnteriorInvalida()));
        siguiente.disableProperty().bind(ocupado.or(paginaSiguienteInvalida()));
        eliminar.disableProperty().bind(ocupado.or(tabla.getSelectionModel().selectedItemProperty().isNull()));
        tabla.getSelectionModel().selectedItemProperty().addListener((ignored, before, lote) -> cargarFormulario(lote));
    }

    private javafx.beans.binding.BooleanBinding paginaAnteriorInvalida() {
        return javafx.beans.binding.Bindings.createBooleanBinding(() -> currentPage <= 0);
    }

    private javafx.beans.binding.BooleanBinding paginaSiguienteInvalida() {
        return javafx.beans.binding.Bindings.createBooleanBinding(() -> totalPages == 0 || currentPage + 1 >= totalPages,
                pagina.textProperty());
    }

    private void configurarIdentificadores() {
        consultar.setId("consultar"); guardar.setId("guardar"); eliminar.setId("eliminar"); nuevo.setId("nuevo");
        anterior.setId("anterior"); siguiente.setId("siguiente"); socio.setId("socio"); peso.setId("peso");
        estadoLote.setId("estado-lote"); filtroSocio.setId("filtro-socio"); filtroEstado.setId("filtro-estado");
        estado.setId("estado"); tabla.setId("lotes");
    }

    private VBox crearCabecera() {
        Label title = new Label("CacaoSelva");
        title.getStyleClass().add("title");
        Label description = new Label("Registro y seguimiento de lotes de cacao");
        description.getStyleClass().add("description");
        filtroSocio.setPromptText("Buscar socio por nombre o ID");
        filtroSocio.setPrefWidth(250);
        filtroEstado.getItems().setAll("TODOS", "PENDIENTE", "LIQUIDADO");
        filtroEstado.setValue("TODOS");
        filtroSocio.textProperty().addListener((o, a, b) -> solicitarFiltro());
        filtroEstado.valueProperty().addListener((o, a, b) -> solicitarFiltro());
        anterior.setOnAction(event -> solicitarPagina(currentPage - 1));
        siguiente.setOnAction(event -> solicitarPagina(currentPage + 1));
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(24, 24);
        progress.visibleProperty().bind(ocupado); progress.managedProperty().bind(ocupado);
        HBox actions = new HBox(10, consultar, filtroSocio, filtroEstado, progress);
        HBox pages = new HBox(8, anterior, pagina, siguiente);
        VBox header = new VBox(10, title, description, actions, pages);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private VBox crearFormulario() {
        tituloFormulario.getStyleClass().add("form-title");
        socio.setPromptText("Selecciona un socio");
        socio.setItems(socios);
        socio.setMaxWidth(Double.MAX_VALUE);
        peso.setPromptText("Ejemplo: 120.500");
        estadoLote.getItems().setAll(EstadoLote.values());
        estadoLote.setValue(EstadoLote.PENDIENTE);
        estadoLote.setMaxWidth(Double.MAX_VALUE);
        nuevo.setOnAction(event -> {
            tabla.getSelectionModel().clearSelection(); cargarFormulario(null); socio.requestFocus();
        });
        VBox fields = new VBox(8, new Label("Socio"), socio, new Label("Peso (kg)"), peso,
                new Label("Estado"), estadoLote);
        fields.disableProperty().bind(ocupado);
        Label help = new Label("Selecciona una fila para editar.\nUsa Nuevo lote para registrar otra entrega.");
        help.setWrapText(true); help.getStyleClass().add("description");
        VBox form = new VBox(14, tituloFormulario, fields, new HBox(8, guardar, nuevo), eliminar, help);
        form.setPadding(new Insets(0, 0, 0, 24)); form.setPrefWidth(300);
        return form;
    }

    private void configurarTabla() {
        TableColumn<Lote, Integer> id = new TableColumn<>("ID");
        id.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        TableColumn<Lote, String> socioCol = new TableColumn<>("Socio");
        socioCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nombreSocio(cell.getValue().socioId())));
        TableColumn<Lote, BigDecimal> pesoCol = new TableColumn<>("Peso (kg)");
        pesoCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().pesoKg()));
        TableColumn<Lote, String> estadoCol = new TableColumn<>("Estado");
        estadoCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().estado().name()));
        tabla.getColumns().setAll(id, socioCol, pesoCol, estadoCol);
        tabla.setItems(registros);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Presiona Consultar para obtener los lotes."));
    }

    private String nombreSocio(Integer id) {
        return socios.stream().filter(item -> item.id().equals(id)).map(Socio::nombre).findFirst().orElse("Socio " + id);
    }

    private void solicitarFiltro() {
        currentPage = 0;
        solicitarPagina(0);
    }

    private void solicitarPagina(int page) {
        if (page < 0 || buscarAction == null) return;
        currentPage = page;
        String value = filtroSocio.getText() == null ? "" : filtroSocio.getText().strip();
        Integer socioId = value.matches("\\d+") ? Integer.valueOf(value) : null;
        EstadoLote state = filtroEstado.getValue() == null || "TODOS".equals(filtroEstado.getValue())
                ? null : EstadoLote.valueOf(filtroEstado.getValue());
        buscarAction.accept(new FiltroLotes(state, socioId, socioId == null ? value : null, page, PAGE_SIZE));
    }

    public void setOnBuscar(Consumer<FiltroLotes> action) { buscarAction = action; }
    public void setOnConsultar(Runnable action) { consultar.setOnAction(event -> action.run()); }

    public void setOnGuardar(BiConsumer<Integer, GuardarLoteCommand> action) {
        guardar.setOnAction(event -> {
            try {
                Socio selected = socio.getValue();
                if (selected == null) { estado.setText("Selecciona un socio de la lista."); return; }
                BigDecimal pesoKg = new BigDecimal(peso.getText().strip().replace(',', '.'));
                action.accept(selectedId, new GuardarLoteCommand(selected.id(), pesoKg, estadoLote.getValue(), selectedVersion));
            } catch (NumberFormatException error) {
                estado.setText("Introduce un peso numérico válido, por ejemplo 120.500.");
            }
        });
    }

    public void setOnEliminar(Consumer<Integer> action) {
        eliminar.setOnAction(event -> {
            Integer id = selectedId;
            if (id == null) return;
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Eliminar el lote #" + id + " de " + (socio.getValue() == null ? "" : socio.getValue().nombre()) + "?",
                    ButtonType.CANCEL, ButtonType.OK);
            confirmation.initOwner(getScene().getWindow()); confirmation.setTitle("Eliminar lote");
            confirmation.setHeaderText("Esta acción elimina el registro de la base de datos.");
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) action.accept(id);
        });
    }

    @Override public void mostrarConsultando() { mostrarOperacion("Consultando..."); }

    @Override public void mostrarOperacion(String message) {
        ocupado.set(true); registros.clear(); tabla.setPlaceholder(new Label(message));
        estado.setText(message); ultimaConsulta.setText("Esperando datos actuales...");
    }

    @Override public void mostrarPaginaLotes(PaginaLotes page) {
        currentPage = page.page(); totalPages = page.totalPages();
        pagina.setText("Página " + (totalPages == 0 ? 0 : currentPage + 1) + " de " + totalPages);
        registros.setAll(page.content().stream().map(item -> item.toDomain()).toList());
        visibles.setText(page.totalElements() + " lotes en total");
        completarConsulta(page.content().size() + " lotes recibidos.");
    }

    @Override public void mostrarLotes(List<Lote> lotes) {
        registros.setAll(lotes); visibles.setText(lotes.size() + " lotes");
        totalPages = lotes.isEmpty() ? 0 : 1; currentPage = 0; pagina.setText("Página " + totalPages + " de " + totalPages);
        completarConsulta(lotes.size() + " lotes recibidos.");
    }

    private void completarConsulta(String message) {
        cargarFormulario(null); tabla.setPlaceholder(new Label("No hay lotes para estos filtros."));
        ocupado.set(false); estado.setText(message);
        ultimaConsulta.setText("Actualizado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }

    @Override public void mostrarSocios(List<Socio> items) { socios.setAll(items); tabla.refresh(); }
    @Override public void mostrarSaludApi(boolean disponible) { saludApi.setText(disponible ? "API: disponible" : "API: sin conexión"); }
    @Override public void mostrarError() { mostrarError("No se pudo conectar con la API."); }

    @Override public void mostrarError(String message) {
        registros.clear(); totalPages = 0; pagina.setText("Página 0 de 0"); visibles.setText("0 lotes en total");
        tabla.setPlaceholder(new Label("Sin datos actuales. Vuelve a consultar.")); ocupado.set(false);
        estado.setText(message); ultimaConsulta.setText("Sin datos actualizados");
    }

    private void cargarFormulario(Lote lote) {
        selectedId = lote == null ? null : lote.id();
        selectedVersion = lote == null ? null : lote.version();
        tituloFormulario.setText(lote == null ? "Nuevo lote" : "Editar lote #" + lote.id());
        socio.setValue(lote == null ? null : socios.stream().filter(item -> item.id().equals(lote.socioId())).findFirst().orElse(null));
        peso.setText(lote == null ? "" : lote.pesoKg().toPlainString());
        estadoLote.setValue(lote == null ? EstadoLote.PENDIENTE : lote.estado());
    }
}
