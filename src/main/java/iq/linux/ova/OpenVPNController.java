package iq.linux.ova;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


public class OpenVPNController {

    private Process vpnProcess;
    private boolean connected = false;
    private Path authFile;

    public OpenVPNController() {
        this.connected = false;
    }

    public boolean connect(File configFile, String username, String password) {
        try {
            // Create temporary auth file
            authFile = createAuthFile(username, password);

            // Build OpenVPN command with full paths
            // Option 1: With sudo (if configured in sudoers)
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "/usr/bin/sudo",
                    "/usr/sbin/openvpn",
                    "--config", configFile.getAbsolutePath(),
                    "--auth-user-pass", authFile.toString(),
                    "--auth-nocache"
            );

            /* Option 2: Without sudo (if you've set capabilities with: sudo setcap cap_net_admin+eip /usr/sbin/openvpn)
            ProcessBuilder processBuilder = new ProcessBuilder(
                "/usr/sbin/openvpn",
                "--config", configFile.getAbsolutePath(),
                "--auth-user-pass", authFile.toString(),
                "--auth-nocache"
            );
            */

            // Set environment
            processBuilder.environment().put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");

            // Redirect error stream
            processBuilder.redirectErrorStream(true);

            // Start process
            vpnProcess = processBuilder.start();

            // Monitor connection status
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(vpnProcess.getInputStream())
            );

            String line;
            boolean initComplete = false;
            int timeout = 30; // 30 seconds timeout
            long startTime = System.currentTimeMillis();

            while ((line = reader.readLine()) != null) {
                System.out.println("OpenVPN: " + line);

                // Check for successful initialization
                if (line.contains("Initialization Sequence Completed") ||
                        line.contains("AUTH_SUCCESSFUL")) {
                    initComplete = true;
                    connected = true;
                    break;
                }

                // Check for authentication failure
                if (line.contains("AUTH_FAILED") ||
                        line.contains("auth-failure")) {
                    System.err.println("Authentication failed");
                    disconnect();
                    return false;
                }

                // Timeout check
                if ((System.currentTimeMillis() - startTime) / 1000 > timeout) {
                    System.err.println("Connection timeout");
                    disconnect();
                    return false;
                }
            }

            if (initComplete) {
                // Start monitoring thread
                startMonitoringThread(reader);
                return true;
            } else {
                disconnect();
                return false;
            }

        } catch (IOException e) {
            System.err.println("Error connecting to OpenVPN: " + e.getMessage());
            e.printStackTrace();
            disconnect();
            return false;
        }
    }

    private Path createAuthFile(String username, String password) throws IOException {
        Path tempFile = Files.createTempFile("openvpn_auth_", ".txt");

        // Write credentials to file
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            writer.write(username);
            writer.newLine();
            writer.write(password);
        }

        // Set file permissions to be readable only by owner
        File file = tempFile.toFile();
        file.setReadable(false, false);
        file.setReadable(true, true);
        file.setWritable(false, false);
        file.setWritable(true, true);

        // Delete on exit for security
        file.deleteOnExit();

        return tempFile;
    }

    private void startMonitoringThread(BufferedReader reader) {
        Thread monitorThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("OpenVPN: " + line);

                    // Check for disconnection
                    if (line.contains("SIGTERM") ||
                            line.contains("process exiting") ||
                            line.contains("Exiting")) {
                        connected = false;
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error monitoring connection: " + e.getMessage());
                connected = false;
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void disconnect() {
        if (vpnProcess != null && vpnProcess.isAlive()) {
            vpnProcess.destroy();
            try {
                vpnProcess.waitFor();
            } catch (InterruptedException e) {
                vpnProcess.destroyForcibly();
            }
        }

        // Clean up auth file
        if (authFile != null) {
            try {
                Files.deleteIfExists(authFile);
            } catch (IOException e) {
                System.err.println("Error deleting auth file: " + e.getMessage());
            }
        }

        connected = false;
        vpnProcess = null;
    }

    public boolean isConnected() {
        return connected && vpnProcess != null && vpnProcess.isAlive();
    }
}