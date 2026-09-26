package pe.edu.cacaoselva.desktop.view;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

public final class LoginWindow extends Dialog<LoginWindow.Credentials> {
    public LoginWindow(Window owner) {
        setTitle("Iniciar sesión - CacaoSelva");
        setHeaderText("Ingresa con una cuenta autorizada");
        var login = new TextField("admin");
        var password = new PasswordField();
        login.setId("login-usuario"); password.setId("login-contrasena");
        var form = new GridPane();
        form.setHgap(10); form.setVgap(10); form.setPadding(new Insets(16));
        form.addRow(0, new Label("Usuario"), login);
        form.addRow(1, new Label("Contraseña"), password);
        getDialogPane().setContent(form);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResultConverter(button -> button == ButtonType.OK
                ? new Credentials(login.getText().strip(), password.getText()) : null);
    }

    public record Credentials(String usuario, String contrasena) { }
}
