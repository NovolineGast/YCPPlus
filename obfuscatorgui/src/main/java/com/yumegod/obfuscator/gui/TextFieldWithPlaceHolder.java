package com.yumegod.obfuscator.gui;

import javax.swing.*;
import java.awt.*;

public class TextFieldWithPlaceHolder extends JTextField {
    private String placeholder;

    public TextFieldWithPlaceHolder(String placeholder) {
        super();
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getText().isEmpty() && placeholder != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.GRAY);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(placeholder, getInsets().left, (getHeight() / 2) + (fm.getAscent() / 2) - 3);
            g2.dispose();
        }
    }
}