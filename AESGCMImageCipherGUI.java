import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESGCMImageCipherGUI extends JFrame {

    private JTextField fileField;
    private JPasswordField passField;
    private JTextArea outputArea;

    public AESGCMImageCipherGUI() {

        // Modern Look
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setTitle("AES-GCM Image Encrypt / Decrypt Tool");
        setSize(600, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main Panel
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel title = new JLabel("🔐 AES-GCM Image Encryption Tool", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);

        // Center Form
        JPanel formPanel = new JPanel(new GridLayout(3, 1, 10, 10));

        // File Row
        JPanel fileRow = new JPanel(new BorderLayout(10, 10));
        fileField = new JTextField();
        JButton browseBtn = new JButton("Browse");
        fileRow.add(new JLabel("Select File:"), BorderLayout.WEST);
        fileRow.add(fileField, BorderLayout.CENTER);
        fileRow.add(browseBtn, BorderLayout.EAST);

        // Password Row
        JPanel passRow = new JPanel(new BorderLayout(10, 10));
        passField = new JPasswordField();
        passRow.add(new JLabel("Password:"), BorderLayout.WEST);
        passRow.add(passField, BorderLayout.CENTER);

        // Button Row
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        JButton encryptBtn = new JButton("Encrypt (AES-GCM)");
        JButton decryptBtn = new JButton("Decrypt (AES-GCM)");
        buttonRow.add(encryptBtn);
        buttonRow.add(decryptBtn);

        formPanel.add(fileRow);
        formPanel.add(passRow);
        formPanel.add(buttonRow);

        panel.add(formPanel, BorderLayout.CENTER);

        // Output Area
        outputArea = new JTextArea(6, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        outputArea.setBorder(BorderFactory.createTitledBorder("Status"));

        panel.add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        add(panel);

        // Browse Action
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                fileField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        // Encrypt Action
        encryptBtn.addActionListener(e -> encryptFile());

        // Decrypt Action
        decryptBtn.addActionListener(e -> decryptFile());

        setVisible(true);
    }

    // ================= AES KEY FROM PASSWORD =================
    private SecretKey getAESKey(String password) throws Exception {

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(password.getBytes("UTF-8"));

        key = Arrays.copyOf(key, 16); // AES-128 Key
        return new SecretKeySpec(key, "AES");
    }

    // ================= ENCRYPT (AES-GCM) =================
    private void encryptFile() {

        try {
            String filePath = fileField.getText();
            String password = new String(passField.getPassword());

            if (filePath.isEmpty() || password.isEmpty()) {
                outputArea.setText("⚠ Please select file and enter password.");
                return;
            }

            String outputFile = filePath + ".gcm";

            SecretKey key = getAESKey(password);

            // Generate Random IV (12 bytes recommended)
            byte[] iv = new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            FileInputStream fis = new FileInputStream(filePath);
            FileOutputStream fos = new FileOutputStream(outputFile);

            // Write IV first (needed for decryption)
            fos.write(iv);

            CipherOutputStream cos = new CipherOutputStream(fos, cipher);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }

            fis.close();
            cos.close();

            outputArea.setText("✅ Encryption Successful!\nEncrypted File:\n" + outputFile);

        } catch (Exception ex) {
            outputArea.setText("❌ Encryption Error:\n" + ex.getMessage());
        }
    }

    // ================= DECRYPT (AES-GCM) =================
    private void decryptFile() {

        try {
            String filePath = fileField.getText();
            String password = new String(passField.getPassword());

            if (filePath.isEmpty() || password.isEmpty()) {
                outputArea.setText("⚠ Please select file and enter password.");
                return;
            }

            if (!filePath.endsWith(".gcm")) {
                outputArea.setText("⚠ Please select a valid .gcm encrypted file.");
                return;
            }

            String outputFile = filePath.replace(".gcm", "_restored.jpg");

            SecretKey key = getAESKey(password);

            FileInputStream fis = new FileInputStream(filePath);

            // Read IV (first 12 bytes)
            byte[] iv = new byte[12];
            fis.read(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            CipherInputStream cis = new CipherInputStream(fis, cipher);
            FileOutputStream fos = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            cis.close();
            fos.close();

            outputArea.setText("✅ Decryption Successful!\nRestored File:\n" + outputFile);

        } catch (Exception ex) {
            outputArea.setText("❌ Decryption Failed!\nWrong Password or File Modified!");
        }
    }

    // ================= MAIN =================
    public static void main(String[] args) {
        new AESGCMImageCipherGUI();
    }
}
