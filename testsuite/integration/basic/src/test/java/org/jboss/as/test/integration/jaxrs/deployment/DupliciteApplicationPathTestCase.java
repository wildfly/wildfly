package org.jboss.as.test.integration.jaxrs.deployment;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RunWith(Arquillian.class)
@RunAsClient
public class DupliciteApplicationPathTestCase {

    static int initWarningsCount;

    @Deployment
    public static Archive<?> deploy_true() {
        initWarningsCount = getWarningCount("WFLYUT0101");
        WebArchive war = ShrinkWrap.create(WebArchive.class, DupliciteApplicationPathTestCase.class.getSimpleName() + ".war");
        war.addClass(DupliciteApplicationOne.class);
        war.addClass(DupliciteApplicationTwo.class);
        return war;
    }

    @Test
    public void testDuplicationTwoAppTwoResourceSameMethodPath() throws Exception {
        int resultWarningsCount = getWarningCount("WFLYUT0101");
        Assert.assertEquals("Expected warning 'WFLYUT0101' not found.",
                1, resultWarningsCount - initWarningsCount);
    }

    /**
     * Get count of lines with specific string in log
     */
    private static int getWarningCount(String findedString) {
        int count = 0;
        List<String> lines = readServerLogLines();
        for (String line : lines) {
            if (line.contains(findedString)) {
                count++;
            }
        }
        return count;
    }

    private static List<String> readServerLogLines() {
        String jbossHome = System.getProperty("jboss.home");
        String logPath = String.format("%s%sstandalone%slog%sserver.log", jbossHome,
                (jbossHome.endsWith(File.separator) || jbossHome.endsWith("/")) ? "" : File.separator,
                File.separator, File.separator);
        logPath = logPath.replace('/', File.separatorChar);
        try {
            return Files.readAllLines(Paths.get(logPath)); // UTF8 is used by default
        } catch (MalformedInputException e1) {
            // some windows machines could accept only StandardCharsets.ISO_8859_1 encoding
            try {
                return Files.readAllLines(Paths.get(logPath), StandardCharsets.ISO_8859_1);
            } catch (IOException e4) {
                throw new RuntimeException("Server logs has not standard Charsets (UTF8 or ISO_8859_1)");
            }
        } catch (IOException e) {
            // server.log file is not created, it is the same as server.log is empty
        }
        return new ArrayList<>();
    }

}