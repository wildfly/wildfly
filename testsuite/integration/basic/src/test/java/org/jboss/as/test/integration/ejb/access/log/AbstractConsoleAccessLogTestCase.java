package org.jboss.as.test.integration.ejb.access.log;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Base class for console-destination access log tests.
 *
 * <p>The {@code console} destination writes JSON records to the server process stdout.
 * WildFly pipes the server's stdout into {@code standalone/log/server.log}, so the
 * correct way to verify records is to read that file — the same approach used by
 * {@link ServerLogAccessLogJsonTestCase}.
 *
 * <p>In-container tests (no {@code @RunAsClient}) are ignored: they run inside the server
 * JVM where there is no client-side log file access.
 */
public abstract class AbstractConsoleAccessLogTestCase extends AbstractAccessLogTestCase {

    /** Capture the server.log offset before any test runs so we only see new lines. */
    @Test
    @InSequence(-1)
    @RunAsClient
    public void setTmpFileForServerLogFile() throws IOException {
        Path serverLogPath = getLogFilePath(SERVER_LOG_FILE);
        Assert.assertTrue(
                String.format("Server log file '%s' does not exist!", serverLogPath.toFile().getAbsolutePath()),
                serverLogPath.toFile().exists());
        writeTmpFile(SERVER_LOG_FILE, serverLogPath, serverLogPath.toFile().length());
    }

    @Test
    @InSequence(Integer.MAX_VALUE)
    @RunAsClient
    public void removeTmpFile() {
        Assert.assertTrue(getTmpFilePath(SERVER_LOG_FILE).toFile().delete());
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
