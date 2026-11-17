package burp.logic;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PlotService {

    private XChartPanel<XYChart> lastChartPanel = null;

    public XChartPanel<XYChart> buildPairedScatter(List<Double> existing, List<Double> nonExisting) {
        int n = Math.min(existing.size(), nonExisting.size());
        List<Double> x = IntStream.rangeClosed(1, n)
                .mapToObj(i -> (double) i)
                .collect(Collectors.toList());

        List<Double> existingSubset = existing.subList(0, n);
        List<Double> nonExistingSubset = nonExisting.subList(0, n);

        XYChart chart = new XYChartBuilder()
                .width(900).height(400)
                .title("HTTP Response Times")
                .xAxisTitle("Request Index (paired)")
                .yAxisTitle("Time (ms)")
                .build();

        chart.getStyler().setMarkerSize(6);
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setLegendFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        chart.getStyler().setChartTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        chart.getStyler().setAxisTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        chart.getStyler().setAxisTickLabelsFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotGridLinesColor(new Color(220, 220, 220));

        XYSeries seriesExisting = chart.addSeries("Pool A", x, existingSubset);
        seriesExisting.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);

        XYSeries seriesNonExisting = chart.addSeries("Pool B", x, nonExistingSubset);
        seriesNonExisting.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);

        lastChartPanel = new XChartPanel<>(chart);
        return lastChartPanel;
    }

    public void saveLastPlot(Component parent) {
        if (lastChartPanel == null)
            throw new IllegalStateException("No plot to save.");

        XYChart chart = lastChartPanel.getChart();

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Plot");
        chooser.setSelectedFile(new File("timing_plot.png"));

        int rc = chooser.showSaveDialog(parent);

        if (rc == JFileChooser.CANCEL_OPTION || rc == JFileChooser.ERROR_OPTION) {
            return;
        }

        File f = chooser.getSelectedFile();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                BitmapEncoder.saveBitmap(chart, f.getAbsolutePath(), BitmapEncoder.BitmapFormat.PNG);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(
                            parent,
                            "Plot saved successfully:\n" + f.getAbsolutePath(),
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(parent, "Error saving plot: " + cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void clear() {
        lastChartPanel = null;
    }

}
