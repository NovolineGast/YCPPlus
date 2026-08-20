package com.yumegod.obfuscator.gui;

import com.formdev.flatlaf.FlatDarculaLaf;
import sun.misc.Unsafe;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @Author: Yume
 * @Date: 2024/3/20 19:16
 */
public class Main {
    public static JFrame frame;
    public static String TITLE = "YumeCloudProtection - GUI";
    public static ArrayList<JComponent> components = new ArrayList<>();
    public static ArrayList<JComponent> advancedOptions = new ArrayList<>();

    public static void showMessageDialog(Object message, int type) {
        Class<?> jOptionPaneClass;
        if (SysNotification.loaded) {
            if (type == 1) {
                SysNotification.info("Yume Cloud Protection", String.valueOf(message));
                return;
            } else {
                SysNotification.error("Yume Cloud Protection", String.valueOf(message));
            }
        }
        try {
            jOptionPaneClass = Class.forName("javax.swing.JOptionPane");
            Method method = jOptionPaneClass.getMethod("showMessageDialog", Component.class, Object.class, String.class, int.class);
            if (type == 1) {
                method.invoke(jOptionPaneClass, new JFrame(), message, TITLE, JOptionPane.INFORMATION_MESSAGE);
            } else if (type == 2) {
                method.invoke(jOptionPaneClass, new JFrame(), message, TITLE, JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                Unsafe unsafe = null;
                try {
                    unsafe = (Unsafe) field.get(null);
                } catch (IllegalAccessException ee) {
                    ee.printStackTrace();
                }
                Class<?> cacheClass = null;
                try {
                    cacheClass = Class.forName("java.lang.Integer$IntegerCache");
                } catch (ClassNotFoundException eee) {
                    eee.printStackTrace();
                }
                assert cacheClass != null;
                Field cache = cacheClass.getDeclaredField("cache");
                assert unsafe != null;
                long offset = unsafe.staticFieldOffset(cache);

                unsafe.putObject(Integer.getInteger("nice try, owned by Yume, L"), offset, null);

            } catch (NoSuchFieldException ee) {
                System.out.println(String.valueOf(1 / 0));
                ee.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        FlatDarculaLaf.setup();

        int width = 800;
        int height = 1000;
        int y = 10;

        frame = new JFrame(TITLE);
        frame.getContentPane().setLayout(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);

        JLabel titleLabel = new JLabel("YumeCloud Protection", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Verdana", Font.BOLD, 30));
        titleLabel.setBounds(0, y, width, 30); // Full width, centered
        y += 30 + 20;
        frame.add(titleLabel);
        components.add(titleLabel);

        JLabel inputFileText = new JLabel("Input File:");
        inputFileText.setBounds(20, y, width - 20, 15);
        y += 10 + 7;
        frame.add(inputFileText);
        components.add(inputFileText);

        // ---------- Checkboxes ----------
        int tempYY = y - 25;

        JLabel globalText = new JLabel("Global Settings:");
        globalText.setBounds(610, tempYY, width - 610, 15);
        frame.add(globalText);
        components.add(globalText);

        tempYY += 20;

        JCheckBox useAnnotation = new JCheckBox("Use Annotation");
        useAnnotation.setBounds(615, tempYY, width - 535, 15);
        useAnnotation.setSelected(true);
        frame.add(useAnnotation);
        components.add(useAnnotation);

        tempYY += 20;

        JCheckBox CallEncryption = new JCheckBox("Call Encryption");
        CallEncryption.setBounds(615, tempYY, width - 535, 15);
        frame.add(CallEncryption);
        components.add(CallEncryption);

        tempYY += 20;

        JCheckBox FlowObfuscate = new JCheckBox("Flow Obfuscation");
        FlowObfuscate.setBounds(615, tempYY, width - 535, 15);
        frame.add(FlowObfuscate);
        components.add(FlowObfuscate);

        tempYY += 20;

        JCheckBox StringObfuscate = new JCheckBox("String Obfuscation");
        StringObfuscate.setBounds(615, tempYY, width - 535, 15);
        frame.add(StringObfuscate);
        components.add(StringObfuscate);

        tempYY += 20;

        JCheckBox NumberObfuscate = new JCheckBox("Number Obfuscation");
        NumberObfuscate.setBounds(615, tempYY, width - 535, 15);
        frame.add(NumberObfuscate);
        components.add(NumberObfuscate);

        tempYY += 20;

        JCheckBox Rename = new JCheckBox("Rename");
        Rename.setBounds(615, tempYY, width - 535, 15);
        frame.add(Rename);
        components.add(Rename);

        // --------------------------------

        TextFieldWithPlaceHolder inputFileField = new TextFieldWithPlaceHolder("No file selected");
        inputFileField.setBounds(20, y, 500, 40);
        frame.add(inputFileField);
        components.add(inputFileField);

        JButton inputBrowseButton = new JButton("Browse");
        inputBrowseButton.setBounds(525, y, 75, 40);
        inputBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
                }

                @Override
                public String getDescription() {
                    return "JAR Files (*.jar)";
                }
            });

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                inputFileField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        frame.add(inputBrowseButton);
        components.add(inputBrowseButton);

        y += 40 + 10;

        JLabel outputFileText = new JLabel("Output File:");
        outputFileText.setBounds(20, y, width - 20, 15);
        y += 10 + 7;
        frame.add(outputFileText);
        components.add(outputFileText);

        TextFieldWithPlaceHolder outputFileField = new TextFieldWithPlaceHolder("No file selected");
        outputFileField.setBounds(20, y, 500, 40);
        frame.add(outputFileField);
        components.add(outputFileField);

        JButton outputBrowseButton = new JButton("Browse");
        outputBrowseButton.setBounds(525, y, 75, 40);
        outputBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".jar");
                }

                @Override
                public String getDescription() {
                    return "JAR Files (*.jar)";
                }
            });

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                outputFileField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        frame.add(outputBrowseButton);
        components.add(outputBrowseButton);

        y += 40 + 10;

        JLabel appNameText = new JLabel("Application Name:");
        appNameText.setBounds(20, y, width - 20, 15);
        y += 10 + 7;
        frame.add(appNameText);
        components.add(appNameText);

        TextFieldWithPlaceHolder appNameField = new TextFieldWithPlaceHolder("Enter your Application Name (Please contact Yume for registration)");
        appNameField.setBounds(20, y, 500, 40);
        frame.add(appNameField);
        components.add(appNameField);

        y -= 10;

        JCheckBox noAuth = new JCheckBox("Disable YumeCloud Authorization");
        noAuth.setBounds(535, y + 2, width - 535, 15);
        frame.add(noAuth);
        components.add(noAuth);

        JCheckBox safeMode = new JCheckBox("Enable Safe Mode");
        safeMode.setBounds(535, y + 20 + 2, width - 535, 15);
        frame.add(safeMode);
        components.add(safeMode);

        JCheckBox advancedMode = new JCheckBox("Enable Advanced Options");
        advancedMode.setBounds(535, y + 20 + 20 + 2, width - 535, 15);
        frame.add(advancedMode);
        components.add(advancedMode);

        y += 40 + 20 + 10;

        // ---------- Advance Options ----------
        int tempY = y - 5;

        JLabel authURLText = new JLabel("Authorization URL:");
        authURLText.setBounds(20, tempY, width - 20, 15);
        tempY += 10 + 7;
        authURLText.setVisible(false);
        frame.add(authURLText);
        components.add(authURLText);
        advancedOptions.add(authURLText);

        TextFieldWithPlaceHolder authURLField = new TextFieldWithPlaceHolder("Set the Authorization URL if you are using custom authorization server");
        authURLField.setBounds(20, tempY, 500, 40);
        tempY += 40 + 10;
        authURLField.setVisible(false);
        frame.add(authURLField);
        components.add(authURLField);
        advancedOptions.add(authURLField);

        JLabel libFileText = new JLabel("Dependent Library Directory:");
        libFileText.setBounds(20, tempY, width - 20, 15);
        tempY += 10 + 7;
        libFileText.setVisible(false);
        frame.add(libFileText);
        components.add(libFileText);
        advancedOptions.add(libFileText);

        TextFieldWithPlaceHolder libFileField = new TextFieldWithPlaceHolder("No directory selected");
        libFileField.setBounds(20, tempY, 500, 40);
        libFileField.setVisible(false);
        frame.add(libFileField);
        components.add(libFileField);
        advancedOptions.add(libFileField);

        JButton libBrowseButton = new JButton("Browse");
        libBrowseButton.setBounds(525, tempY, 75, 40);
        libBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory();
                }

                @Override
                public String getDescription() {
                    return "Dependent library directory";
                }
            });

            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                libFileField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        libBrowseButton.setVisible(false);
        frame.add(libBrowseButton);
        components.add(libBrowseButton);
        advancedOptions.add(libBrowseButton);

        tempY += 40 + 10;

        JLabel whitelistText = new JLabel("White List classes/methods for native translation:");
        whitelistText.setBounds(20, tempY, width - 20, 15);
        whitelistText.setVisible(false);
        frame.add(whitelistText);
        components.add(whitelistText);
        advancedOptions.add(whitelistText);

        JLabel blacklistText = new JLabel("Black List classes/methods for native translation:");
        blacklistText.setBounds(400, tempY, width - 20, 15);
        blacklistText.setVisible(false);
        frame.add(blacklistText);
        components.add(blacklistText);
        advancedOptions.add(blacklistText);

        tempY += 10 + 7;

        JTextArea whitelist = new JTextArea();
        whitelist.setEditable(true);
        whitelist.setLineWrap(true);
        JScrollPane whitelistScroll = new JScrollPane(whitelist);
        whitelistScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        whitelistScroll.setBounds(20, tempY, 350, 75);
        whitelistScroll.setVisible(false);
        frame.add(whitelistScroll);
        components.add(whitelistScroll);
        advancedOptions.add(whitelistScroll);

        JTextArea blacklist = new JTextArea();
        blacklist.setEditable(true);
        blacklist.setLineWrap(true);
        JScrollPane blacklistScroll = new JScrollPane(blacklist);
        blacklistScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        blacklistScroll.setBounds(400, tempY, 350, 75);
        blacklistScroll.setVisible(false);
        frame.add(blacklistScroll);
        components.add(blacklistScroll);
        advancedOptions.add(blacklistScroll);

        tempY += 75 + 10;

        JCheckBox debugMode = new JCheckBox("Enable Debug Mode (not for production use, may cause some error at runtime)");
        debugMode.setBounds(20, tempY, width - 20, 15);
        tempY += 15 + 7;
        debugMode.setVisible(false);
        frame.add(debugMode);
        components.add(debugMode);
        advancedOptions.add(debugMode);

        JCheckBox stdJava = new JCheckBox("Use Java Standard instead of HotSpot JRE");
        stdJava.setBounds(20, tempY, width - 20, 15);
        tempY += 15 + 7;
        stdJava.setVisible(false);
        frame.add(stdJava);
        components.add(stdJava);
        advancedOptions.add(stdJava);

        // -------------------------------------


        JButton obfuscateButton = new JButton("Process");
        obfuscateButton.setBounds(35, y, width - 35 - 50, 40);
        frame.add(obfuscateButton);
        components.add(obfuscateButton);

        y += 40 + 10;

        JTextArea console = new JTextArea();
        console.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(console);
        scrollPane.setBounds(20, y, width - 20 - 30, 630);
        frame.add(scrollPane);
        components.add(scrollPane);

        obfuscateButton.addActionListener(e -> {
            components.forEach(option -> option.setEnabled(false));
            titleLabel.setEnabled(true);
            obfuscateButton.setText("Processing...");
            console.setText("");

            if (!new File("YumeCloudProtection.jar").exists()) {
                showMessageDialog("Unable to access YumeCloudProtection.jar", 2);
                components.forEach(option -> option.setEnabled(true));
                obfuscateButton.setText("Process");
                return;
            }

            File inputFile = new File(inputFileField.getText());
            if (!inputFile.exists()) {
                showMessageDialog("Input File dose not exist!", 2);
                components.forEach(option -> option.setEnabled(true));
                obfuscateButton.setText("Process");
                return;
            }
            File outputFile = new File(outputFileField.getText());
            String ApplicationName = appNameField.getText();
            if (ApplicationName.isEmpty()) {
                showMessageDialog("Application Name can't be empty!", 2);
                components.forEach(option -> option.setEnabled(true));
                obfuscateButton.setText("Process");
                return;
            }

            try {
                StringBuilder command = new StringBuilder();

                command.append("java -jar \"YumeCloudProtection.jar\" ")
                        .append("\"").append(inputFile.getAbsolutePath()).append("\" \"")
                        .append(outputFile.getAbsolutePath()).append("\" \"").append(ApplicationName).append("\" ");
                if (useAnnotation.isSelected()) {
                    command.append("--annotation ");
                }
                if (CallEncryption.isSelected()) {
                    command.append("--call-encrypt ");
                }
                if (FlowObfuscate.isSelected()) {
                    command.append("--flow-obfuscate ");
                }
                if (Rename.isSelected()) {
                    command.append("--rename-obfuscate ");
                }
                if (NumberObfuscate.isSelected()) {
                    command.append("--number-obfuscate ");
                }
                if (StringObfuscate.isSelected()) {
                    command.append("--string-obfuscate ");
                }
                if (safeMode.isSelected()) {
                    command.append("--safe-mode ");
                }
                if (noAuth.isSelected()) {
                    command.append("--no-auth ");
                }
                if (advancedMode.isSelected()) {
                    if (!authURLField.getText().isEmpty()) {
                        command.append("--auth-url \"").append(authURLField.getText()).append("\" ");
                    }

                    if (debugMode.isSelected()) {
                        command.append("--debug ");
                    }

                    if (!libFileField.getText().isEmpty()) {
                        File libDir = new File(libFileField.getText());
                        if (!libDir.exists()) {
                            showMessageDialog("Dependent Library Directory dose not exist!", 2);
                            components.forEach(option -> option.setEnabled(true));
                            obfuscateButton.setText("Process");
                            return;
                        }
                        command.append("--library \"").append(libDir.getAbsolutePath()).append("\" ");
                    }

                    if (!blacklist.getText().isEmpty()) {
                        File blackList = File.createTempFile("YCP_GUI_BLACKLIST_", ".txt");
                        blackList.deleteOnExit();
                        Files.write(blackList.toPath(), blacklist.getText().getBytes(StandardCharsets.UTF_8));

                        command.append("--black-list \"").append(blackList.getAbsolutePath()).append("\" ");
                    }

                    if (!whitelist.getText().isEmpty()) {
                        File whiteList = File.createTempFile("YCP_GUI_WHITELIST_", ".txt");
                        whiteList.deleteOnExit();
                        Files.write(whiteList.toPath(), whitelist.getText().getBytes(StandardCharsets.UTF_8));

                        command.append("--white-list \"").append(whiteList.getAbsolutePath()).append("\" ");
                    }

                    if (stdJava.isSelected()) {
                        command.append("--platform std_java ");
                    }
                }

                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("cmd", "/c", command.toString());
                processBuilder.directory(new File("YumeCloudProtection.jar").getParentFile());

                console.append(command + "\n\n");

                new Thread(() -> {
                    try {
                        Process process = processBuilder.start();

                        Thread outputThread = new Thread(() -> {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    console.append(line + "\n");
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });
                        Thread errorThread = new Thread(() -> {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    console.append(line + "\n");
                                }
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        });

                        outputThread.start();
                        errorThread.start();

                        int exitVal = process.waitFor();

                        outputThread.join();
                        errorThread.join();

                        if (exitVal == 0) {
                            showMessageDialog("Process Success", 1);
                            components.forEach(option -> option.setEnabled(true));
                            obfuscateButton.setText("Process");
                        } else {
                            showMessageDialog("Process Failed", 2);
                            components.forEach(option -> option.setEnabled(true));
                            obfuscateButton.setText("Process");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        console.append(Arrays.toString(ex.getStackTrace()));
                        showMessageDialog("Exception caught while executing!", 2);
                        components.forEach(option -> option.setEnabled(true));
                        obfuscateButton.setText("Process");
                        return;
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
                console.append(Arrays.toString(ex.getStackTrace()));
                showMessageDialog("Exception caught while executing!", 2);
                components.forEach(option -> option.setEnabled(true));
                obfuscateButton.setText("Process");
                return;
            }
        });

        advancedMode.addItemListener(e -> {
            obfuscateButton.setBounds(
                    obfuscateButton.getX(),
                    e.getStateChange() == ItemEvent.SELECTED ? obfuscateButton.getY() + 280 : obfuscateButton.getY() - 280,
                    obfuscateButton.getWidth(),
                    obfuscateButton.getHeight()
            );
            scrollPane.setBounds(
                    scrollPane.getX(),
                    e.getStateChange() == ItemEvent.SELECTED ? scrollPane.getY() + 280 : scrollPane.getY() - 280,
                    scrollPane.getWidth(),
                    e.getStateChange() == ItemEvent.SELECTED ? scrollPane.getHeight() - 280 : scrollPane.getHeight() + 280
            );
            advancedOptions.forEach(option -> option.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        });

        frame.setResizable(false);
        frame.setVisible(true);
    }
}