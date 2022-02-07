package curltool;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertSame;

/**
 * Unit tests for <code>curltool.CurlCmd</code> methods
 */
public class CurlCmdTest {

    // todo need to add more tests for different states of CurlCmd and methods

    private CurlCmdStartProcessAndWaitForResultMock curlCmd;
    private CurlCmd.Builder builder;

    @BeforeMethod
    public void setup() {
        builder = new CurlCmd.Builder();
        curlCmd = new CurlCmdStartProcessAndWaitForResultMock();
        ReflectionTestUtils.setField(builder, "curlCmd", curlCmd);
        builder.setCurlCmd("curlCmd")
                .setUrlToTest("urlToTest");
    }

    /**
     * Checks that logs count equals to default count if count is not set
     */
    @Test
    public void executeDefaultCountTest() {
        CurlCmd curl = builder.execute();
        List<File> resultLogs = (List<File>) ReflectionTestUtils.getField(curl, "logs");
        assertSame(CurlCmd.DEFAULT_COUNT, resultLogs.size());
        assertSame(CurlCmd.DEFAULT_COUNT, this.curlCmd.callCount);
    }

    @DataProvider(name = "LogsCount")
    public static Object[][] countData() {
        return new Object[][]{
                {2},
                {10},
                {100}
        };
    }

    /**
     * Checks that if count is set for a proper value, the result logs count and method calls count will be equal to set count
     */
    @Test(dataProvider = "LogsCount")
    public void executeCountTest(Integer count) {
        CurlCmd curl = builder.setCount(count).execute();
        List<File> resultLogs = (List<File>) ReflectionTestUtils.getField(curl, "logs");
        assertSame(count, resultLogs.size());
        assertSame(count, this.curlCmd.callCount);
    }

    @DataProvider(name = "WrongCount")
    public static Object[][] wrongCountData() {
        return new Object[][]{
                {-1},
                {0},
                {1}
        };
    }

    /**
     * Checks that if count is less then 2, RuntimeException will be thrown
     */
    @Test(expectedExceptions = RuntimeException.class, dataProvider = "WrongCount")
    public void executeWrongCountTest(Integer count) {
        builder.setCount(count).execute();
    }

    class CurlCmdStartProcessAndWaitForResultMock extends CurlCmd {

        private int callCount = 0;

        @Override
        protected boolean startProcessAndWaitForResult(String[] args, File logFile) throws InterruptedException, IOException {
            callCount++;
            return true;
        }
    }

}
