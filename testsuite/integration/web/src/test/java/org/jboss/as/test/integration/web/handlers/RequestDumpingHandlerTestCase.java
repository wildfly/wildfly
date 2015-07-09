package org.jboss.as.test.integration.web.handlers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERIODIC_ROTATING_FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the use of undertow-handlers.conf
 *
 * @author <a href="mailto:jstourac@redhat.com">Jan Stourac</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RequestDumpingHandlerTestCase {

    private static Logger log = Logger.getLogger(RequestDumpingHandlerTestCase.class);

    /** Name of the log file that will be used for testing. */
    private static String logFilePrefix;
    private static final String LOG_FILE_SUFFIX = ".log";

    @ArquillianResource
    private static ManagementClient mgmtClient;

    /** Path to custom server log file. */
    private static Path logFilePath;

    private static ModelNode originalValue;
    private static String relativeTo;

    // Address to server log setting
    private static final PathAddress aLogAddr = PathAddress.pathAddress().append(SUBSYSTEM, "logging")
            .append(PERIODIC_ROTATING_FILE_HANDLER, "FILE");

    @Before
    public void setup() throws Exception {
        logFilePrefix = "test_server_" + System.currentTimeMillis();

        // Retrieve path to server log files
        ModelNode op = Util.getReadAttributeOperation(aLogAddr, "file");
        originalValue = ManagementOperations.executeOperation(mgmtClient.getControllerClient(), op);
        log.info("Original value: " + originalValue.toString());
        relativeTo = originalValue.get("relative-to").asString();

        op = Util.getReadAttributeOperation(PathAddress.pathAddress("path", relativeTo), "path");
        ModelNode logPathModel = ManagementOperations.executeOperation(mgmtClient.getControllerClient(), op);
        logFilePath = Paths.get(logPathModel.asString() + File.separator + logFilePrefix + LOG_FILE_SUFFIX);
    }

    @After
    public void tearDown() throws Exception {
        // Remove custom server log setting
        ManagementOperations.executeOperation(mgmtClient.getControllerClient(),
                Util.getWriteAttributeOperation(aLogAddr, "file", originalValue));

        // Delete custom server log file
        Files.delete(logFilePath);
    }

    private final static String DEPLOYMENT = "no-req-dump";
    private final static String DEPLOYMENT_DUMP = "req-dump";

    @Deployment(name = DEPLOYMENT_DUMP)
    public static WebArchive deployWithReqDump() {
        WebArchive war = ShrinkWrap
                .create(WebArchive.class, DEPLOYMENT_DUMP + ".war")
                .addPackage(RequestDumpingHandlerTestCase.class.getPackage())
                .addAsWebInfResource(RequestDumpingHandlerTestCase.class.getPackage(), "jboss-web-req-dump.xml",
                        "jboss-web.xml").addAsWebResource(new StringAsset("A file"), "file.txt");
        return war;
    }

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deployWithoutReqDump() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war")
                .addPackage(RequestDumpingHandlerTestCase.class.getPackage())
                .addAsWebInfResource(RequestDumpingHandlerTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                .addAsWebResource(new StringAsset("A file"), "file.txt");
        return war;
    }

    /**
     * Testing app has already defined request dumper handler. This test checks that when an request to URL is performed then
     * request detail data is stored in proper format in the proper log file. Also check that warning for the user is generated.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_DUMP)
    public void testReqDumpHandlerOn(@ArquillianResource URL url) throws Exception {
        commonTestBody(url, true);
    }

    /**
     * Testing app has no request dumper handler registered. This test checks that there is not dump additional info about
     * executed request in the logfile. Also no warning is shown to user.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testReqDumpHandlerOff(@ArquillianResource URL url) throws Exception {
        commonTestBody(url, false);
    }

    /**
     * Common test body part.
     *
     * @param url deployment url
     * @param requestDumperOn whether RequestDumpingHandler is turned on
     * @throws Exception
     */
    private void commonTestBody(@ArquillianResource URL url, boolean requestDumperOn) throws Exception {
        // Set custom system log name
        ModelNode file = new ModelNode();
        file.get("relative-to").set(relativeTo);
        file.get("path").set(logFilePrefix + LOG_FILE_SUFFIX);
        ModelNode op = Util.getWriteAttributeOperation(aLogAddr, "file", file);
        ManagementOperations.executeOperation(mgmtClient.getControllerClient(), op);

        // Test there is no request dump before actual http request OR no log file at all...
        if (logFilePath.toFile().exists() && logFilePath.toFile().isFile()) {
            checkIsReqDumData(logFilePath, null, null, url.getHost(), url.getPort(), false);
        } else {
            log.info("The log file ('" + logFilePath + "') does not exist yet");
        }

        Header reqHdrs[] = null;
        Header respHdrs[] = null;
        // Perform http request...
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        try {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "file.txt");
            reqHdrs = httpget.getAllHeaders();

            HttpResponse response = httpclient.execute(httpget);
            respHdrs = response.getAllHeaders();

            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            Assert.assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            Assert.assertEquals("Could not reach expected content via http request", "A file", result);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.close();
        }

        // Test whether there is request dump after the http request executed...
        checkIsReqDumData(logFilePath, reqHdrs, respHdrs, url.getHost(), url.getPort(), requestDumperOn);
    }

    /**
     * Check whether request dumper data is available.
     *
     * @param logFilePath path to log file
     * @param reqHdrs request headers
     * @param respHdrs response headers
     * @param host server IP address
     * @param port server listening port
     * @param expected whether we expect searched data to be found or not
     * @throws FileNotFoundException
     */
    private void checkIsReqDumData(Path logFilePath, Header[] reqHdrs, Header[] respHdrs, String host, int port,
            boolean expected) throws FileNotFoundException {
        Assert.assertTrue("Log file ('" + logFilePath + "') does not exist", logFilePath.toFile().exists());
        Assert.assertTrue("The '" + logFilePath + "' is not a file", logFilePath.toFile().isFile());

        // logfile exists -> read its content...
        Scanner scanner = new Scanner(logFilePath.toFile());

        // Check request dump part...
        searchInFile(scanner, "-+REQUEST-+", logFilePath, expected);
        if (expected) {
            searchInFile(scanner, "\\s+URI=/req-dump/file\\.txt", logFilePath, expected);
        } else {
            searchInFile(scanner, "\\s+URI=/no-req-dump/file\\.txt", logFilePath, expected);
        }
        searchInFile(scanner, "\\s+characterEncoding=", logFilePath, expected);
        searchInFile(scanner, "\\s+contentLength=", logFilePath, expected);
        searchInFile(scanner, "\\s+contentType=", logFilePath, expected);

        searchForHeaders(reqHdrs, "-+REQUEST-+", expected);

        searchInFile(scanner, "\\s+locale=\\[.*\\]", logFilePath, expected);
        searchInFile(scanner, "\\s+method=GET", logFilePath, expected);
        searchInFile(scanner, "\\s+protocol=", logFilePath, expected);
        searchInFile(scanner, "\\s+queryString=", logFilePath, expected);
        searchInFile(scanner, "\\s+remoteAddr=", logFilePath, expected);
        searchInFile(scanner, "\\s+remoteHost=", logFilePath, expected);
        searchInFile(scanner, "\\s+scheme=http", logFilePath, expected);
        searchInFile(scanner, "\\s+host=" + host, logFilePath, expected);
        searchInFile(scanner, "\\s+serverPort=" + port, logFilePath, expected);

        // Now check response dump part...
        searchInFile(scanner, "-+RESPONSE-+", logFilePath, expected);
        searchInFile(scanner, "\\s+contentLength=", logFilePath, expected);
        searchInFile(scanner, "\\s+contentType=text/plain", logFilePath, expected);

        searchForHeaders(respHdrs, "-+RESPONSE-+", expected);

        searchInFile(scanner, "\\s+status=200", logFilePath, expected);

        scanner.close();
    }

    /**
     * Search for request and response headers. Respect that they might be in different order.
     *
     * @param hdrs array of headers which should be searched for
     * @param startPattern starting pattern in log file
     * @param expected whether header is expected to be in log file or not
     * @throws FileNotFoundException
     */
    private void searchForHeaders(Header[] hdrs, String startPattern, boolean expected) throws FileNotFoundException {
        // Create new scanner and start from the REQUEST/RESPONSE part directly.
        Scanner scanner = new Scanner(logFilePath.toFile());

        if (hdrs != null) {
            for (Header hdr : hdrs) {
                // Close current scanner, reopen it and move to start pattern directly...
                scanner.close();
                scanner = new Scanner(logFilePath.toFile());
                searchInFile(scanner, startPattern, logFilePath, expected);
                searchInFile(scanner, "\\s+header=" + hdr.getName() + "=" + hdr.getValue(), logFilePath, expected);
            }
        } else {
            // request contains no headers -> we really do not expect it to be in dump
            searchInFile(scanner, startPattern, logFilePath, expected);
            searchInFile(scanner, "\\s+header=", logFilePath, false);
        }

        scanner.close();
    }

    /**
     * Searches in file using {@link Scanner} instance for given pattern.
     *
     * @param scanner instance of the file scanner
     * @param pattern pattern that is searched in the file
     * @param logFilePath path to the file (just for logging purpose)
     * @param expected whether given pattern is expected to be in file or not
     */
    private void searchInFile(Scanner scanner, String pattern, Path logFilePath, boolean expected) {
        String result = null;

        // try to find particular line in the file...
        while (scanner.hasNextLine()) {
            result = scanner.findInLine(Pattern.compile(pattern));
            if (result == null && scanner.hasNextLine()) {
                scanner.nextLine();
            } else {
                break;
            }
        }

        if (expected) {
            Assert.assertNotNull("The '" + pattern + "' WAS NOT found in log file ('" + logFilePath.toString() + "')", result);
        } else {
            Assert.assertNull("The '" + pattern + "' was found in log file ('" + logFilePath.toString() + "')", result);
        }
    }
}
