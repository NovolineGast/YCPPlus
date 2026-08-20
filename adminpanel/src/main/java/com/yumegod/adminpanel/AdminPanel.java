package com.yumegod.adminpanel;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.HashMap;
import java.util.Map;

public class AdminPanel {
    private static int centerButtonX(int inputX, int inputWidth, int buttonWidth) {
        return inputX + (inputWidth - buttonWidth) / 2;
    }

    public static boolean validateTime(String str) {
        int hyphenCount = 0;
        int colonCount = 0;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '-') {
                hyphenCount++;
            }
            if (ch == ':') {
                colonCount++;
            }
        }
        return hyphenCount == 2 && colonCount == 2;
    }

    public static void create() {
        JFrame adminFrame = new JFrame(Main.TITLE);
        adminFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        adminFrame.setBounds(Main.frame.getBounds()); // Use the same bounds as the login frame
        adminFrame.setLayout(null); // Disable the default layout manager

        // Title
        JLabel titleLabel = new JLabel("YumeCloud Protection", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Verdana", Font.BOLD, 30));
        titleLabel.setBounds(0, 10, 600, 60); // Full width, centered
        adminFrame.add(titleLabel);

        // Spacing constants
        int inputWidth = 200;
        int inputHeight = 30;
        int buttonWidth = 110; // Static width for buttons
        int buttonHeight = 30;
        int verticalSpacing = 5;
        int labelBottomMargin = 110;
        int labelHeight = 25;

        // Generate Key Section
        JLabel generateKeyLabel = new JLabel("Generate key:");
        generateKeyLabel.setBounds(50, 75, inputWidth, labelHeight);
        TextFieldWithPlaceHolder keyTimeInput = new TextFieldWithPlaceHolder("time of key (days)");
        keyTimeInput.setBounds(50, 75 + labelHeight, (int) (inputWidth * (2.0 / 3.0)), inputHeight);
        TextFieldWithPlaceHolder generateKeyInput = new TextFieldWithPlaceHolder("amount");
        generateKeyInput.setBounds(50 + (int) (inputWidth * (2.0 / 3.0)), 75 + labelHeight, inputWidth / 3, inputHeight);
        JButton generateKeyButton = new JButton("Generate");
        generateKeyButton.setBounds(centerButtonX(50, inputWidth, buttonWidth + 20), 75 + labelHeight + inputHeight + verticalSpacing, buttonWidth + 20, buttonHeight);

        // Ban Key Section
        JLabel banKeyLabel = new JLabel("Ban key:");
        banKeyLabel.setBounds(350, 75, inputWidth, labelHeight);
        TextFieldWithPlaceHolder banKeyInput = new TextFieldWithPlaceHolder("specific key to ban");
        banKeyInput.setBounds(350, 75 + labelHeight, inputWidth, inputHeight);
        JButton banKeyButton = new JButton("Ban");
        banKeyButton.setBounds(centerButtonX(350, inputWidth, buttonWidth), 75 + labelHeight + inputHeight + verticalSpacing, buttonWidth, buttonHeight);

        // Reset Section
        JLabel resetLabel = new JLabel("Reset key:");
        resetLabel.setBounds(50, 75 + labelBottomMargin, inputWidth, labelHeight);
        TextFieldWithPlaceHolder resetInput = new TextFieldWithPlaceHolder("specific key to reset");
        resetInput.setBounds(50, 75 + labelBottomMargin + labelHeight, inputWidth, inputHeight);
        JButton resetButton = new JButton("Reset");
        resetButton.setBounds(centerButtonX(50, inputWidth, buttonWidth), 75 + labelBottomMargin + labelHeight + inputHeight + verticalSpacing, buttonWidth, buttonHeight);

        // Last Login Section
        JLabel lastLoginLabel = new JLabel("Check last login time:");
        lastLoginLabel.setBounds(350, 75 + labelBottomMargin, inputWidth, labelHeight);
        TextFieldWithPlaceHolder lastLoginInput = new TextFieldWithPlaceHolder("specific key to check");
        lastLoginInput.setBounds(350, 75 + labelBottomMargin + labelHeight, inputWidth, inputHeight);
        JButton lastLoginButton = new JButton("Check");
        lastLoginButton.setBounds(centerButtonX(350, inputWidth, buttonWidth), 75 + labelBottomMargin + labelHeight + inputHeight + verticalSpacing, buttonWidth, buttonHeight);

        // Add components to frame
        adminFrame.add(titleLabel);
        adminFrame.add(generateKeyLabel);
        adminFrame.add(keyTimeInput);
        adminFrame.add(generateKeyInput);
        adminFrame.add(generateKeyButton);
        adminFrame.add(banKeyLabel);
        adminFrame.add(banKeyInput);
        adminFrame.add(banKeyButton);
        adminFrame.add(resetLabel);
        adminFrame.add(resetInput);
        adminFrame.add(resetButton);
        adminFrame.add(lastLoginLabel);
        adminFrame.add(lastLoginInput);
        adminFrame.add(lastLoginButton);

        generateKeyButton.addActionListener(actionEvent -> {
            keyTimeInput.setEnabled(false);
            generateKeyInput.setEnabled(false);
            generateKeyButton.setEnabled(false);
            int amount = 0;
            int time = 0;
            try {
                time = Integer.parseInt(keyTimeInput.getText());
                amount = Integer.parseInt(generateKeyInput.getText());
                if (amount < 1 || amount > 50) {
                    Main.showMessageDialog("You only can generate 1-50 keys in one time", 2);
                    keyTimeInput.setEnabled(true);
                    generateKeyInput.setEnabled(true);
                    generateKeyButton.setEnabled(true);
                    keyTimeInput.setText("");
                    generateKeyInput.setText("");
                    return;
                }
                if (time < 1 || time > 9999) {
                    Main.showMessageDialog("You only can generate keys with 1-9999 days", 2);
                    keyTimeInput.setEnabled(true);
                    generateKeyInput.setEnabled(true);
                    generateKeyButton.setEnabled(true);
                    keyTimeInput.setText("");
                    generateKeyInput.setText("");
                    return;
                }
            } catch (Exception e) {
                Main.showMessageDialog("Invalid input", 2);
                keyTimeInput.setEnabled(true);
                generateKeyInput.setEnabled(true);
                generateKeyButton.setEnabled(true);
                keyTimeInput.setText("");
                generateKeyInput.setText("");
                return;
            }
            Map<String, String> requestParams = new HashMap<>();
            requestParams.put("app", Main.Username);
            requestParams.put("password", Main.Password);
            requestParams.put("command", "Key " + amount + " " + time);
            String result = Main.sendPostRequest(Main.adminURL + "admin", requestParams);
            try {
                if (result.startsWith(Main.Username + "_")) {
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    Transferable contents = new StringSelection(result);
                    clipboard.setContents(contents, null);

                    Main.showMessageDialog("Keys copied to clipboard!", 1);
                    keyTimeInput.setEnabled(true);
                    generateKeyInput.setEnabled(true);
                    generateKeyButton.setEnabled(true);
                    keyTimeInput.setText("");
                    generateKeyInput.setText("");
                } else {
                    Main.showMessageDialog(result, 2);
                    keyTimeInput.setEnabled(true);
                    generateKeyInput.setEnabled(true);
                    generateKeyButton.setEnabled(true);
                    keyTimeInput.setText("");
                    generateKeyInput.setText("");
                }
            } catch (Exception e) {
                Main.showMessageDialog(result, 2);
                keyTimeInput.setEnabled(true);
                generateKeyInput.setEnabled(true);
                generateKeyButton.setEnabled(true);
                keyTimeInput.setText("");
                generateKeyInput.setText("");
            }
        });

        banKeyButton.addActionListener(actionEvent -> {
            banKeyInput.setEnabled(false);
            banKeyButton.setEnabled(false);
            if (banKeyInput.getText().isEmpty()) {
                Main.showMessageDialog("Key can't be empty", 2);
                banKeyInput.setEnabled(true);
                banKeyButton.setEnabled(true);
                banKeyInput.setText("");
                return;
            }
            Map<String, String> requestParams = new HashMap<>();
            requestParams.put("app", Main.Username);
            requestParams.put("password", Main.Password);
            requestParams.put("command", "Ban " + banKeyInput.getText());
            String result = Main.sendPostRequest(Main.adminURL + "admin", requestParams);
            if (result.equals("success")) {
                Main.showMessageDialog("Success!", 1);
                banKeyInput.setEnabled(true);
                banKeyButton.setEnabled(true);
                banKeyInput.setText("");
            } else {
                Main.showMessageDialog(result, 2);
                banKeyInput.setEnabled(true);
                banKeyButton.setEnabled(true);
                banKeyInput.setText("");
            }
        });

        resetButton.addActionListener(actionEvent -> {
            resetInput.setEnabled(false);
            resetButton.setEnabled(false);
            if (resetInput.getText().isEmpty()) {
                Main.showMessageDialog("Key can't be empty", 2);
                resetInput.setEnabled(true);
                resetButton.setEnabled(true);
                resetInput.setText("");
                return;
            }
            Map<String, String> requestParams = new HashMap<>();
            requestParams.put("app", Main.Username);
            requestParams.put("password", Main.Password);
            requestParams.put("command", "Reset " + resetInput.getText());
            String result = Main.sendPostRequest(Main.adminURL + "admin", requestParams);
            if (result.equals("success")) {
                Main.showMessageDialog("Success!", 1);
                resetInput.setEnabled(true);
                resetButton.setEnabled(true);
                resetInput.setText("");
            } else {
                Main.showMessageDialog(result, 2);
                resetInput.setEnabled(true);
                resetButton.setEnabled(true);
                resetInput.setText("");
            }
        });

        lastLoginButton.addActionListener(actionEvent -> {
            lastLoginInput.setEnabled(false);
            lastLoginButton.setEnabled(false);
            if (lastLoginInput.getText().isEmpty()) {
                Main.showMessageDialog("Key can't be empty", 2);
                lastLoginInput.setEnabled(true);
                lastLoginButton.setEnabled(true);
                lastLoginInput.setText("");
                return;
            }
            Map<String, String> requestParams = new HashMap<>();
            requestParams.put("app", Main.Username);
            requestParams.put("password", Main.Password);
            requestParams.put("command", "LastLogin " + lastLoginInput.getText());
            String result = Main.sendPostRequest(Main.adminURL + "admin", requestParams);
            if (validateTime(result)) {
                Main.showMessageDialog(result, 1);
                lastLoginInput.setEnabled(true);
                lastLoginButton.setEnabled(true);
                lastLoginInput.setText("");
            } else if (result.equals("null")) {
                Main.showMessageDialog("Key has never been used!", 1);
                lastLoginInput.setEnabled(true);
                lastLoginButton.setEnabled(true);
                lastLoginInput.setText("");
            } else {
                Main.showMessageDialog(result, 2);
                lastLoginInput.setEnabled(true);
                lastLoginButton.setEnabled(true);
                lastLoginInput.setText("");
            }
        });

        // Hide the login frame and show the admin frame
        Main.frame.setVisible(false);
        adminFrame.setResizable(false);
        adminFrame.setVisible(true);
    }
}