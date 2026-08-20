package com.yumegod.obfuscator.gui;

import java.awt.*;

public class SysNotification {
    static final TrayIcon notificationTray = new TrayIcon(Toolkit.getDefaultToolkit().createImage(new byte[0]), "Flow Breaker");

    public static void info(String title, String message) {
        if (loaded) {
            notificationTray.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    public static void warning(String title, String message) {
        if (loaded) {
            notificationTray.displayMessage(title, message, TrayIcon.MessageType.WARNING);
        }
    }

    public static void error(String title, String message) {
        if (loaded) {
            notificationTray.displayMessage(title, message, TrayIcon.MessageType.ERROR);
        }
    }

    public static void msg(String title, String message) {
        if (loaded) {
            notificationTray.displayMessage(title, message, TrayIcon.MessageType.NONE);
        }
    }

    static boolean loaded = false;
    static {
        try {
            if (SystemTray.isSupported()) {
                SystemTray.getSystemTray().add(notificationTray);
                loaded = true;
            }
        } catch (AWTException ignored) {}
    }
}

