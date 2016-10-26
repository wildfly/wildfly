package org.jboss.as.test.integration.web.handlers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.jboss.as.test.integration.web.websocket.AnnotatedClient;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 * Tests the use of Undertow request dumping handler. This is base class that implements particular test behaviour and controls
 * what is tested.
 *
 * @author <a href="mailto:jstourac@redhat.com">Jan Stourac</a>
 */
public abstract class RequestDumpingHandlerTestImpl {

    private static Logger log = Logger.getLogger(RequestDumpingHandlerTestImpl.class);

    private final int TOTAL_DELAY = 3000;
    private final int SLEEP_TIMEOUT = 200;

    private final Path logFilePath;

    // Expected value of the "contentType" header in response - default is "text/plain" but can be overridden by child class
    protected String contentType = "text/plain";
    // Expected value of the "status" header in response - default is "200" but can be overridden by child class
    protected String status = "200";
    // Expected value of the "scheme" header in request - default is "http" but can be overridden by child class
    protected String scheme = "http";

    /**
     * Constructor that immediately executes test body.
     *
     * @param uri             testing URI to connect to
     * @param logFilePath     path to log file in which are logged request dumps
     * @param requestDumperOn whether request dumping feature is enabled at all
     */
    public RequestDumpingHandlerTestImpl(URI uri, Path logFilePath, boolean requestDumperOn) throws Exception {
        this.logFilePath = logFilePath;
        commonTestBody(uri, requestDumperOn);
    }

    /***
     * Abstract method which implements way of performing request to the server. It should be implemented depending on what type
     * of request we want to perform (HTTP, HTTPS, etc.).
     *
     * @param uri testing URI to connect to
     * @return 2dimensional array - request (as a first member) and response (as a second member) headers arrays; in case that
     *         there is no simple way how to obtain request and response headers then returns null
     * @throws Exception
     */
    public abstract Header[][] performRequest(URI uri) throws Exception;

    /**
     * Common test body part.
     *
     * @param uri             deployment URI
     * @param requestDumperOn whether RequestDumpingHandler is turned on
     * @throws Exception
     */
    private void commonTestBody(URI uri, boolean requestDumperOn) throws Exception {
        // Test whether custom log file exists already. If so then count number of lines in it so further we will ignore them.
        long skipBytes = 0;
        if (logFilePath.toFile().exists() && logFilePath.toFile().isFile()) {
            skipBytes = logFilePath.toFile().length();
        } else {
            log.trace("The log file ('" + logFilePath + "') does not exist yet, that is ok.");
        }

        // Perform request on server...
        Header[] reqHdrs = null;
        Header[] respHdrs = null;
        Header[][] hdrs = performRequest(uri);
        if (hdrs != null) {
            reqHdrs = hdrs[0];
            respHdrs = hdrs[1];
        }

        // Test whether there is request dump for particular URL after the HTTP request executed...
        testLogForDumpWithURL(logFilePath, uri.getPath(), skipBytes, requestDumperOn);

        if (requestDumperOn) {
            // If we expect request dump -> check its data.
            checkReqDumpData(logFilePath, skipBytes, reqHdrs, respHdrs, uri.getHost(), uri.getPort(), uri.getPath());
        }
    }

    /**
     * Reads content of the file into a string variable.
     *
     * @param logFilePath
     * @param skipBytes   number of bytes from the beginning of the file that should be skipped
     * @return content of the file as a string
     * @throws FileNotFoundException
     */
    private String readLogFile(Path logFilePath, long skipBytes) throws FileNotFoundException, IOException {
        Assert.assertTrue("Log file ('" + logFilePath + "') does not exist", logFilePath.toFile().exists());
        Assert.assertTrue("The '" + logFilePath + "' is not a file", logFilePath.toFile().isFile());

        // logfile exists -> read its content...
        LineNumberReader lnr = new LineNumberReader(new FileReader(logFilePath.toFile()));
        StringBuilder sb = new StringBuilder();

        log.trace("I am skipping '" + skipBytes + "' bytes from the beggining of the file.");
        lnr.skip(skipBytes);

        String input;
        while ((input = lnr.readLine()) != null) {
            sb.append("\n" + input);
        }
        lnr.close();

        return sb.toString();
    }

    /**
     * Searching log for request dump of request to particular path. If no such request dump is found there is sanity loop to
     * ensure that system has had enough time to write data to the disk.
     *
     * @param logFilePath path to log file
     * @param path        URI path searched for in log file
     * @param skipBytes   number of bytes from the beginning of the file that should be skipped
     * @param expected    whether we expect to find given path
     * @throws FileNotFoundException
     */
    private void testLogForDumpWithURL(Path logFilePath, String path, long skipBytes, boolean expected) throws Exception {
        Pattern pattern = Pattern.compile("-+REQUEST-+.+" + path + ".+-+RESPONSE-+", Pattern.DOTALL);
        Matcher m;

        long startTime = System.currentTimeMillis();
        boolean hasFound = false;
        long currTime;
        String content;

        // Give system time to write data on disk...
        do {
            currTime = System.currentTimeMillis();
            content = readLogFile(logFilePath, skipBytes);
            m = pattern.matcher(content);

            // Search for pattern...
            if (m.find()) {
                hasFound = true;
                break;
            }
            Thread.sleep(SLEEP_TIMEOUT);
        } while (currTime - startTime < TOTAL_DELAY);

        log.trace("I have read following content of the file '" + logFilePath + "':\n" + content + "\n---END-OF-FILE-OUTPUT---");

        // Finally compare search result with our expectation...
        Assert.assertEquals("Searching for pattern: '" + pattern + "' in log file ('" + logFilePath.toString() + "')",
                expected, hasFound);
    }

    /**
     * Check request dumper data.
     *
     * @param logFilePath path to log file
     * @param skipBytes   number of bytes from the beginning of the file that should be skipped
     * @param reqHdrs     request headers
     * @param respHdrs    response headers
     * @param host        server IP address
     * @param port        server listening port
     * @param path        URI path
     * @throws IOException
     */
    private void checkReqDumpData(Path logFilePath, long skipBytes, Header[] reqHdrs, Header[] respHdrs, String host, int port, String path) throws IOException {
        String content = readLogFile(logFilePath, skipBytes);

        // Split into request and response part:
        String request = content.substring(0, content.indexOf("-RESPONSE-"));
        String response = content.substring(content.indexOf("-RESPONSE-"), content.length());

        // Check request dump part...
        searchInFile(request, "-+REQUEST-+");
        searchInFile(request, "\\s+URI=" + Pattern.quote(path));
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
        searchInFile(request, "\\s+scheme=" + Pattern.quote(scheme));
        searchInFile(request, "\\s+host=" + Pattern.quote(host));
        searchInFile(request, "\\s+serverPort=" + Pattern.quote(String.valueOf(port)));

        // Now check response dump part...
        searchInFile(response, "-+RESPONSE-+");
        searchInFile(response, "\\s+contentLength=");
        searchInFile(response, "\\s+contentType=" + Pattern.quote(contentType));

        searchForHeaders(response, respHdrs);

        searchInFile(response, "\\s+status=" + Pattern.quote(status));
    }

    /**
     * Search for request and response headers. Respect that they might be in different order.
     *
     * @param content content in which is searched
     * @param hdrs    array of headers which should be searched for; if null then no check will be performed; if zero length then
     *                no header line is expected in the log
     * @throws FileNotFoundException
     */
    private void searchForHeaders(String content, Header[] hdrs) throws FileNotFoundException {
        if (hdrs == null) {
            log.trace("No array with headers given -> skipping testing header content in log file.");
            return;
        }

        final String HEADER_REGEXP = "\\s+header=";

        if (hdrs.length > 0) {
            // Check that number of "header" occurrences equals to hdrs.length
            Assert.assertEquals("", hdrs.length, countMatch(content, HEADER_REGEXP));

            for (Header hdr : hdrs) {
                // Close current scanner, reopen it and move to start pattern directly...
                searchInFile(content, HEADER_REGEXP + Pattern.quote(hdr.getName()) + "=" + Pattern.quote(hdr.getValue()));
            }
        } else {
            // request contains no headers -> we really do not expect it to be in dump
            searchInFile(content, HEADER_REGEXP, false);
        }
    }

    /**
     * Searches in given content for given pattern.
     *
     * @param content content of the file as a string
     * @param regExp  regular expression that is searched in the file
     */
    private void searchInFile(String content, String regExp) {
        searchInFile(content, regExp, true);
    }

    /**
     * Searches in given content for given pattern.
     *
     * @param content  content of the file as a string
     * @param regExp   regular expression that is searched in the file
     * @param expected whether searching pattern is expected to be found or not
     */
    private void searchInFile(String content, String regExp, boolean expected) {
        Pattern pattern = Pattern.compile(regExp);
        Matcher m = pattern.matcher(content);

        Assert.assertEquals("Searching for pattern: '" + regExp + "' in log file ('" + logFilePath.toString() + "')", expected,
                m.find());
    }

    /**
     * Counts number of occurrences of given string in given content.
     *
     * @param content in this content will be searching for given pattern
     * @param regExp  given pattern to search in content
     * @return number of occurrences in given content
     */
    private int countMatch(String content, String regExp) {
        Pattern pattern = Pattern.compile(regExp);
        Matcher m = pattern.matcher(content);
        int occurs = 0;

        while (m.find()) {
            occurs++;
        }

        return occurs;
    }

    /**
     * Testing class which implements HTTPS requests on server.
     *
     * @author <a href="mailto:jstourac@redhat.com">Jan Stourac</a>
     */
    static class HttpsRequestDumpingHandlerTestImpl extends RequestDumpingHandlerTestImpl {

        HttpsRequestDumpingHandlerTestImpl(URI uri, Path logFilePath, boolean requestDumperOn) throws Exception {
            super(uri, logFilePath, requestDumperOn);
        }

        @Override
        public Header[][] performRequest(URI uri) throws Exception {
            // Override value of the "scheme" header expected in response
            scheme = "https";

            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
            }};

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HttpsURLConnection httpsConn = (HttpsURLConnection) uri.toURL().openConnection();

            httpsConn.setDoOutput(false);

            httpsConn.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            BufferedReader br = new BufferedReader(new InputStreamReader(httpsConn.getInputStream()));

            StringBuilder sb = new StringBuilder();
            String input;

            while ((input = br.readLine()) != null) {
                sb = sb.append(input);
            }
            br.close();
            httpsConn.disconnect();

            Header[][] reqAndrespHeaders = new Header[2][];
            reqAndrespHeaders[1] = retrieveHeaders(httpsConn.getHeaderFields());

            log.trace("The content of the URL ('" + uri + "'):\n" + sb.toString());

            Assert.assertEquals(200, httpsConn.getResponseCode());
            Assert.assertEquals("Could not reach expected content via http request", "A file", sb.toString());

            // NOTE: leaving request headers null (won't be checked) as there is no easy way how to obtain them
            return reqAndrespHeaders;
        }

        private Header[] retrieveHeaders(Map<String, List<String>> headers) {
            //System.out.println("---- PRINTING HEADERS ----");
            LinkedList<Header> hdrsList = new LinkedList<Header>();
            for (String header : headers.keySet()) {
                for (String value : headers.get(header)) {
                    if (header != null) {
                        //System.out.println(header + ": " + value);
                        hdrsList.add(new BasicHeader(header, value));
                    }
                }
            }
            return hdrsList.toArray(new Header[hdrsList.size()]);
        }
    }

    /**
     * Testing class that implements standard HTTP requests.
     *
     * @author <a href="mailto:jstourac@redhat.com">Jan Stourac</a>
     */
    static class HttpRequestDumpingHandlerTestImpl extends RequestDumpingHandlerTestImpl {

        HttpRequestDumpingHandlerTestImpl(URI uri, Path logFilePath, boolean requestDumperOn) throws Exception {
            super(uri, logFilePath, requestDumperOn);
        }

        @Override
        public Header[][] performRequest(URI uri) throws Exception {
            Header[][] ret = new Header[2][];

            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                HttpGet httpget = new HttpGet(uri.toURL().toExternalForm() + "file.txt");

                HttpContext localContext = new BasicHttpContext();
                HttpResponse response = httpClient.execute(httpget, localContext);
                HttpRequest request = (HttpRequest) localContext.getAttribute(HttpCoreContext.HTTP_REQUEST);

                // Fill return values
                ret[0] = request.getAllHeaders();
                ret[1] = response.getAllHeaders();

                StatusLine statusLine = response.getStatusLine();
                Assert.assertEquals(200, statusLine.getStatusCode());

                String result = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Could not reach expected content via http request", "A file", result);
            }
            return ret;
        }
    }

    /**
     * Testing class that implements standard websocket requests via http upgrade.
     *
     * @author <a href="mailto:jstourac@redhat.com">Jan Stourac</a>
     */
    static class WsRequestDumpingHandlerTestImpl extends RequestDumpingHandlerTestImpl {

        WsRequestDumpingHandlerTestImpl(URI uri, Path logFilePath, boolean requestDumperOn) throws Exception {
            super(uri, logFilePath, requestDumperOn);
        }

        @Override
        public Header[][] performRequest(URI uri) throws Exception {
            // Override value of the "contentType" header expected in response
            contentType = "null";
            // Override value of the "status" header expected in response
            status = "101";

            AnnotatedClient endpoint = new AnnotatedClient();
            WebSocketContainer serverContainer = ContainerProvider.getWebSocketContainer();
            serverContainer.connectToServer(endpoint, uri);
            Assert.assertEquals("Hello Stuart", endpoint.getMessage());

            // NOTE: leaving request and response headers null (won't be checked) as there is no easy way how to obtain them
            return null;
        }
    }
}