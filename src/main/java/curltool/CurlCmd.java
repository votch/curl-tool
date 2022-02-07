package curltool;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.System.*;

/**
 * Holds settings for curl requests, executes curl and counts statistics.
 * Required settings are cmd and urlToTest. These settings should be defined.
 * Instantiate this class via <code>CurlCmd.Builder::execute</code> method.
 * After that call <code>countStatistics</code> method to calculate and print statistics.
 *
 * @author Elena Barzilovich
 */
public class CurlCmd {

    private static final String TIME_PRETRANSFER = "time_pretransfer";
    private static final String TIME_STARTTRANSFER = "time_starttransfer";
    private static final String TIME_TOTAL = "time_total";

    // this is a log template that we will write for every curl request an then process to culc a statistics
    private static final String CURL_OUT_TEMPLATE = "\n" +
            "exitcode=%{exitcode}\n" +
            "json=%{json}\n" +
            "stdout=%{stdout}\n" +
            "time_pretransfer=%{" + TIME_PRETRANSFER + "}\n" +
            "time_starttransfer=%{" + TIME_STARTTRANSFER + "}\n" +
            "time_total=%{" + TIME_TOTAL + "}\n";

    static final int DEFAULT_COUNT = 10;
    static final int DEFAULT_TIMEOUT = 10 * 1000;
    static final boolean DEFAULT_SILENT = true;
    static final boolean DEFAULT_DEL_LOGS = true;

    private static final DecimalFormat AVERAGE = new DecimalFormat("##.##");

    // These 2 fields should be filled
    private String cmd = null;
    private String urlToTest = null;

    // in the most cases the default values will be used
    private String logFileName = "curl";
    private int count = DEFAULT_COUNT;
    private boolean deleteLogs = DEFAULT_DEL_LOGS;
    private String method = null;
    private long timeout = DEFAULT_TIMEOUT;
    private boolean silent = DEFAULT_SILENT;
    private final Set<String> headers = new HashSet<>();
    private final Set<String> formArgs = new HashSet<>();
    private final Set<String> bodyArgs = new HashSet<>();

    private final List<File> logs = new LinkedList<>();
    private boolean verbose = false;

    protected CurlCmd() {
    }

    private String[] curlArgs() {
        Stream.Builder<String> argsBuilder = Stream.<String>builder()
                .add(cmd)
                .add("-w").add(CURL_OUT_TEMPLATE)
                .add("-k");
        if (silent) argsBuilder.accept("-s");
        if (verbose) argsBuilder.add("-v");
        if (method != null) argsBuilder.add("-X").accept(method);
        headers.forEach(h -> argsBuilder.add("-H").accept(h));
        formArgs.forEach(a -> argsBuilder.add("-F").accept("'" + a + "'"));
        bodyArgs.forEach(a -> argsBuilder.add("-d").accept(a));
        argsBuilder.accept(urlToTest);

        return argsBuilder.build().toArray(String[]::new);
    }

    /**
     * Executes http request n times and saves log files with the response and the result statistic.
     */
    protected void execute() throws Exception {
        logs.clear();

        String log = logFileName + ".%d.log";
        Path tempDir = Files.createTempDirectory(Paths.get(""), "temp");
        // if included files won't be deleted, this directory won't be deleted either
        tempDir.toFile().deleteOnExit();

        String[] args = curlArgs();
        for (int i = 0; i < count; i++) {
            File logFile = tempDir.resolve(format(log, i)).toFile();
            if (deleteLogs) {
                logFile.deleteOnExit();
            }

            boolean normal = startProcessAndWaitForResult(args, logFile);
            if (normal) {
                logs.add(logFile);
            } else {
                err.println(i + " attempt has been timeouted.");
            }
        }
    }

    protected boolean startProcessAndWaitForResult(String[] args, File logFile) throws InterruptedException, IOException {
        return new ProcessBuilder(args)
                        .redirectErrorStream(true)
                        .redirectOutput(logFile)
                        .start()
                        .waitFor(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * To the proper work before calling this method, <code>execute</code> method should be called.
     */
    public String countStatistics() {
        StringJoiner sj = new StringJoiner(lineSeparator());

        // sum of all total times except the first one
        AtomicLong totalSum = new AtomicLong();
        // sum all calculation times except the first one
        AtomicLong calcSum = new AtomicLong();

        AtomicBoolean first = new AtomicBoolean(true);
        logs.stream()
                .map(CurlCmd::logToProps)
                .map(props -> new Times(sToMs(props, TIME_TOTAL),
                        sToMs(props, TIME_STARTTRANSFER) - sToMs(props, TIME_PRETRANSFER)))
                .forEach(times -> {
                    if (first.get()) {
                        sj.add(statisticToStr("First", times.total, times.calc));
                        first.set(false);
                    } else {
                        totalSum.set(totalSum.get() + times.total);
                        calcSum.set(calcSum.get() + times.calc);
                    }
                });

        sj.add(statisticToStr("Then",
                AVERAGE.format(totalSum.get() / (count - 1)),
                AVERAGE.format(calcSum.get() / (count - 1))));

        return sj.toString();
    }

    // Read the log file to a Properties
    private static Properties logToProps(File log) {
        try {
            Properties props = new Properties();
            try (Reader logReader = new FileReader(log)) {
                props.load(logReader);
            }
            return props;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Convert statistic values to string
    private static <T> String statisticToStr(String statisticTitle, T totalTime, T calcTime) {
        StringJoiner sj = new StringJoiner(lineSeparator());
        return sj.add(statisticTitle + ":")
                .add("- total time - " + totalTime)
                .add("- calculation time - " + calcTime).toString();
    }

    // Get property in seconds and convert it to long value in milliseconds
    private static long sToMs(Properties props, String param) {
        String val = props.getProperty(param).replaceAll(",", ".");
        return (long) (Float.parseFloat(val) * 1000);
    }

    // print curl-tool settings that is needed to process the request
    private void printSettings() {
        StringJoiner sj = new StringJoiner(lineSeparator())
                .add("Configuration")
                .add("Curl: " + cmd)
                .add("URL to test: " + urlToTest)
                .add("Count of calls: " + count)
                .add("Remote request timeout: " + timeout);
        if (!silent) {
            sj.add("Silent mode: " + silent)
                    .add("Log file name: " + logFileName)
                    .add("Delete logs on exit: " + deleteLogs)
                    .add("");
        }
        if (method != null) {
            sj.add("Method: " + method);
        }
        if (!headers.isEmpty()) {
            sj.add("Headers: " + headers);
        }

        out.println(sj.add(""));
    }

    /**
     * This class is for convenience. It is intended to easily work with statistics measurements.
     */
    private static class Times {
        private final long total;
        private final long calc;

        Times(long total, long calc) {
            this.total = total;
            this.calc = calc;
        }
    }

    /**
     * Builds CurlCmd and executes it via calling <code>execute</code> method.
     * Required settings are cmd and urlToTest. These settings should be explicitly defined.
     */
    public static class Builder {

        private final CurlCmd curlCmd = new CurlCmd();

        /**
         * Calls <code>CurlCmd::execute</code> method if CurlCmd "is ready". Otherwise throws <code>RuntimeException</code>.
         * Returns <code>CurlCmd</code> instance that could be used then to manipulate the requests statistic.
         */
        public CurlCmd execute() {
            if (isNotReady()) {
                throw new RuntimeException("Curl path or url is not set.");
            }

            try {
                curlCmd.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return curlCmd;
        }

        // print CurlCmd instance settings that is needed to process the request
        public Builder printSettings() {
            curlCmd.printSettings();
            return this;
        }

        /**
         * Returns true if required properties were set
         */
        public boolean isNotReady() {
            return curlCmd.cmd == null || curlCmd.urlToTest == null;
        }

        public void setLogFileName(String logFileName) {
            curlCmd.logFileName = logFileName;
        }

        public Builder setCurlCmd(String curlCmd) {
            this.curlCmd.cmd = curlCmd;
            return this;
        }

        public Builder setUrlToTest(String urlToTest) {
            curlCmd.urlToTest = urlToTest.trim().replace(" ", "%20");
            return this;
        }

        public Builder setCount(int count) {
            if (count < 2) {
                throw new NumberFormatException();
            }
            curlCmd.count = count;
            return this;
        }

        public void setDeleteLogs(boolean deleteLogs) {
            curlCmd.deleteLogs = deleteLogs;
        }

        public void setMethod(String method) {
            curlCmd.method = method;
        }

        public void setTimeout(long timeout) {
            curlCmd.timeout = timeout;
        }

        public void setSilent(boolean silent) {
            curlCmd.silent = silent;
        }

        public void addHeader(String header) {
            curlCmd.headers.add(header);
        }

        public void addFormArg(String arg) {
            curlCmd.formArgs.add(arg);
        }

        public void addBodyArg(String arg) {
            curlCmd.bodyArgs.add(arg);
        }

        public void setVerbose(boolean verbose) {
            curlCmd.verbose = verbose;
        }
    }
}
