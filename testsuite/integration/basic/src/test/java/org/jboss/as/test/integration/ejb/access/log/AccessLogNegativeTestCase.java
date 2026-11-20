package org.jboss.as.test.integration.ejb.access.log;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLog;
import org.jboss.as.test.integration.ejb.access.log.util.AccessLogFormat;
import org.jboss.as.test.integration.ejb.access.log.util.ServerLog;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * This test verifies that no ejb access logs are produced when they are not configured
 *
 * @author tborgato <a href="mailto:tborgato@redhat.com">Tommaso Borgato</a>
 */
@RunWith(Arquillian.class)
public class AccessLogNegativeTestCase extends AbstractAccessLogTestCase {
    private static final AccessLogFormat ACCESS_LOG_FORMAT = AccessLogFormat.LONG;

    /* ==============================================
                        PRE-REQUISITE
       ============================================== */

    /**
     * Retrieve the path to server log file and store its location so that it can be used by tests run out of container
     */
    @Test
    @InSequence(Integer.MIN_VALUE)
    @RunAsClient
    public void setTmpFiles() throws IOException {
        Path serverLogPath = getLogFilePath(SERVER_LOG_FILE);
        Assert.assertTrue(String.format("Server log file '%s' does not exist!", serverLogPath.toFile().getAbsolutePath()), serverLogPath.toFile().exists());
        writeTmpFile(
                SERVER_LOG_FILE,
                serverLogPath,
                serverLogPath.toFile().length() // discard output generated so far
        );
    }

    @Test
    @InSequence(Integer.MAX_VALUE)
    @RunAsClient
    public void removeTmpFiles() {
        // remove the tmp file containing the location and offset of the actual log files
        Assert.assertTrue(getTmpFilePath(SERVER_LOG_FILE).toFile().delete());
    }

    @Deployment
    public static Archive createDeployment() {
        return createDeployment(AccessLogNegativeTestCase.class);
    }

    private void checkAccessLogServerLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user) throws IOException, InterruptedException {
        // read the tmp file holding location and offset of the server log file
        Map.Entry<Path, Long> entry = getTmpFileContent(SERVER_LOG_FILE.replace(".log", ""));
        // access the server log file
        ServerLog serverLog = new ServerLog(entry.getKey(), entry.getValue());
        // and read the chunk added since the last read
        String[] lines = serverLog.getNewLines();

        Assert.assertNotNull("No access log messages generated in server log file!", lines);

        // get access logs in the expected number and format
        List<AccessLog> accessLogs = getAccessLogs(lines, ACCESS_LOG_FORMAT, ejbInterface.getSimpleName(), ejbClass.getSimpleName(), ejbMethod, user);

        // mark where the log file was last accessed
        writeTmpFile(SERVER_LOG_FILE.replace(".log", ""), entry.getKey(), serverLog.getOffset());

        Assert.assertTrue("EJB access log found in " + SERVER_LOG_FILE + "!", accessLogs == null || accessLogs.isEmpty());
    }

    @Override
    protected void checkAccessLog(Class ejbInterface, Class ejbClass, String ejbMethod, String user) throws IOException, InterruptedException {
        checkAccessLogServerLog(ejbInterface, ejbClass, ejbMethod, user);
        Assert.assertFalse(getLogFilePath(EJB_ACCESS_LOG_FILE).toFile().exists());
    }

    @Override
    @Ignore("Test is ignored as must run as client to read server log file")
    public void testSFSB() throws Exception {
    }

    @Override
    @Ignore("Test is ignored as must run as client to read server log file")
    public void testSLSB() throws Exception {
    }

    @Override
    @Ignore("Test is ignored as must run as client to read server log file")
    public void testSLSBSecured() throws Exception {
    }

    @Override
    @Ignore("Test is ignored as must run as client to read server log file")
    public void testSFSBSecured() throws Exception {
    }
}
