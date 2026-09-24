package pe.edu.cacaoselva.desktop.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

public final class LotesWindow extends BorderPane implements LotesView {
    private final Button consultar = new Button("Consultar");
    private final Button guardar = new Button("Guardar");
    private final Button eliminar = new Button("Eliminar");
    private final Button nuevo = new Button("Nuevo lote");
    private final TableView<Lote> tabla = new TableView<>();
    private final Label estado = new Label("Estado: Listo");
    private final Label ultimaConsulta = new Label("Sin consultas realizadas");
    private final Label visibles = new Label("0 lotes visibles");
    private final TextField filtroSocio = new TextField();
    private final ComboBox<String> filtroEstado = new ComboBox<>();
    private final TextField socio = new TextField();
    private final TextField peso = new TextField();
    private final ComboBox<EstadoLote> estadoLote = new ComboBox<>();
    private final Label tituloFormulario = new Label("Nuevo lote");
    private final BooleanProperty ocupado = new SimpleBooleanProperty();
    private final ObservableList<Lote> registros = FXCollections.observableArrayList();
    private final FilteredList<Lote> filtrados = new FilteredList<>(registros);
    private Integer selectedId;

    public LotesWindow() {
        getStyleClass().add("lotes-window");
        setPadding(new Insets(24));
        configurarIdentificadores();
        configurarTabla();
        setTop(crearCabecera());
        setCenter(tabla);
        setRight(crearFormulario());
        VBox footer = new VBox(5, estado, ultimaConsulta, visibles);
        footer.setPadding(new Insets(14, 0, 0, 0));
        setBottom(footer);
        consultar.disableProperty().bind(ocupado);
        guardar.disableProperty().bind(ocupado);
        nuevo.disableProperty().bind(ocupado);
        eliminar.disableProperty().bind(ocupado.or(tabla.getSelectionModel().selectedItemProperty().isNull()));
        tabla.getSelectionModel().selectedItemProperty().addListener((ignored, before, lote) -> cargarFormulario(lote));
    }

    private void configurarIdentificadores() {
        consultar.setId("consultar");
        guardar.setId("guardar");
        eliminar.setId("eliminar");
        nuevo.setId("nuevo");
        socio.setId("socio");
        peso.setId("peso");
        estadoLote.setId("estado-lote");
        filtroSocio.setId("filtro-socio");
        filtroEstado.setId("filtro-estado");
        estado.setId("estado");
        tabla.setId("lotes");
    }

    private VBox crearCabecera() {
        Label title = new Label("CacaoSelva");
        title.getStyleClass().add("title");
        Label description = new Label("Registro y seguimiento de lotes de cacao");
        description.getStyleClass().add("description");
        filtroSocio.setPromptText("Filtrar por socio o ID");
        filtroSocio.setPrefWidth(250);
        filtroEstado.getItems().setAll("TODOS", "PENDIENTE", "LIQUIDADO");
        filtroEstado.setValue("TODOS");
        filtroSocio.textProperty().addListener((o, a, b) -> filtrar());
        filtroEstado.valueProperty().addListener((o, a, b) -> filtrar());
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(24, 24);
        progress.visibleProperty().bind(ocupado);
        progress.managedProperty().bind(ocupado);
        HBox actions = new HBox(10, consultar, filtroSocio, filtroEstado, progress);
        VBox header = new VBox(10, title, description, actions);
        header.setPadding(new Insets(0, 0, 18, 0));
        return header;
    }

    private VBox crearFormulario() {
        tituloFormulario.getStyleClass().add("form-title");
        socio.setPromptText("Nombre del socio");
        peso.setPromptText("Ejemplo: 120.500");
        estadoLote.getItems().setAll(EstadoLote.values());
        estadoLote.setValue(EstadoLote.PENDIENTE);
        estadoLote.setMaxWidth(Double.MAX_VALUE);
        nuevo.setOnAction(event -> {
            tabla.getSelectionModel().clearSelection();
            cargarFormulario(null);
            socio.requestFocus();
        });
        VBox fields = new VBox(8, new Label("Socio"), socio, new Label("Peso (kg)"), peso,
                new Label("Estado"), estadoLote);
        fields.disableProperty().bind(ocupado);
        Label help = new Label("Selecciona una fila para editar.\nUsa Nuevo lote para registrar otra entrega.");
        help.setWrapText(true);
        help.getStyleClass().add("description");
        VBox form = new VBox(14, tituloFormulario, fields, new HBox(8, guardar, nuevo), eliminar, help);
        form.setPadding(new Insets(0, 0, 0, 24));
        form.setPrefWidth(300);
        return form;
    }

    private void configurarTabla() {
        TableColumn<Lote, Integer> id = new TableColumn<>("ID");
        id.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        TableColumn<Lote, String> socioCol = new TableColumn<>("Socio");
        socioCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().socio()));
        TableColumn<Lote, BigDecimal> pesoCol = new TableColumn<>("Peso (kg)");
        pesoCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().pesoKg()));
        TableColumn<Lote, String> estadoCol = new TableColumn<>("Estado");
        estadoCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().estado().name()));
        tabla.getColumns().add(id);
        tabla.getColumns().add(socioCol);
        tabla.getColumns().add(pesoCol);
        tabla.getColumns().add(estadoCol);
        SortedList<Lote> sorted = new SortedList<>(filtrados);
        sorted.comparatorProperty().bind(tabla.comparatorProperty());
        tabla.setItems(sorted);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Presiona Consultar para obtener los lotes."));
    }

    private void filtrar() {
        String text = filtroSocio.getText().strip().toLowerCase(Locale.ROOT);
        String state = filtroEstado.getValue();
        filtrados.setPredicate(lote -> (lote.socio().toLowerCase(Locale.ROOT).contains(text)
                || lote.id().toString().contains(text)) && ("TODOS".equals(state) || lote.estado().name().equals(state)));
        visibles.setText(filtrados.size() + " de " + registros.size() + " lotes visibles");
    }

    private void cargarFormulario(Lote lote) {
        selectedId = lote == null ? null : lote.id();
        tituloFormulario.setText(lote == null ? "Nuevo lote" : "Editar lote #" + lote.id());
        socio.setText(lote == null ? "" : lote.socio());
        peso.setText(lote == null ? "" : lote.pesoKg().toPlainString());
        estadoLote.setValue(lote == null ? EstadoLote.PENDIENTE : lote.estado());
    }

    public void setOnConsultar(Runnable action) { consultar.setOnAction(event -> action.run()); }

    public void setOnGuardar(BiConsumer<Integer, GuardarLoteCommand> action) {
        guardar.setOnAction(event -> {
            try {
                BigDecimal pesoKg = new BigDecimal(peso.getText().strip().replace(',', '.'));
                action.accept(selectedId, new GuardarLoteCommand(socio.getText(), pesoKg, estadoLote.getValue()));
            } catch (NumberFormatException error) {
                estado.setText("Introduce un peso numérico válido, por ejemplo 120.500.");
            }
        });
    }

    public void setOnEliminar(Consumer<Integer> action) {
        eliminar.setOnAction(event -> {
            Integer id = selectedId;
            if (id == null) { return; }
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                    "¿Eliminar el lote #" + id + " de " + socio.getText() + "?", ButtonType.CANCEL, ButtonType.OK);
            confirmation.initOwner(getScene().getWindow());
            confirmation.setTitle("Eliminar lote");
            confirmation.setHeaderText("Esta acción elimina el registro de la base de datos.");
            if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                action.accept(id);
            }
        });
    }

    @Override
    public void mostrarConsultando() { mostrarOperacion("Consultando..."); }

    @Override
    public void mostrarOperacion(String message) {
        ocupado.set(true);
        registros.clear();
        filtrar();
        tabla.setPlaceholder(new Label(message));
        estado.setText(message);
        ultimaConsulta.setText("Esperando datos actuales...");
    }

    @Override
    public void mostrarLotes(List<Lote> lotes) {
        registros.setAll(lotes);
        filtrar();
        cargarFormulario(null);
        tabla.setPlaceholder(new Label("No hay lotes que coincidan con los filtros."));
        ocupado.set(false);
        estado.setText(lotes.size() + " lotes recibidos.");
        ultimaConsulta.setText("Actualizado: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }

    @Override
    public void mostrarError() { mostrarError("No se pudo conectar con la API."); }

    @Override
    public void mostrarError(String message) {
        registros.clear();
        filtrar();
        tabla.setPlaceholder(new Label("Sin datos actuales. Vuelve a consultar."));
        ocupado.set(false);
        estado.setText(message);
        ultimaConsulta.setText("Sin datos actualizados");
    }
}
