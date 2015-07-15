package org.jboss.as.test.integration.web.handlers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERIODIC_ROTATING_FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
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
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the use of undertow request dumping handler.
 *
 * @author <a href="mailto:jstourac@redhat.com">Jan Stourac</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RequestDumpingHandlerTestCase.RequestDumpingHandlerTestCaseSetupAction.class)
public class RequestDumpingHandlerTestCase {

    public static class RequestDumpingHandlerTestCaseSetupAction implements ServerSetupTask {

        private static String relativeTo;
        private static ModelNode originalValue;

        /** Name of the log file that will be used for testing. */
        private static final String LOG_FILE_PREFIX = "test_server_" + System.currentTimeMillis();
        private static final String LOG_FILE_SUFFIX = ".log";

        // Address to server log setting
        private static final PathAddress aLogAddr = PathAddress.pathAddress().append(SUBSYSTEM, "logging")
                .append(PERIODIC_ROTATING_FILE_HANDLER, "FILE");

        // Address to custom file handler
        private static final String FILE_HANDLER_NAME = "testing-req-dump-handler";
        private static final PathAddress ADDR_FILE_HANDLER = PathAddress.pathAddress().append(SUBSYSTEM, "logging")
                .append(FILE_HANDLER, FILE_HANDLER_NAME);

        // Address to custom logger
        private static final String LOGGER_NAME = "io.undertow.request.dump";
        private static final PathAddress ADDR_LOGGER = PathAddress.pathAddress().append(SUBSYSTEM, "logging")
                .append(LOGGER, LOGGER_NAME);

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            // Retrieve original path to server log files
            ModelNode op = Util.getReadAttributeOperation(aLogAddr, "file");
            originalValue = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
            log.info("Original value: " + originalValue.toString());
            // Retrieve relative path to log files
            relativeTo = originalValue.get("relative-to").asString();

            op = Util.getReadAttributeOperation(PathAddress.pathAddress("path", relativeTo), "path");
            ModelNode logPathModel = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
            logFilePath = Paths.get(logPathModel.asString() + File.separator + LOG_FILE_PREFIX + LOG_FILE_SUFFIX);

            // Set custom file handler to log dumping requests into separate log file
            ModelNode file = new ModelNode();
            file.get("relative-to").set(relativeTo);
            file.get("path").set(LOG_FILE_PREFIX + LOG_FILE_SUFFIX);
            op = Util.createAddOperation(ADDR_FILE_HANDLER);
            op.get(FILE).set(file);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            // Set custom logger that uses previous custom file handler for logging
            op = Util.createAddOperation(ADDR_LOGGER);
            LinkedList<ModelNode> handlers = new LinkedList<ModelNode>();
            handlers.add(new ModelNode(FILE_HANDLER_NAME));
            op.get("handlers").set(handlers);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // Remove custom logger and file-handler
            ModelNode op = Util.createRemoveOperation(ADDR_LOGGER);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            op = Util.createRemoveOperation(ADDR_FILE_HANDLER);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

            // Delete custom server log file
            Files.delete(logFilePath);
        }
    }

    private final int TOTAL_DELAY = 3000;
    private final int SLEEP_TIMEOUT = 200;

    private static Logger log = Logger.getLogger(RequestDumpingHandlerTestCase.class);

    /** Path to custom server log file. */
    private static Path logFilePath;

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
     * Testing app has already defined request dumper handler. This test checks that when a request to URL is performed then
     * request detail data is stored in proper format in the proper log file.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT_DUMP)
    public void testReqDumpHandlerOn(@ArquillianResource URL url) throws Exception {
        commonTestBody(url, true);
    }

    /**
     * Testing app has no request dumper handler registered. This test checks that there is not dumped additional info about
     * executed request in the log file.
     */
    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testReqDumpHandlerOff(@ArquillianResource URL url) throws Exception {
        commonTestBody(url, false);
    }

    /**
     * Common test body part.
     *
     * @param url deployment URL
     * @param requestDumperOn whether RequestDumpingHandler is turned on
     * @throws Exception
     */
    private void commonTestBody(@ArquillianResource URL url, boolean requestDumperOn) throws Exception {
        // Test that there is no request dump for current URL before actual HTTP request OR no custom log file exists at all...
        if (logFilePath.toFile().exists() && logFilePath.toFile().isFile()) {
            testLogForDumpWithURL(logFilePath, url.getFile(), false);
        } else {
            log.info("The log file ('" + logFilePath + "') does not exist yet, that is ok.");
        }

        Header reqHdrs[] = null;
        Header respHdrs[] = null;
        // Perform HTTP request...
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

        // Test whether there is request dump for particular URL after the HTTP request executed...
        testLogForDumpWithURL(logFilePath, url.getFile(), requestDumperOn);

        if (requestDumperOn) {
            // If we expect request dump -> check its data.
            checkReqDumpData(logFilePath, reqHdrs, respHdrs, url.getHost(), url.getPort());
        }
    }

    /**
     * Reads content of the file into a string variable.
     * 
     * @param logFilePath
     * @return content of the file as a string
     * @throws FileNotFoundException
     */
    private String readLogFile(Path logFilePath) throws FileNotFoundException {
        Assert.assertTrue("Log file ('" + logFilePath + "') does not exist", logFilePath.toFile().exists());
        Assert.assertTrue("The '" + logFilePath + "' is not a file", logFilePath.toFile().isFile());

        // logfile exists -> read its content...
        Scanner scanner = new Scanner(logFilePath.toFile());
        StringBuilder sb = new StringBuilder();

        while (scanner.hasNextLine()) {
            sb.append("\n" + scanner.nextLine());
        }
        scanner.close();

        return sb.toString();
    }

    /**
     * Searching log for request dump of request to particular path. If no such request dump is found there is sanity loop to
     * ensure that system has had enough time to write data to the disk.
     * 
     * @param logFilePath path to log file
     * @param path URI path searched for in log file
     * @param expected whether we expect to find given path
     * @throws FileNotFoundException
     */
    private void testLogForDumpWithURL(Path logFilePath, String path, boolean expected) throws Exception {
        Pattern pattern = Pattern.compile("-+REQUEST-+.+" + path + ".+-+RESPONSE-+", Pattern.DOTALL);
        Matcher m;

        long startTime = System.currentTimeMillis();
        boolean hasFound = false;
        long currTime;
        String content;

        // Give system time to write data on disk...
        do {
            currTime = System.currentTimeMillis();
            content = readLogFile(logFilePath);
            m = pattern.matcher(content);

            // Search for pattern...
            if (m.find()) {
                hasFound = true;
                break;
            }
            Thread.sleep(SLEEP_TIMEOUT);
        } while (currTime - startTime < TOTAL_DELAY);

        log.info("I have read following content of the file '" + logFilePath + "':\n" + content + "\n---END-OF-FILE-OUTPUT---");

        // Finally compare search result with our expectation...
        Assert.assertEquals("Searching for pattern: '" + pattern + "' in log file ('" + logFilePath.toString() + "')",
                expected, hasFound);
    }

    /**
     * Check request dumper data.
     *
     * @param logFilePath path to log file
     * @param reqHdrs request headers
     * @param respHdrs response headers
     * @param host server IP address
     * @param port server listening port
     * @throws FileNotFoundException
     */
    private void checkReqDumpData(Path logFilePath, Header[] reqHdrs, Header[] respHdrs, String host, int port)
            throws FileNotFoundException {
        String content = readLogFile(logFilePath);

        // Split into request and response part:
        String request = content.substring(0, content.indexOf("-RESPONSE-"));
        String response = content.substring(content.indexOf("-RESPONSE-"), content.length());

        // Check request dump part...
        searchInFile(request, "-+REQUEST-+");
        searchInFile(request, "\\s+URI=/req-dump/file\\.txt");
        searchInFile(request, "\\s+characterEncoding=");
        searchInFile(request, "\\s+contentLength=");
        searchInFile(request, "\\s+contentType=");

        searchForHeaders(request, reqHdrs);

        searchInFile(request, "\\s+locale=\\[.*\\]");
        searchInFile(request, "\\s+method=GET");
        searchInFile(request, "\\s+protocol=");
        searchInFile(request, "\\s+queryString=");
        searchInFile(request, "\\s+remoteAddr=");
        searchInFile(request, "\\s+remoteHost=");
        searchInFile(request, "\\s+scheme=http");
        searchInFile(request, "\\s+host=" + host);
        searchInFile(request, "\\s+serverPort=" + port);

        // Now check response dump part...
        searchInFile(response, "-+RESPONSE-+");
        searchInFile(response, "\\s+contentLength=");
        searchInFile(response, "\\s+contentType=text/plain");

        searchForHeaders(response, respHdrs);

        searchInFile(response, "\\s+status=200");
    }

    /**
     * Search for request and response headers. Respect that they might be in different order.
     *
     * @param content content in which is searched
     * @param hdrs array of headers which should be searched for
     * @throws FileNotFoundException
     */
    private void searchForHeaders(String content, Header[] hdrs) throws FileNotFoundException {
        if (hdrs != null) {
            for (Header hdr : hdrs) {
                // Close current scanner, reopen it and move to start pattern directly...
                searchInFile(content, "\\s+header=" + hdr.getName() + "=" + hdr.getValue());
            }
        } else {
            // request contains no headers -> we really do not expect it to be in dump
            searchInFile(content, "\\s+header=", false);
        }
    }

    /**
     * Searches in given content for given pattern.
     *
     * @param content content of the file as a string
     * @param regExp regular expression that is searched in the file
     */
    private void searchInFile(String content, String regExp) {
        searchInFile(content, regExp, true);
    }

    /**
     * Searches in given content for given pattern.
     *
     * @param content content of the file as a string
     * @param regExp regular expression that is searched in the file
     * @param expected whether searching pattern is expected to be found or not
     */
    private void searchInFile(String content, String regExp, boolean expected) {
        Pattern pattern = Pattern.compile(regExp);
        Matcher m = pattern.matcher(content);

        Assert.assertEquals("Searching for pattern: '" + regExp + "' in log file ('" + logFilePath.toString() + "')", expected,
                m.find());
    }
}
