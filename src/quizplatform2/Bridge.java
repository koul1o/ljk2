package quizplatform2;

/**
 *
 * @author koul1o
 */
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class Bridge {

    private static final String QUESTION_NAME = "question";

    int time;
    private int cnt ,cnt2 = 1;
    private JSObject window;
    private String title;
    private WebEngine engine;
    String qUrl = null;
    String nextUrl = null;

    HashMap<String, String> quizLinks;

    public Bridge(WebEngine engine, Stage stage) {
        time = -1100;
        final LongProperty startTime = new SimpleLongProperty();
        final LongProperty endTime = new SimpleLongProperty();
        final LongProperty elapsedTime = new SimpleLongProperty();
        this.quizLinks = new HashMap<String, String>();
        engine.getLoadWorker().stateProperty().addListener((ObservableValue<? extends State> obs, State oldState, State newState) -> {

            switch (newState) {
                case RUNNING:
                    startTime.set(System.nanoTime());
                    break;

                case SUCCEEDED:
                    endTime.set(System.nanoTime());
                    elapsedTime.bind(Bindings.subtract(endTime, startTime));
                    if (cnt2>0){
                        time=(int) (time+elapsedTime.divide(1_000_000).getValue());
                        System.out.println("Time: "+time+" Elapsed t: "+elapsedTime.divide(1_000_000).getValue() );
                    }
                    cnt2++;
                    break;
            }

            if (newState == State.SUCCEEDED) {

                this.engine = engine;
                window = (JSObject) engine.executeScript("window");
                /* Register our java app to the window so that we can make upcalls to the app using java.functionName(); */
                window.setMember("java", this);

                title = engine.getTitle();
                stage.setTitle(engine.getTitle());
                /* */
                if (engine != null) {
                    /* Update the global time passed everytime we load a new page */
                    engine.executeScript("var time=" + time + "");
                    /* Check if we are in a document page and format the url removing the file:// prefix */
                    if (title.toLowerCase().contains(QUESTION_NAME)) {
                        qUrl = engine.getLocation();
                        qUrl = qUrl.replace("file://", "");
                        engine.executeScript("var qUrl=\'" + qUrl + "\'");
                        engine.executeScript("sendTrace()");

                        // nextUrl=URLToNextQuestion(qUrl);
                    } else if (title.toLowerCase().contains("quiz")) {
                        engine.executeScript("sendTrace()");
                    } else {
                        engine.executeScript("var qUrl=\'" + qUrl + "\'");
                    }

                }
            }
        });
    }

//    public long getTime() {
//        return time;
//    }
//
//    public void setTime(long time) {
//        this.time = time;
//    }

      
    
    /* Upcall to this function from the page, to update the global time passed  */
    public void updateTime(int time) {
        this.time = time;
        //System.out.println("Exit time: "+time);
    }

    /* Function, to exit the platform */
    public void exit() {
        //lastTrace();
        Platform.exit();

    }

    /* Upcall to this function from the page, to get the interaction trace */
    public void getTrace(String j) {
        System.out.println("Trace: " + j);
        saveData(j);

    }

    /* Upcall to this function from the page, to get the last trace on window exit */
    public void exitTrace() {
        engine.executeScript("quit();");
    }

    public void lastTrace() {
        engine.executeScript("quit();");
    }

    public void finalQuiz() {
        engine.executeScript("finalQuiz();");
    }

    /* Upcall to this function from the page, to update the next question Url for a document quiz */
    /**
     * This function changes the String <b>quizUrl</b> by adding 1 to the number
     * of the quiz. So C://example/document1_quiz1.html would become
     * C://example/document1_quiz2.html.<br>
     * It takes the path to the html file then parses it and changes only the
     * last number. <br>
     * It then changes the entry of the document in the <b>quizLinks</b> hashmap
     * so that the value matches the path of the next question. <br>
     * <br>
     * If the resulting file does not exist, the url is set to the list of
     * documents. <br>
     *
     * @param quizUrl - the url to save
     */
    public void URLToNextQuestion(String quizUrl) {
        cnt++;
        String finalUrl = "";
        File f = new File(quizUrl);
        if (f.exists() && !f.isDirectory()) {
            String s[] = quizUrl.split("/");
            String fs[] = quizUrl.split("/");

            quizUrl = "";
            s[s.length - 1] = "question" + cnt + ".html";
            fs[s.length - 1] = "final_quiz.html";
            int i = 0;
            for (i = 0; i < s.length; i++) {
                if (i != 0) {
                    quizUrl = quizUrl + "/" + s[i];
                    finalUrl = finalUrl + "/" + fs[i];
                } else {
                    quizUrl = quizUrl + s[i];
                    finalUrl = finalUrl + fs[i];
                }
                File fn = new File(quizUrl);
                if (fn.exists() && !fn.isDirectory()) {
                    engine.executeScript("var nextUrl=\'" + quizUrl + "\'");
                } else {
                    engine.executeScript("var nextUrl=\'" + finalUrl + "\'");
                }

            }

        }
        // return null;

    }

    /* This function redirects us to the next question while in the quiz */
    public void redirect(String url) {
        engine.executeScript("window.location.replace(\'" + url + "\');");
    }

    /**
     * This function saves the String <b>j</b> into a file called "./test.csv".
     * <br>
     * It takes a formatted String containing data separated by underscores and
     * changes the underscores into commas. <br>
     * It appends this changed String to the end of the file. <br>
     * <br>
     * If the file does not exist, it creates it and adds the right header to it
     * (separation char ',' and the name of each columns : "Time" and
     * "Location"). <br>
     * If it exists and the data is the first one of the test (ie : if the time
     * is equal to 0), it skips a line to separate the tests. <br>
     * <br>
     * <i>If you want to specify a file path, please use the <b>saveJson(String
     * j, String filepath)</b> function.</i>
     *
     * @param j - the string to save
     */
    public void saveData(String j) {
        saveData(j, "test.csv");
    }

    /**
     * This function saves the String <b>j</b> into a file located by the
     * <b>filepath</b> parameter. <br>
     * It takes a formatted String containing data separated by underscores and
     * changes the underscores into commas. <br>
     * It appends this changed String to the end of the file. <br>
     * <br>
     * If the file does not exist, it creates it and adds the right header to it
     * (separation char ',' and the name of each columns : "Time" and
     * "Location"). <br>
     * If it exists and the data is the first one of the test (ie : if the time
     * is equal to 0), it skips a line to separate the tests. <br>
     *
     * @param j - the string to save
     * @param filepath - the file path of the file to save in
     */
    public void saveData(String j, String filepath) {

        try {
            StringBuilder sb = new StringBuilder();
            File f = new File(filepath);

            // if the file doesn't exist we need to create it and add the header (separation char and name of the columns)
            if (!f.exists()) {
                f.createNewFile();
                sb.append("sep=,");
                sb.append('\n');
                sb.append("Time,Location");
                sb.append('\n');
            }

            // we leave a space at the beginning of each test, to separate them
            if (j.startsWith("0")) {
                sb.append("\n");
            }

            // add the data to the string to put in the file
            j = j.replace('_', ',');
            sb.append(j);
            sb.append('\n');

            FileWriter fw = new FileWriter(f, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);

            pw.write(sb.toString());
            pw.close();

        } catch (FileNotFoundException e) {

            e.printStackTrace();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    public void execute(Consumer<Object> callback, String function, Object... args) {
        callback.accept(window.call(function, args));
    }
}
