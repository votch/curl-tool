package curltool;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;

import static curltool.CurlEntryPoint.Argument.*;
import static java.lang.String.format;
import static java.lang.System.*;

/**
 * This is an entry point for curl-tool. It contains main method that process args, instantiates <code>curltool.Curl</code>,
 * executes http request n times and counts statistics.
 *
 * @author Elena Barzilovich
 */
public class CurlEntryPoint {

    // curl tool args
    enum Argument {
        ARG_HELP("--help"),
        ARG_BODY("-b"),
        ARG_CURL("-c"),
        ARG_DEL("-d"),
        ARG_FORM_ARG("-f"),
        ARG_HEADER("-h"),
        ARG_LOG("-l"),
        ARG_HTTP_METHOD("-m"),
        ARG_COUNT("-n"),
        ARG_TIMEOUT("-t"),
        ARG_SILENT("-s"),
        ARG_URL("-u"),
        ARG_VERBOSE("-v")
        ;

        private final String str;

        Argument(String str) {
            this.str = str;
        }

        private static Argument findByStr(String str) {
            for (Argument arg : Argument.values()) {
                if (arg.str.equals(str)) {
                    return arg;
                }
            }
            return null;
        }
    }

    // Info that will be printed for user
    private static final String INFO = new StringJoiner(lineSeparator())
            .add("Required arguments:")
            .add(ARG_CURL.str + " - curl command")
            .add(ARG_URL.str + " - url to test")
            .add("Optional arguments:")
            .add(ARG_COUNT.str + " - integer count of calls more then or equals to 2 (default - " + CurlCmd.DEFAULT_COUNT + ")")
            .add(ARG_LOG.str + " - intermediate log file name (created for every curl call, marked with index)")
            .add(ARG_DEL.str + " - boolean delete intermediate log files on exit (default - " + CurlCmd.DEFAULT_DEL_LOGS + ")")
            .add(ARG_HTTP_METHOD.str + " - request method - GET, POST, PUT, etc (default - GET). " +
                    "It is up to user to make sure the provided value is a correct method")
            .add(ARG_TIMEOUT.str + " - timeout to wait for remote host response in milliseconds (default - " + CurlCmd.DEFAULT_TIMEOUT + ")")
            .add(ARG_SILENT.str + " - boolean silent mode (default - " + CurlCmd.DEFAULT_SILENT + "). If true, hides curl progress info")
            .add(ARG_HEADER.str + " - header for the request (default - empty), could be repeated several times")
            .add(ARG_FORM_ARG.str + " - form argument to be send in the requests body. cUrl equivalent: '-F, --form'")
            .add(ARG_BODY.str + " - raw body data, ex json. cUrl equivalent: '-d, --data'")
            .add(ARG_VERBOSE.str + " - verbose - print all debug info to curl log. cUrl equivalent: '-v, --verbose'")
            .toString();

    // form help message with an additional message as a first line
    private static Function<String, String> errorMessage = m -> new StringJoiner(lineSeparator())
            .add(m)
            .add(INFO)
            .toString();

    public static void main(String[] args) {
        CurlCmd.Builder curl = processArguments(args).printSettings();
        out.println(curl.execute().countStatistics());
    }

    private static CurlCmd.Builder processArguments(String[] args) {
        CurlCmd.Builder curlBuilder = new CurlCmd.Builder();

        for (int i = 0; i < args.length; i++) {
            if (i == 0 && Objects.equals(ARG_HELP, args[i])) {
                printHelpAndExit();
            }
            i = processArg(curlBuilder, args, i);
        }

        if (curlBuilder.isNotReady()) {
            err.println(errorMessage.apply("One or several required arguments were not set."));
            exit(1);
        }
        return curlBuilder;
    }

    // process i-argument
    private static int processArg(CurlCmd.Builder curlBuilder, String[] args, int i) {
        String argStr = args[i++];
        Argument arg = Argument.findByStr(argStr);
        exitIfNull(argStr);
        switch (arg) {
            case ARG_LOG:
                curlBuilder.setLogFileName(getArg(args, i));
                break;
            case ARG_CURL:
                curlBuilder.setCurlCmd(getArg(args, i));
                break;
            case ARG_URL:
                curlBuilder.setUrlToTest(getArg(args, i));
                break;
            case ARG_COUNT:
                setTriesCount(curlBuilder, getArg(args, i));
                break;
            case ARG_DEL:
                curlBuilder.setDeleteLogs(Boolean.parseBoolean(getArg(args, i)));
                break;
            case ARG_HTTP_METHOD:
                curlBuilder.setMethod(getArg(args, i));
                break;
            case ARG_TIMEOUT:
                curlBuilder.setTimeout(Long.parseLong(getArg(args, i)));
                break;
            case ARG_SILENT:
                curlBuilder.setSilent(Boolean.parseBoolean(getArg(args, i)));
                break;
            case ARG_HEADER:
                curlBuilder.addHeader(getArg(args, i));
                break;
            case ARG_FORM_ARG:
                curlBuilder.addFormArg(getArg(args, i));
                break;
            case ARG_BODY:
                curlBuilder.addBodyArg(getArg(args, i));
                break;
            case ARG_VERBOSE:
                curlBuilder.setVerbose(Boolean.parseBoolean(getArg(args, i)));
                break;
        }
        return i;
    }

    private static void exitIfNull(String arg) {
        if (arg == null) {
            err.println(errorMessage.apply(format(
                    "Unexpected argument: %s - run with '%s' argument for the information about program arguments.",
                    arg, ARG_HELP)));
            exit(2);
        }
    }

    private static void setTriesCount(CurlCmd.Builder curlBuilder, String countStr) {
        try {
            curlBuilder.setCount(Integer.parseInt(countStr));
        } catch (NumberFormatException e) {
            err.println(errorMessage.apply(format(
                    "Count argument should have an integer value >= 2. But it has a value: %s",
                    countStr)));
            exit(2);
        }
    }

    private static void printHelpAndExit() {
        out.println(errorMessage.apply(format(
                "Usage: java -jar curl_tools.jar %s $CURL %s $URL [%s $CALLS_COUNT] " +
                        "[%s $SILENT] [%s $DELETE_LOGS] [OTHER ARGUMENTS]",
                ARG_CURL, ARG_URL, ARG_COUNT, ARG_SILENT, ARG_DEL)));
        exit(0);
    }

    private static String getArg(String[] args, int i) {
        if (i < args.length) {
            return args[i];
        } else {
            throw new RuntimeException(errorMessage.apply(format("'%s' argument should have a value.", args[i - 1])));
        }
    }
}
