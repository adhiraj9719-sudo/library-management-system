package com.library.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JPanel;

/** Minimal bar chart drawn with Java2D (borrowing trends). */
class BarChartPanel extends JPanel {
    private Map<String, Integer> data = new LinkedHashMap<>();
    private final String title;

    BarChartPanel(String title) {
        this.title = title;
        setPreferredSize(new Dimension(500, 280));
        setBackground(Color.WHITE);
    }

    void setData(Map<String, Integer> data) {
        this.data = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(title, 12, 20);
        if (data.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString("No borrowing data yet.", 12, 50);
            return;
        }
        int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int left = 40, bottom = getHeight() - 45, top = 40;
        int slot = (getWidth() - left - 20) / data.size();
        int barW = Math.max(12, slot - 16);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        int i = 0;
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            int h = (int) ((bottom - top) * (e.getValue() / (double) max));
            int x = left + i * slot + 8;
            g.setColor(new Color(52, 120, 200));
            g.fillRoundRect(x, bottom - h, barW, h, 6, 6);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.valueOf(e.getValue()), x + barW / 2 - 3, bottom - h - 4);
            String label = e.getKey().length() > 12 ? e.getKey().substring(0, 11) + "…" : e.getKey();
            g.drawString(label, x + barW / 2 - fm.stringWidth(label) / 2, bottom + 16);
            i++;
        }
        g.setColor(Color.GRAY);
        g.drawLine(left, bottom, getWidth() - 10, bottom);
    }
}
