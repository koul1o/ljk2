package quizplatform2;

/**
 *
 * @author koul1o
 */
import java.net.URL;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class QuizPlatform2 extends Application {

    private static String[] arguments;
    double percent = 0.0;
    public Bridge bridge;
    final LongProperty startTime = new SimpleLongProperty();
    final LongProperty endTime = new SimpleLongProperty();
    final LongProperty elapsedTime = new SimpleLongProperty();
    int cnt2 = 0;
    private float tTime = 60;
    private float fTime = 20;
    private float step = 4;
    private String root = "html/math"  ;
    private static final String START_URL = "/Instructions.html";
    ProgressBar progressBar = new ProgressBar();
    private String experimentId = "00000";
    private Boolean highlightEnabled = false;

    @Override
    public void start(Stage primaryStage) {
        argsToProperties(arguments);
        /* Create the WebView and WebEngine */
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        setProperties();

        /* Initialize the Bridge */
        bridge = new Bridge(engine, primaryStage, this, tTime, fTime, step, root, experimentId);
        
        if(this.highlightEnabled){
        	webView.setContextMenuEnabled(false);
        	createContextMenu(webView, bridge);
        }
        
        /* Load the first Url */
        engine.load(getClass().getResource("/bin/quizplatform2/" + root + START_URL).toExternalForm());
        /* Enable JS in the WebEngine */
        engine.setJavaScriptEnabled(true);

        /* Create a progress bar */
        progressBar.setProgress(percent);

        /* Add progress bar and webView in top and center of a BorderPane */
        BorderPane borderPane = new BorderPane(webView, null, null, progressBar, null);

        /* Align the process bar on the center */
        borderPane.setAlignment(progressBar, Pos.CENTER);

        /* Set the scene containing the BorderPane we created and set the size of it */
        Scene scene = new Scene(borderPane, 1000, 800);

        /* Add custom css for the progress bar */
        URL url = this.getClass().getResource("caspian.css");
        if (url == null) {
            System.out.println("Resource not found. Aborting.");
            System.exit(-1);
        }
        String css = url.toExternalForm();
        scene.getStylesheets().add(css);
        progressBar.prefWidthProperty().bind(borderPane.widthProperty().subtract(20));

        /* Set the scene  */
        primaryStage.setScene(scene);
        try {
            if (System.getProperty("fullscreen").equals("y")) {
                primaryStage.initStyle(StageStyle.UNDECORATED);
                primaryStage.setFullScreen(true);
                primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            }
        } catch (Exception e) {
            System.out.println("Property Fullscreen missing. To change this parameter set fullscreen=y in run.bat");

        }

        primaryStage.show();

        primaryStage.setOnCloseRequest(exit());
    }

    /* Handles the platform exit. Collects the last trace prior to exit*/
 /* Handles the platform exit. Collects the last trace prior to exit*/
    public EventHandler<WindowEvent> exit() {
        return (WindowEvent event) -> {
            bridge.exit();
        };
    }

    /**
     * Convert command line arguments into system properties
     *
     * @param args
     */
    public static void argsToProperties(String[] args) {
        // Go through all the parameters

        for (int i = 0; i < args.length; i++) {
            String[] nameval = args[i].split("=");

            // Does it have the parameter format : name=value
            if (nameval.length == 2) {
                // Remove the '," and additional spaces if needed
                nameval[1].replace('"', ' ');
                nameval[1].replace('\'', ' ');
                // Set the property
                System.setProperty(nameval[0].trim(), nameval[1].trim());

            }
        }
    }

    public void setProperties() {

        try {
            tTime = Integer.parseInt(System.getProperty("tTime"));
        } catch (NumberFormatException e) {
            System.out.println("Property Training Time missing, default value set: " + tTime + "  To change this parameter set tTime=minutes in run.bat");
        }
        try {
            fTime = Integer.parseInt(System.getProperty("fTime"));
        } catch (NumberFormatException e) {
            System.out.println("Property Final Quiz Time missing, default value set: " + fTime + "  To change this parameter set fTime=minutes in run.bat");
        }
        try {
            step = Integer.parseInt(System.getProperty("step"));
        } catch (NumberFormatException e) {
            System.out.println("Property Step missing, default value set: " + step + "  To change this parameter set step=number of steps in run.bat");
        }
        try {
            if (!System.getProperty("root").isEmpty()) {
                this.root = "html/" + (String) System.getProperty("root");
            }
        } catch (NullPointerException e) {
            System.out.println("Property Root missing, default value set: " + root + "  To change this parameter set root=name (available folders: psych,math) of setup in run.bat");
        }
        try {
            if (!System.getProperty("experimentId").isEmpty()) {
                this.experimentId = (String) System.getProperty("experimentId");
            }
        } catch (NullPointerException e) {
            System.out.println("Property Experiment Id missing, default value set: " + this.experimentId + "  To change this parameter set experimentId=id of setup in run.bat");
        }
        try {
            if (!System.getProperty("highlightEnabled").isEmpty()) {
                this.highlightEnabled = Boolean.valueOf(System.getProperty("highlightEnabled"));
            }
        } catch (NullPointerException e) {
            System.out.println("Property highlight enabled missing, default value set: " + this.highlightEnabled + "  To change this parameter set highlightEnabled=boolean (true or false) of setup in run.bat");
        }

    }
    
    private void createContextMenu(WebView webView, Bridge bridge) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem highlight = new MenuItem("Highlight");
        highlight.setOnAction(e -> bridge.checkHighlight());
        contextMenu.getItems().addAll(highlight);

        webView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }

    public static void main(String[] args) {

        arguments = args;

        launch(args);
    }

}
