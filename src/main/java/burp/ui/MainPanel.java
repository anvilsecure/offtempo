package burp.ui;

import burp.logic.PlotService;
import burp.logic.StatsService;
import burp.model.TimingAnalysisResult;
import burp.model.TimingTable;
import burp.logic.Winsorizer;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainPanel {

    private final JPanel root;
    private final Component suiteFrame;
    private final TimingTable existingModel;
    private final TimingTable nonExistingModel;

    private final ControlPanel controlPanel;
    private final TimingTablesPanel tablesPanel;
    private final AnalysisPanel analysisPanel;
    private final PlotContainerPanel plotPanel;

    private final StatsService statsService;
    private final PlotService plotService;

    private volatile SwingWorker<TimingAnalysisResult, Void> analysisWorker;

    public MainPanel(StatsService statsService, PlotService plotService, Component suiteFrame) {
        this.statsService = statsService;
        this.plotService = plotService;
        this.suiteFrame = suiteFrame;

        existingModel = new TimingTable();
        nonExistingModel = new TimingTable();

        root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        controlPanel = new ControlPanel(e -> showHelp(), e -> clearPoolA(), e -> clearPoolB(), e -> clearTables(), e -> exportCsv());
        tablesPanel = new TimingTablesPanel(existingModel, nonExistingModel);
        plotPanel = new PlotContainerPanel();
        analysisPanel = new AnalysisPanel(e -> runAnalysis(), e -> savePlot());

        root.add(controlPanel, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(analysisPanel, BorderLayout.NORTH);
        bottom.add(plotPanel, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tablesPanel,
                bottom
        );

        split.setResizeWeight(0.5);
        split.setContinuousLayout(true);
        split.setDividerSize(8);

        root.add(split, BorderLayout.CENTER);
    }

    public JPanel getRootPanel() { return root; }

    private void showHelp() {
        HelpPanel hp = new HelpPanel();
        JOptionPane.showMessageDialog(suiteFrame, hp, "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void runAnalysis() {
        List<Double> existingTimes = existingModel.getAllTimings();
        List<Double> nonExistingTimes = nonExistingModel.getAllTimings();
        if (existingTimes.isEmpty() || nonExistingTimes.isEmpty()) {
            JOptionPane.showMessageDialog(suiteFrame, "No data available", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        analysisPanel.setRunEnabled(false);

        analysisWorker = new SwingWorker<>() {
            @Override
            protected TimingAnalysisResult doInBackground() {
                double[] a = existingTimes.stream().mapToDouble(Double::doubleValue).toArray();
                double[] b = nonExistingTimes.stream().mapToDouble(Double::doubleValue).toArray();
                return statsService.computeStats(a, b);
            }

            @Override
            protected void done() {
                analysisWorker = null;
                if (isCancelled()) return;
                try {
                    analysisPanel.showResult(get());
                    showPlot();
                    analysisPanel.showSaveButton();
                } catch (InterruptedException | ExecutionException ex) {
                    JOptionPane.showMessageDialog(suiteFrame, "Analysis failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    analysisPanel.setRunEnabled(true);
                }
            }
        };
        analysisWorker.execute();
    }

    private void showPlot() {
        try {
            if (existingModel.getAllTimings().isEmpty() || nonExistingModel.getAllTimings().isEmpty()) {
                return;
            }
            var existing = Winsorizer.winsorize(existingModel.getAllTimings(), 0.95);
            var nonExisting = Winsorizer.winsorize(nonExistingModel.getAllTimings(), 0.95);
            XChartPanel<XYChart> panel = plotService.buildPairedScatter(existing, nonExisting);
            plotPanel.showPlot(panel);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(suiteFrame, "Error creating plot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePlot() {
        try {
            if (plotPanel.getLastPanel() == null) {
                JOptionPane.showMessageDialog(suiteFrame, "No plot available. Generate a plot first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            plotService.saveLastPlot(suiteFrame);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(suiteFrame, "Error saving plot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isCaptureEnabled() { return controlPanel.isCaptureEnabled(); }
    public String getSelectedType() { return controlPanel.getSelectedType(); }
    public void addTiming(String targetType, int burpMessageId, double elapsedMs) {
        SwingUtilities.invokeLater(() -> {
            if ("Pool A".equals(targetType)) existingModel.addTiming(burpMessageId, elapsedMs);
            else nonExistingModel.addTiming(burpMessageId, elapsedMs);
            syncControls(existingModel.getRowCount(), nonExistingModel.getRowCount());
        });
    }

    private void exportCsv() {
        if (existingModel.getRowCount() == 0 && nonExistingModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(suiteFrame, "No data to export.", "Export CSV", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new java.io.File("offtempo_timings.csv"));

        if (chooser.showSaveDialog(suiteFrame) != JFileChooser.APPROVE_OPTION) return;

        java.io.File selected = chooser.getSelectedFile();
        java.io.File file = selected.getName().endsWith(".csv")
                ? selected
                : new java.io.File(selected.getAbsolutePath() + ".csv");

        // Snapshot rows on the EDT as table models are not thread-safe and could keep receiving timing data
        List<TimingTable.ExportRow> rowsA = existingModel.getExportRows();
        List<TimingTable.ExportRow> rowsB = nonExistingModel.getExportRows();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                    w.write("Pool,Message ID,Elapsed (ms)\n");
                    for (TimingTable.ExportRow row : rowsA) {
                        w.write(String.format(Locale.ROOT, "A,%d,%.3f\n", row.messageId(), row.elapsed()));
                    }
                    for (TimingTable.ExportRow row : rowsB) {
                        w.write(String.format(Locale.ROOT, "B,%d,%.3f\n", row.messageId(), row.elapsed()));
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(suiteFrame, "Error writing file: " + cause.getMessage(), "Export CSV", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void clearPoolA() {
        existingModel.clear();
        syncControls(0, nonExistingModel.getRowCount());
    }

    private void clearPoolB() {
        nonExistingModel.clear();
        syncControls(existingModel.getRowCount(), 0);
    }

    private void clearTables() {
        existingModel.clear();
        nonExistingModel.clear();
        plotPanel.clearPlot();
        analysisPanel.clearResult();
        syncControls(0, 0);
    }

    private void syncControls(int countA, int countB) {
        controlPanel.updateCounts(countA, countB);
        analysisPanel.setRunEnabled(countA > 0 && countB > 0);
    }

    public void dispose() {
        SwingWorker<TimingAnalysisResult, Void> worker = analysisWorker;
        if (worker != null) worker.cancel(true);
        SwingUtilities.invokeLater(() -> {
            clearTables();
            plotService.clear();
        });
    }
}
