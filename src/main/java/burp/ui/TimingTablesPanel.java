package burp.ui;

import burp.model.TimingTable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class TimingTablesPanel extends JPanel {

    public TimingTablesPanel(TimingTable existing, TimingTable nonExisting) {
        super(new GridLayout(1, 2, 20, 0));

        JTable existingTable = new JTable(existing);
        JTable nonExistingTable = new JTable(nonExisting);

        existingTable.setRowSorter(new TableRowSorter<>(existing));
        nonExistingTable.setRowSorter(new TableRowSorter<>(nonExisting));

        leftAlignNumbers(existingTable);
        leftAlignNumbers(nonExistingTable);

        JScrollPane leftScroll = new JScrollPane(existingTable);
        JScrollPane rightScroll = new JScrollPane(nonExistingTable);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(leftScroll, BorderLayout.CENTER);
        leftPanel.setBorder(BorderFactory.createTitledBorder("Pool A"));
        leftPanel.setPreferredSize(new Dimension(0, 300));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(rightScroll, BorderLayout.CENTER);
        rightPanel.setBorder(BorderFactory.createTitledBorder("Pool B"));
        rightPanel.setPreferredSize(new Dimension(0, 300));

        add(leftPanel);
        add(rightPanel);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
    }

    private static void leftAlignNumbers(JTable table) {
        for (Class<?> type : new Class<?>[]{Integer.class, Double.class}) {
            if (table.getDefaultRenderer(type) instanceof DefaultTableCellRenderer renderer) {
                renderer.setHorizontalAlignment(SwingConstants.LEFT);
            }
        }
    }
}