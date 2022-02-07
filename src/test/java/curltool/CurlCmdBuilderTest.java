package curltool;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

/**
 * Unit tests for <code>curltool.CurlCmd</code> methods
 */
public class CurlCmdBuilderTest {

    private CurlCmdMock curlCmd;
    private CurlCmd.Builder builder;

    @BeforeMethod
    public void setup() {
        builder = new CurlCmd.Builder();
        curlCmd = new CurlCmdMock();
        ReflectionTestUtils.setField(builder, "curlCmd", curlCmd);
    }

    /**
     * Checks that if required args is not set, RuntimeException will be thrown by execute method
     */
    @Test(expectedExceptions = RuntimeException.class)
    public void executeWithoutSettingUp() {
        builder.execute();
    }

    /**
     * Checks that if cmd is not set and url is set, RuntimeException will be thrown by execute method
     */
    @Test(expectedExceptions = RuntimeException.class)
    public void executeWithoutCmd() {
        builder.setUrlToTest("http://localhost")
                .execute();
    }

    /**
     * Checks that if url is not set and cmd is set, RuntimeException will be thrown by execute method
     */
    @Test(expectedExceptions = RuntimeException.class)
    public void executeWithoutUrl() {
        builder.setCurlCmd("/curl")
                .execute();
    }

    @DataProvider(name = "Positive")
    public static Object[][] positiveData() {
        return new Object[][]{
                {"/path_to_curl", "http://localhost"},
                {"/path_to_curl", ""},
                {"", "http://localhost"},
                {"/path_to_curl", "localhost"},
                {"/path_to_curl", "http://google.com"}
        };
    }

    /**
     * Checks that if only required properties are set execute() behaves as expected and returns a correct object
     */
    @Test(dataProvider = "Positive")
    public void executeReturnsCmd(String cmd, String urlToTest) {
        CurlCmd curl = builder.setCurlCmd(cmd)
                .setUrlToTest(urlToTest)
                .execute();
        assertSame(this.curlCmd, curl);
        assertEquals(this.curlCmd.countExecuteCalls, 1);
    }

    /**
     * This class mocks execute method of CurlCmd
     */
    class CurlCmdMock extends CurlCmd {

        private int countExecuteCalls = 0;

        @Override
        protected void execute() throws Exception {
            countExecuteCalls++;
        }
    }

}
