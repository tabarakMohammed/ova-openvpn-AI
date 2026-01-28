package iq.linux.ova;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

public class OpenVPNApp extends Application {

    private TextField configFileField;
    private TextField usernameField;
    private PasswordField passwordField;
    private CheckBox rememberCheckBox;
    private Button connectButton;
    private Button disconnectButton;
    private Label statusLabel;
    private OpenVPNController controller;
    private SettingsManager settingsManager;
    private File selectedConfigFile;

    @Override
    public void start(Stage primaryStage) {
        controller = new OpenVPNController();
        settingsManager = new SettingsManager();

        primaryStage.setTitle("OpenVPN GUI Manager");

        // Create UI Components
        VBox mainLayout = new VBox(15);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // Title
        Label titleLabel = new Label("OpenVPN Connection Manager");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Config File Section
        HBox configBox = new HBox(10);
        configBox.setAlignment(Pos.CENTER_LEFT);
        Label configLabel = new Label("Config File:");
        configLabel.setMinWidth(100);
        configFileField = new TextField();
        configFileField.setPromptText("Select .ovpn file");
        configFileField.setEditable(false);
        configFileField.setPrefWidth(300);
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> browseConfigFile(primaryStage));
        configBox.getChildren().addAll(configLabel, configFileField, browseButton);

        // Username Section
        HBox usernameBox = new HBox(10);
        usernameBox.setAlignment(Pos.CENTER_LEFT);
        Label usernameLabel = new Label("Username:");
        usernameLabel.setMinWidth(100);
        usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.setPrefWidth(300);
        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        // Password Section
        HBox passwordBox = new HBox(10);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        Label passwordLabel = new Label("Password:");
        passwordLabel.setMinWidth(100);
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setPrefWidth(300);
        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        // Remember Settings Checkbox
        HBox rememberBox = new HBox(10);
        rememberBox.setAlignment(Pos.CENTER_LEFT);
        rememberCheckBox = new CheckBox("Remember config file and username");
        rememberCheckBox.setStyle("-fx-font-size: 12px;");
        rememberBox.getChildren().add(rememberCheckBox);
        rememberBox.setPadding(new Insets(0, 0, 0, 100));

        // Buttons Section
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        connectButton = new Button("Connect");
        connectButton.setPrefWidth(120);
        connectButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        connectButton.setOnAction(e -> connect());

        disconnectButton = new Button("Disconnect");
        disconnectButton.setPrefWidth(120);
        disconnectButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        disconnectButton.setDisable(true);
        disconnectButton.setOnAction(e -> disconnect());

        Button clearButton = new Button("Clear Saved");
        clearButton.setPrefWidth(120);
        clearButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white;");
        clearButton.setOnAction(e -> clearSavedSettings());

        buttonBox.getChildren().addAll(connectButton, disconnectButton, clearButton);

        // Status Section
        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        Label statusTitleLabel = new Label("Connection Status:");
        statusTitleLabel.setStyle("-fx-font-weight: bold;");
        statusLabel = new Label("Disconnected");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        setStatusColor("DISCONNECTED");
        statusBox.getChildren().addAll(statusTitleLabel, statusLabel);

        // Add all to main layout
        mainLayout.getChildren().addAll(
                titleLabel,
                new Separator(),
                configBox,
                usernameBox,
                passwordBox,
                rememberBox,
                buttonBox,
                new Separator(),
                statusBox
        );

        // Load saved settings
        loadSavedSettings();

        Scene scene = new Scene(mainLayout, 550, 500);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            if (controller.isConnected()) {
                controller.disconnect();
            }
        });
    }

    private void loadSavedSettings() {
        String savedConfigPath = settingsManager.getConfigFilePath();
        String savedUsername = settingsManager.getUsername();

        if (savedConfigPath != null && !savedConfigPath.isEmpty()) {
            File configFile = new File(savedConfigPath);
            if (configFile.exists()) {
                selectedConfigFile = configFile;
                configFileField.setText(savedConfigPath);
                rememberCheckBox.setSelected(true);
            }
        }

        if (savedUsername != null && !savedUsername.isEmpty()) {
            usernameField.setText(savedUsername);
        }

        // Focus on password field if config and username are loaded
        if (selectedConfigFile != null && !usernameField.getText().isEmpty()) {
            passwordField.requestFocus();
        }
    }

    private void saveSettings() {
        if (rememberCheckBox.isSelected()) {
            String configPath = selectedConfigFile != null ? selectedConfigFile.getAbsolutePath() : "";
            String username = usernameField.getText().trim();
            settingsManager.saveSettings(configPath, username);
        } else {
            settingsManager.clearSettings();
        }
    }

    private void clearSavedSettings() {
        settingsManager.clearSettings();
        configFileField.clear();
        usernameField.clear();
        passwordField.clear();
        selectedConfigFile = null;
        rememberCheckBox.setSelected(false);
        showAlert("Info", "Saved settings have been cleared", Alert.AlertType.INFORMATION);
    }

    private void browseConfigFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select OpenVPN Config File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("OpenVPN Config", "*.ovpn", "*.conf")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedConfigFile = file;
            configFileField.setText(file.getAbsolutePath());
        }
    }

    private void connect() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (selectedConfigFile == null) {
            showAlert("Error", "Please select a config file", Alert.AlertType.ERROR);
            return;
        }

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter username and password", Alert.AlertType.ERROR);
            return;
        }

        // Save settings if remember is checked
        saveSettings();

        connectButton.setDisable(true);
        statusLabel.setText("Connecting...");
        setStatusColor("CONNECTING");

        new Thread(() -> {
            boolean success = controller.connect(selectedConfigFile, username, password);

            javafx.application.Platform.runLater(() -> {
                if (success) {
                    statusLabel.setText("Connected");
                    setStatusColor("CONNECTED");
                    connectButton.setDisable(true);
                    disconnectButton.setDisable(false);
                    showAlert("Success", "Connected to OpenVPN successfully!", Alert.AlertType.INFORMATION);
                } else {
                    statusLabel.setText("Connection Failed");
                    setStatusColor("FAILED");
                    connectButton.setDisable(false);
                    disconnectButton.setDisable(true);
                    showAlert("Error", "Failed to connect to OpenVPN. Check credentials and config file.", Alert.AlertType.ERROR);
                }
            });
        }).start();
    }

    private void disconnect() {
        controller.disconnect();
        statusLabel.setText("Disconnected");
        setStatusColor("DISCONNECTED");
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        passwordField.clear();
        showAlert("Info", "Disconnected from OpenVPN", Alert.AlertType.INFORMATION);
    }

    private void setStatusColor(String status) {
        switch (status) {
            case "CONNECTED":
                statusLabel.setTextFill(Color.GREEN);
                break;
            case "CONNECTING":
                statusLabel.setTextFill(Color.ORANGE);
                break;
            case "FAILED":
                statusLabel.setTextFill(Color.RED);
                break;
            case "DISCONNECTED":
                statusLabel.setTextFill(Color.GRAY);
                break;
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
      //  Application.launch(OpenVPNApp.class, args);
      launch(args);
    }
}