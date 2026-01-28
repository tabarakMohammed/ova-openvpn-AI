package iq.linux.ova;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class SettingsManager {

    private static final String SETTINGS_DIR = System.getProperty("user.home") + "/.config/openvpn-gui";
    private static final String SETTINGS_FILE = SETTINGS_DIR + "/settings.properties";

    private Properties properties;

    public SettingsManager() {
        properties = new Properties();
        ensureSettingsDirectoryExists();
        loadSettings();
    }

    private void ensureSettingsDirectoryExists() {
        try {
            Path settingsPath = Paths.get(SETTINGS_DIR);
            if (!Files.exists(settingsPath)) {
                Files.createDirectories(settingsPath);
            }
        } catch (IOException e) {
            System.err.println("Error creating settings directory: " + e.getMessage());
        }
    }

    private void loadSettings() {
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading settings: " + e.getMessage());
            }
        }
    }

    public void saveSettings(String configFilePath, String username) {
        properties.setProperty("config_file_path", configFilePath);
        properties.setProperty("username", username);

        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(fos, "OpenVPN GUI Settings");

            // Set file permissions to be readable/writable only by owner
            File settingsFile = new File(SETTINGS_FILE);
            settingsFile.setReadable(false, false);
            settingsFile.setReadable(true, true);
            settingsFile.setWritable(false, false);
            settingsFile.setWritable(true, true);

            System.out.println("Settings saved successfully");
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    public String getConfigFilePath() {
        return properties.getProperty("config_file_path", "");
    }

    public String getUsername() {
        return properties.getProperty("username", "");
    }

    public void clearSettings() {
        properties.clear();
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            settingsFile.delete();
        }
        System.out.println("Settings cleared");
    }
}