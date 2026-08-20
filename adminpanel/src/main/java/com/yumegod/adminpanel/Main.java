package com.yumegod.adminpanel;

import com.formdev.flatlaf.FlatDarculaLaf;
import sun.misc.Unsafe;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @Author: Yume
 * @Date: 2024/3/12 0:39
 */
public class Main {
    public static JFrame frame;
    public static JTextField usernameInput;
    public static JTextField adminURLInput;
    public static JTextField passwordInput;
    public static JButton loginButton;
    public static String TITLE = "YumeCloudProtection - Admin Panel";
    public static String Username;
    public static String Password;
    public static String adminURL;

    public static void showMessageDialog(Object message, int type) {
        Class<?> jOptionPaneClass;
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

    public static String sendPostRequest(String requestUrl, Map<String, String> params) {
        try {
            // Convert Map into URL query string
            StringJoiner sj = new StringJoiner("&");
            for (Map.Entry<String, String> entry : params.entrySet())
                sj.add(entry.getKey() + "=" + entry.getValue());

            byte[] postDataBytes = sj.toString().getBytes(StandardCharsets.UTF_8);

            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);

            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postDataBytes);
            }

            StringBuilder content;

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                content = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            } finally {
                conn.disconnect();
            }

            return content.toString().trim(); // Remove trailing newlines
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        //Initialize UI
        FlatDarculaLaf.setup();
        frame = new JFrame(TITLE);
        frame.getContentPane().setLayout(null);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        frame.setSize(600, 350);
        frame.setLocationRelativeTo(null);

        JLabel titleLabel = new JLabel("YumeCloud Protection", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Verdana", Font.BOLD, 30));
        titleLabel.setBounds(0, 10, 600, 30); // Full width, centered
        frame.add(titleLabel);

        //创建用户名
        JLabel usernameText = new JLabel("App name");
        usernameText.setBounds(70, 85, 100, 35);
        frame.getContentPane().add(usernameText);
        //用户名输入框
        usernameInput = new JTextField(20);
        usernameInput.setBounds(175, 85, 345, 35);
        frame.getContentPane().add(usernameInput);

        //创建密码
        JLabel passwordText = new JLabel("Password");
        passwordText.setBounds(70, 130, 100, 35);
        frame.getContentPane().add(passwordText);
        //密码输入框
        passwordInput = new JPasswordField(20);
        passwordInput.setBounds(175, 130, 345, 35);
        frame.getContentPane().add(passwordInput);

        //创建登录链接
        JLabel adminURLText = new JLabel("URL (Optional)");
        adminURLText.setBounds(45, 175, 150, 35);
        frame.getContentPane().add(adminURLText);
        //登录链接输入框
        adminURLInput = new JTextField(20);
        adminURLInput.setBounds(175, 175, 345, 35);
        frame.getContentPane().add(adminURLInput);

        //添加登录按钮
        loginButton = new JButton("Login");
        loginButton.setBounds(50, 230, 470, 35);
        frame.getContentPane().add(loginButton);

        //登录按钮编码
        loginButton.addActionListener(actionEvent -> {
            if (usernameInput.getText().isEmpty()) {
                showMessageDialog("Application name cannot be empty!", 2);
                return;
            }
            if (passwordInput.getText().isEmpty()) {
                showMessageDialog("Password cannot be empty!", 2);
                return;
            }

            Username = usernameInput.getText();
            Password = passwordInput.getText();
            adminURL = adminURLInput.getText().isEmpty() ? "http://protection.yumegod.com:13337/" : adminURLInput.getText();

            usernameInput.setEnabled(false);
            passwordInput.setEnabled(false);
            loginButton.setEnabled(false);

            loginButton.setText("Logging in...");

            try {
                Map<String, String> requestParams = new HashMap<>();
                requestParams.put("app", Username);
                requestParams.put("password", Password);
                String result = sendPostRequest(adminURL + "admin_login", requestParams);
                if (result.equals("success")) {
                    showMessageDialog("Login success!", 1);
                    AdminPanel.create();
                } else {
                    showMessageDialog(result, 2);
                    usernameInput.setEnabled(true);
                    passwordInput.setEnabled(true);
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showMessageDialog("Exception caught while logging-in", 2);
                usernameInput.setEnabled(true);
                passwordInput.setEnabled(true);
                loginButton.setEnabled(true);
                loginButton.setText("Login");
            }
        });
        frame.setResizable(false);
        frame.setVisible(true);
    }
}