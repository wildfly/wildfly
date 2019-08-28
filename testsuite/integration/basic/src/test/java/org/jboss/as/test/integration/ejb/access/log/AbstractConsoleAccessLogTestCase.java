package org.jboss.as.test.integration.ejb.access.log;

import org.jboss.as.test.integration.ejb.access.log.util.ServerStdout;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Introduces an intermediate level to inspect console log and switches off tests where console log is not available
 */
public abstract class AbstractConsoleAccessLogTestCase extends AbstractAccessLogTestCase {
    protected ServerStdout serverStdout;
    private PrintStream currentStdout;

    @Before
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void setup() {
        // Capture the current stdout to introduce a level of indirection that can be inspected by our tests
        currentStdout = System.out;
        serverStdout = new ServerStdout(currentStdout);
        System.setOut(new PrintStream(serverStdout));
        // discard output generated so far
        serverStdout.getNewLinesSync();
    }

    @After
    public void tearDown() throws IOException {
        // Replaced with the captured stdout
        System.setOut(currentStdout);
    }

    @Override
    @Ignore("Test is ignored as must run as client to capture console output")
    public void testSFSB() throws Exception {
    }

    @Override
    @Ignore("Test is ignored as must run as client to capture console output")
    public void testSLSB() throws Exception {
    }

    @Override
    @Ignore("Test is ignored as must run as client to capture console output")
    public void testSLSBSecured() throws Exception {
    }

    @Override
    @Ignore("Test is ignored as must run as client to capture console output")
    public void testSFSBSecured() throws Exception {
    }

}
