package quizplatform2;

/**
 *
 * @author koul1o
 */
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.StandardCopyOption.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.control.ProgressBar;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;

public class Bridge {

    private static final String QUESTION_NAME = "question";
    private static final String[] FORBIDDEN_WORDS = {QUESTION_NAME, "info", "final_quiz", "manual", "documents"};
    private int cnt = 1, cnt2 = 1, time = 0;
    private JSObject window;
    private String title;
    private WebEngine engine;
    final LongProperty startTime = new SimpleLongProperty();
    final LongProperty endTime = new SimpleLongProperty();
    final LongProperty elapsedTime = new SimpleLongProperty();
    private String traceT = "";
    private String qUrl = "";
    private String nextUrl = null;
    private boolean firstStat = true;
    private String experimentId = "";
    private String fullFilepath = "";
    private static String[][] files;
    private static String[][] allFiles;
    private static final float MILIS = 60000;
    private float augmentBar;
    private Timer timer2;
    private Timer t;
    private Timer timerAutoQuiz;
    private Timer demogTimer;
    private String setup;
    private float tTime, fTime;
    private double percent = 0.0;
    private String previousUrl = "";
    private String changedHtml = "";
    private String section = "";
    private String srcPath = "";
    private String binPath = "";
    private Timer timerAutoDemog;

    public Bridge(WebEngine engine, Stage stage, QuizPlatform2 quizPlatform, float tTime, float fTime, float step, String root, String experimentId, ProgressBar progressBar) {

        String DOCUMENT_PATH = "./src" + File.separator + "quizplatform2" + File.separator + root;
        this.setup = root;
        this.setup = this.setup.replace("html/", "");
        this.srcPath = DOCUMENT_PATH;
        this.binPath = this.srcPath.replace("src", "bin");
        try {
            findFiles(new File(DOCUMENT_PATH));

            this.resetFiles();

        } catch (IOException e) {
            e.printStackTrace();
        }
        engine.getLoadWorker().stateProperty().addListener((ObservableValue<? extends State> obs, State oldState, State newState) -> {

            if (newState == State.SUCCEEDED) {

                this.engine = engine;
                window = (JSObject) engine.executeScript("window");
                /* Register our java app to the window so that we can make upcalls to the app using java.functionName(); */
                window.setMember("java", this);

                title = engine.getTitle();

                stage.setTitle(engine.getTitle());
                getDocuments();
                /* */
                if (engine != null) {
                    {

                        if (!title.toLowerCase().contains("final") && !title.toLowerCase().contains("demographic")) {
                            engine.executeScript("setDocuments();");
                        }
                        if (cnt <= 1) {
                            startTime.set(System.nanoTime());
                            getTime();
                            traceT = time + "_" + title;
                            getTrace(traceT);
                            if (title.toLowerCase().contains("instructions")) {
                                qUrl = engine.getLocation();
                                qUrl = qUrl.replace("file://", "");
                                qUrl = qUrl.replace("Instructions.html", "question1.html");
                                engine.executeScript("var qUrl=\'" + qUrl + "\'");
                            }

                            /* Using org.reactfx.util.FxTimer augment the progress bar periodicaly every 15min by 25% */
                            augmentBar = ((tTime / step));

                            Timer timer = FxTimer.runPeriodically(
                                    Duration.ofMillis((long) (augmentBar * MILIS)),
                                    () -> {
                                        percent += 1 / step;
                                        progressBar.setProgress(percent);
                                    });

                            // call either by the timerAutoQuiz or when the final question of the training quiz is correctly answered, starts the demogTimer
                            this.t = FxTimer.create(
                                    Duration.ofMillis(1),
                                    () -> {
                                        percent = 0;
                                        augmentBar = ((fTime / step));
                                        timer.stop();
                                        timer2 = FxTimer.runPeriodically(
                                                Duration.ofMillis((long) (augmentBar * MILIS)),
                                                () -> {
                                                    percent += 1 / step;
                                                    progressBar.setProgress(percent);
                                                });

                                        progressBar.setProgress(percent);
                                        engine.load(getClass().getResource(binPath.substring(1) + "/final_quiz.html").toExternalForm());
                                        this.timerAutoDemog.restart();
                                    });

                            // this timer is used to launch the t timer. Avoid to have all arguments as private instance variable
                            this.timerAutoQuiz = FxTimer.runLater(
                                    Duration.ofMillis((long) ((tTime * MILIS) + 3000)), // adds 3 seconds to the time so that the progress bar is full during 3 seconds
                                    () -> {
                                        //sends to the final quiz
                                        this.t.restart();
                                    });

                            this.timerAutoDemog = FxTimer.create(
                                    Duration.ofMillis((long) ((tTime * MILIS) + 3000)), // adds 3 seconds to the time so that the progress bar is full during 3 seconds
                                    () -> {
                                        //sends to the final quiz
                                        this.demogTimer.restart();
                                    });

                            this.demogTimer = FxTimer.create(
                                    Duration.ofMillis((long) (1)),
                                    () -> {
                                        //System.out.println("Entering demog");
                                        if (title.toLowerCase().contains("final")) {
                                            engine.executeScript("checkFinalAnswers();");
                                            timer2.stop();
                                            percent = 0;
                                            progressBar.setProgress(percent);
                                            engine.load(getClass().getResource(binPath.substring(1) + "/info.html").toExternalForm());
                                        }
                                    });
                            cnt++;

                        } else if (!(title.toLowerCase().contains("final") || title.toLowerCase().contains("demographic"))) {
                            getTime();
                            traceT = time + "_" + title;

                            if (!title.toLowerCase().contains(QUESTION_NAME)) {
                                this.section = "_Panel 1";
                                traceT = time + "_" + title + this.section;
                            }
                            getTrace(traceT);
                        } else {
                            getTime();

                            traceT = time + "_" + title;
                            getTrace(traceT);
                        }

                        if (title.toLowerCase().contains(QUESTION_NAME)) {
                            qUrl = engine.getLocation();
                            qUrl = qUrl.replace("file://", "");
                            engine.executeScript("var qUrl=\'" + qUrl + "\'");

                        } else {
                            engine.executeScript("var qUrl=\'" + qUrl + "\'");

                        }

                    }
                }
            }
        });
    }

    /* Function, to exit the platform */
    public void exit() {
        getLastTrace(traceT);
        Platform.exit();

    }

    /**
     * Upcall to this function from the page, to get the interaction trace
     */
    public void getTrace(String trace) {
        System.out.println("Trace: " + trace);
        saveData(trace);
        if (!this.previousUrl.equals(this.engine.getLocation()) && !this.changedHtml.equals("")) {
            this.savePage();
        }
    }

    public void getLastTrace(String trace) {
        getTime();
        traceT = time + "_" + title + "_Exit";
        getTrace(traceT);
    }

    public void elementTrace(String element) {
        getTime();
        this.section = element;
        traceT = time + "_" + title + "_" + element;
        getTrace(traceT);
    }

    public void getTime() {
        endTime.set(System.nanoTime());
        elapsedTime.bind(Bindings.subtract(endTime, startTime));
        time = (int) (0 + elapsedTime.divide(1_000_000).getValue());
    }

    /**
     * This function sends an double dimension array to the javascript
     * containing the name of the documents and their urls.</br>
     * The urls are in the first column, the names are in the second. </br>
     * The array is stored in the <b>docs</b> variable in the javascript.
     */
    public void getDocuments() {
        String s = "var docs = [";
        for (int i = 0; i < Bridge.files[0].length; i++) {
            s = s + "[\'" + Bridge.files[0][i] + "\',\'" + Bridge.files[1][i] + "\']";
            if (i != Bridge.files[0].length - 1) {
                s = s + ",";
            }
        }
        s = s + "];";
        engine.executeScript(s);
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
        cnt2++;
        String finalUrl = "";
        try {
            quizUrl = URLDecoder.decode(quizUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        File f = new File(quizUrl);
        System.out.println(f.getAbsolutePath());
        if (f.exists() && !f.isDirectory()) {
            String s[] = quizUrl.split("/");
            String fs[] = quizUrl.split("/");

            quizUrl = "";
            s[s.length - 1] = "question" + cnt2 + ".html";
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
            }

            System.out.println(quizUrl);

            File fn = new File(quizUrl);
            if (fn.exists() && !fn.isDirectory()) {
                engine.executeScript("var nextUrl=\'" + quizUrl + "\'");
            } else {
                cnt2 = 1;
                this.quizFinished();
                engine.executeScript("var nextUrl=\'" + finalUrl + "\'");
            }
        }
    }

    /**
     * Adds one to the last digit of the argument
     *
     * @param sti the String to increment
     * @return the modified String
     */
    public String incrementString(String sti) {
        Pattern digitPattern = Pattern.compile("(\\d+)");

        Matcher matcher = digitPattern.matcher(sti);
        StringBuffer result = new StringBuffer();
        int index = 0;
        while (matcher.find()) {
            index = matcher.start();
        }
        matcher.find(index);
        matcher.appendReplacement(result, String.valueOf(Integer.parseInt(matcher.group(1)) + 1));
        matcher.appendTail(result);

        return result.toString();
    }

    /* This function redirects us to the next question while in the quiz */
    public void redirect(String url) {
        engine.executeScript("window.location.replace(\'" + url + "\');");
    }

    /**
     * This function goes through all files contained in the <b>directory</b>
     * path. If the file is a document, then it adds it to the returning
     * array.<br/>
     * The entries of the array have two values, the first one is the canonical
     * path to the file and the second is the name of the file without its
     * extension.<br/>
     *
     * @param directory The path to the directory to explore
     * @return An array of two dimensions. The first dimension contains the
     * canonical path (0) and the filename (1), the second dimension is the
     * entries
     * @throws IOException If an I/O error occurs, which is possible because the
     * construction of the canonical pathname may require filesystem queries.
     */
    public static void findFiles(File directory) throws IOException {
        File[] file;
        HashMap<String, String> al = new HashMap<String, String>();
        HashMap<String, String> allFileList = new HashMap<String, String>();
        if (directory.isDirectory()) {
            file = directory.listFiles(); // Calls same method again.
            for (File f : file) {
                if (f.isDirectory()) {
                    // findFiles(f);
                } else {
                    String key = f.getName();
                    String value = f.getName().split("\\.")[0]; // we remove extension from the file name.

                    if (!al.containsKey(key) && Bridge.notIn(value, Bridge.FORBIDDEN_WORDS)) {
                        al.put(key, value);
                    }
                    if (!allFileList.containsKey(key)) {
                        allFileList.put(key, value);
                    }
                }
            }
            Bridge.files = new String[2][al.size()];
            SortedSet<String> sortedKeys = new TreeSet<String>(al.keySet());
            int i = 0;
            for (String key : sortedKeys) {
                Bridge.files[0][i] = key;
                Bridge.files[1][i] = al.get(key);
                i++;
            }

            Bridge.allFiles = new String[2][allFileList.size()];
            SortedSet<String> sortedKeysAllFiles = new TreeSet<String>(allFileList.keySet());
            int j = 0;
            for (String key : sortedKeysAllFiles) {
                Bridge.allFiles[0][j] = key;
                Bridge.allFiles[1][j] = allFileList.get(key);
                j++;
            }

        } else {
            System.out.println("The argument should be a directory ! Got : " + directory.getAbsolutePath());
        }
    }

    /**
     * This function checks if the <b>forbiddenWords</b> are contained within
     * the <b>stringToBeChecked</b>.
     *
     * @param stringToBeChecked The string to check
     * @param forbidenWords The array of forbidden words
     * @return true if the string is clear, false if it contains at least one
     * forbidden word.
     */
    public static boolean notIn(String stringToBeChecked, String[] forbiddenWords) {
        boolean clear = true;
        if (!stringToBeChecked.equals("")) {
            for (String s : forbiddenWords) {
                if (clear) {
                    clear = !stringToBeChecked.contains(s);
                }
            }
        } else {
            clear = false;
        }

        return clear;
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
        this.saveData(j, experimentId + "_1.csv");
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
     * If it exists and the data is the first one of the test, it skips a line
     * to separate the tests. <br>
     *
     * @param j - the string to save
     * @param filepath - the file path of the file to save in
     */
    public void saveData(String j, String filepath) {

        try {
            StringBuilder sb = new StringBuilder();

            File f;
            // we leave a space at the beginning of each test, to separate them
            if (this.firstStat) {
                int cpt = 1;
                new SimpleDateFormat("yyyyMMdd_HHmmss");
                Date date = new Date();

                f = new File(this.setup);

                if (!f.exists()) {
                    f.mkdir();
                }

                f = new File(this.setup + File.separator + filepath);
                while (f.exists()) {
                    filepath = incrementString(filepath);
                    f = new File(this.setup + File.separator + filepath);
                    cpt++;
                }

                this.fullFilepath = this.setup + File.separator + filepath;
                f.createNewFile();
                sb.append("sep=^");
                sb.append('\n');
                sb.append(date);
                sb.append('\n');
                sb.append("Experiment Id : " + experimentId);
                sb.append('\n');
                sb.append("Participant Id : " + cpt);
                sb.append('\n');
                sb.append("Setup: " + setup);
                sb.append('\n');
                sb.append("Training Time : " + tTime + " Final Quiz Time : " + fTime);
                sb.append('\n');

                sb.append("\n");
                this.firstStat = false;
            } else {

                f = new File(this.fullFilepath);
            }

            // add the data to the string to put in the file
            j = j.replace('_', '^');
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

    public void print(String l) {
        System.out.println("quizplatform.Bridge.print()" + l);
    }

    public void execute(Consumer<Object> callback, String function, Object... args) {
        callback.accept(window.call(function, args));
    }

    public void savePage() {

        StringBuilder sb = new StringBuilder();

        File f = new File(this.previousUrl.replaceAll("src", "bin"));
        if (f.exists()) {
            this.changedHtml = this.changedHtml.replaceAll("(<div id=\"documents\">)[^&]*(</div>)", "$1 <h2>Documents</h2> $2");
            sb.append(this.changedHtml);
            FileWriter fw;
            try {
                fw = new FileWriter(f, false);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter pw = new PrintWriter(bw);
                pw.write(sb.toString());
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void checkHighlight() {
        String highlightedText = this.engine.executeScript("checkHighlight()").toString();
        this.previousUrl = this.engine.getLocation().replace("file:///", "");
        this.changedHtml = (String) this.engine.executeScript("document.documentElement.outerHTML");
        this.getTrace(time + "_" + title + this.section + "_highlighted_" + highlightedText);
    }

    public void loadUserData(String filepath) {
        StringBuilder sb = new StringBuilder();
        try {
            String line = "";
            File file = new File(filepath);
            BufferedReader br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copies all the html files from the src directory to the bin directory,
     * thus resetting the highlighting
     */
    public void resetFiles() {
        Path source = Paths.get(this.srcPath);
        Path target = Paths.get(this.binPath);
        try {
            Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {

                private CopyOption options = REPLACE_EXISTING;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {

                    Path targetdir = target.resolve(source.relativize(dir));
                    try {
                        if (!targetdir.toFile().exists() || !targetdir.toFile().isDirectory()) {
                            Files.copy(dir, targetdir, options);
                        }
                    } catch (FileAlreadyExistsException e) {
                        if (!Files.isDirectory(targetdir)) {
                            throw e;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.copy(file, target.resolve(source.relativize(file)), options);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDocumentsFolderPath() {
        String filePath = getClass().getResource("html/" + this.setup + "/documents.html").toExternalForm().replaceAll("file:/", "");
        String[] tmp = filePath.split("/");
        String folder = "";
        tmp[tmp.length - 1] = "";
        for (int i = 0; i < tmp.length; i++) {
            folder += tmp[i] + "/";
        }
        return folder;
    }

    public void quizFinished() {
        this.timerAutoQuiz.stop();
        this.t.restart();
    }

    public void finalQuizFinished() {
        this.timerAutoDemog.stop();
        this.demogTimer.restart();
    }
}
