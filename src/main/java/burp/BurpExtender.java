package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.UserInterface;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.BurpExtension;
import burp.handlers.TimingHttpHandler;
import burp.logic.PlotService;
import burp.logic.StatsService;
import burp.ui.MainPanel;
import java.awt.Component;

public class BurpExtender implements BurpExtension, ExtensionUnloadingHandler {

    private MontoyaApi api;
    private volatile MainPanel mainPanel;
    private volatile TimingHttpHandler httpHandler;
    private final String extensionName = "OffTempo";

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName(extensionName);
        api.extension().registerUnloadingHandler(this);
        StatsService statsService = new StatsService();
        PlotService plotService = new PlotService();

        javax.swing.SwingUtilities.invokeLater(() -> {
            UserInterface ui = api.userInterface();
            Component suiteFrame = ui.swingUtils().suiteFrame();
            mainPanel = new MainPanel(statsService, plotService, suiteFrame);
            httpHandler = new TimingHttpHandler(mainPanel);
            ui.registerSuiteTab(extensionName, mainPanel.getRootPanel());
            api.http().registerHttpHandler(httpHandler);
        });
    }

    @Override
    public void extensionUnloaded() {
        if (httpHandler != null) httpHandler.clear();
        if (mainPanel != null) mainPanel.dispose();
    }
}