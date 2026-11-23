package burp.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class TimingTable extends AbstractTableModel {

    private static class Row {
        final int messageId;
        final double elapsed;

        Row(int messageId, double elapsed) {
            this.messageId = messageId;
            this.elapsed = elapsed;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    private final String[] columns = {
            "Message ID",
            "Elapsed (ms)"
    };

    public void addTiming(int messageId, double elapsed) {
        rows.add(new Row(messageId, elapsed));
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return switch (column) {
            case 0 -> Integer.class;
            case 1 -> Double.class;
            default -> Object.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int col) {
        Row row = rows.get(rowIndex);
        return switch (col) {
            case 0 -> row.messageId;
            case 1 -> row.elapsed;
            default -> "";
        };
    }

    public List<Double> getAllTimings() {
        List<Double> result = new ArrayList<>(rows.size());
        for (Row r : rows) {
            result.add(r.elapsed);
        }
        return result;
    }

    public record ExportRow(int messageId, double elapsed) {}

    public List<ExportRow> getExportRows() {
        List<ExportRow> result = new ArrayList<>(rows.size());
        for (Row r : rows) {
            result.add(new ExportRow(r.messageId, r.elapsed));
        }
        return result;
    }
}
