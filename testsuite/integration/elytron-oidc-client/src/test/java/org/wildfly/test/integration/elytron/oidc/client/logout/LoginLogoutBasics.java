/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.logout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE_PASSWORD;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSubmitInput;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.junit.After;

/**
 * Tests for the OpenID Connect logout types.
 */
public class LoginLogoutBasics extends EnvSetupUtils {

    private CloseableHttpClient httpClient;

    private final String KEYCLOAK_USERNAME = "username";
    private final String KEYCLOAK_PASSWORD = "password";

    public LoginLogoutBasics() {}

    private URL generateURL(String appName, String servletPath) {
        try {
            return new URL("http", TestSuiteEnvironment.getHttpAddress(),
                    TestSuiteEnvironment.getHttpPort(),
                    "/" + appName + servletPath);
        } catch (MalformedURLException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @After
    public void closeHttpClient() throws Exception {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }

    public void browserLoginToApp(WebClient webClient, String appName) throws Exception {
        browserLoginToApp(webClient, ALICE, ALICE_PASSWORD, SecuredFrontChannelServlet.SERVLET_PATH,
                generateURL(appName, SecuredFrontChannelServlet.SERVLET_PATH));

    }
    public void browserLoginToApp(WebClient webClient, String username, String password,
                           String expectedText, URL requestUrl) throws Exception {
        HtmlPage page = webClient.getPage(requestUrl);
        HtmlForm form = (HtmlForm)page.getElementById("kc-form-login");
        HtmlInput userName = form.getInputByName("username");
        userName.setValue(username);
        HtmlInput passwd = form.getInputByName("password");
        passwd.setValue(password);
        HtmlSubmitInput login = form.getInputByName("login");
        HtmlPage rtnPage = login.click();
        String rtnText = rtnPage.asXml();
        assertTrue("Expected result [ " + expectedText + " ] but was ["
                        + rtnText + "]", rtnText.contains(expectedText));
    }

    public void browserLogoutOfKeycloak(WebClient webClient, String appName) throws Exception {
        URL requestUrl = generateURL(appName, Constants.LOGOUT_PATH_VALUE);
        browserLogoutOfKeycloak(webClient, requestUrl);
    }

    public void browserLogoutOfKeycloak(WebClient webClient, URL requestUrL) throws Exception {
        webClient.getPage(requestUrL);
        Thread.sleep(3500); // give time for logout to complete
    }

    public void browserAssertUserLoggedIn(WebClient webClient, String appName, String expectedText) throws Exception {
        browserAccessPage(webClient, generateURL(appName, SecuredFrontChannelServlet.SERVLET_PATH), expectedText);
    }

    public void browserAssertUserLoggedOut(WebClient webClient, String appName, String expectedText) throws Exception {
        browserAccessPage(webClient, generateURL(appName, SecuredFrontChannelServlet.SERVLET_PATH), expectedText);
    }

    public void browserAccessPage(WebClient webClient, URL requestUrl, String expectedText) throws Exception {
        HtmlPage assertPage = webClient.getPage(requestUrl);
        String apStr = assertPage.asXml();
        assertTrue("Expected result [ " + expectedText + " ] but was ["
                + apStr + "]", apStr.contains(expectedText));
    }

    public void loginToApp(String appName) throws Exception {
        loginToApp(appName, ALICE, ALICE_PASSWORD, HttpURLConnection.HTTP_OK,
                SimpleServlet.RESPONSE_BODY);
    }

    public void loginToApp(String appName,
                                  String username, String password, int expectedStatusCode, String expectedText) throws Exception {
        loginToApp(username, password, expectedStatusCode, expectedText, true,
                generateURL(appName, SimpleSecuredServlet.SERVLET_PATH).toURI());
    }

    public void loginToApp(String username, String password,
                                  int expectedStatusCode, String expectedText,
                                  boolean loginToKeycloak, URI requestUri) throws Exception {

        HttpGet getMethod = new HttpGet(requestUri);
        HttpContext context = new BasicHttpContext();
        CloseableHttpResponse response = null;
        Form keycloakLoginForm = null;

        int retryMax = 10;
        int retry = 0;
        boolean retryAgain = true;
        // allow for slow system response with limited retries
        do {
            Thread.sleep(500);
            HttpClientUtils.closeQuietly(response);
            response = httpClient.execute(getMethod, context);
            if (response.getStatusLine().getStatusCode() == expectedStatusCode) {
                try {
                    keycloakLoginForm = new Form(response);
                    retryAgain = false;
                } catch (IOException ee) {
                    HttpClientUtils.closeQuietly(response);
                    // continue retries
                }
            }
            retry++;
        } while(retryAgain &&  retry < retryMax);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (loginToKeycloak) {
                assertEquals("Expected code == OK but got " + statusCode
                        + " for request=" + requestUri, HttpURLConnection.HTTP_OK, statusCode);
                assertNotNull("GET of " + requestUri + " did not produce a usable response", keycloakLoginForm);
                try (CloseableHttpResponse afterLoginClickResponse = simulateClickingOnButton(httpClient,
                        keycloakLoginForm, username, password, "Sign In")) {

                    afterLoginClickResponse.getEntity().getContent();
                    assertEquals(expectedStatusCode, afterLoginClickResponse.getStatusLine().getStatusCode());

                    if (expectedText != null) {
                        String responseString = new BasicResponseHandler().handleResponse(afterLoginClickResponse);
                        assertTrue("Unexpected result " + responseString, responseString.contains(expectedText));
                    }
                }
            }
            else {
                assertEquals("Expected code == FORBIDDEN but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_FORBIDDEN, statusCode);
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public void logoutOfKeycloak(String appName, String expectedText) throws Exception {
        URI requestUri = generateURL(appName, Constants.LOGOUT_PATH_VALUE).toURI();
        logoutOfKeycloak(requestUri, HttpURLConnection.HTTP_OK, expectedText, true);
    }

    public void logoutOfKeycloak(URI requestUri, int expectedStatusCode, String expectedText,
                                        boolean logoutFromKeycloak) throws Exception {

        HttpContext context = new BasicHttpContext();
        HttpResponse response = null;
        HttpGet getMethod = new HttpGet(requestUri);

        int retryMax = 10;
        int retry = 0;
        // allow for slow system response with limited retries
        do {
            Thread.sleep(500);
            HttpClientUtils.closeQuietly(response);  // if we are looping close the previous unwanted response
            response = httpClient.execute(getMethod, context);
            retry++;
        } while((response.getStatusLine().getStatusCode() != expectedStatusCode)
                &&  retry < retryMax);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (logoutFromKeycloak) {
                assertEquals("Expected code == OK but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_OK, statusCode);
                response.getEntity();
                String responseString = new BasicResponseHandler().handleResponse(response);
                assertTrue("Unexpected result " + expectedText + " but result was [ "
                        + responseString +" ]", responseString.contains(expectedText));
            }
            else {
                assertEquals("Expected code == FORBIDDEN but got " + statusCode + " for request=" + requestUri, HttpURLConnection.HTTP_FORBIDDEN, statusCode);
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
            Thread.sleep(2000);  // give a slow CI time to fully process the logout
        }
    }

    public void assertUserLoggedIn(String appName, String expectedText) throws Exception {
        accessPage(generateURL(appName, SimpleSecuredServlet.SERVLET_PATH).toURI(), HttpURLConnection.HTTP_OK, expectedText);
    }

    public void assertUserLoggedOut(String appName, String expectedText) throws Exception {
        accessPage(generateURL(appName, SimpleSecuredServlet.SERVLET_PATH).toURI(), HttpURLConnection.HTTP_OK, expectedText);
    }

    public void accessPage(URI requestUri, int expectedStatusCode,
                                     String expectedText) throws Exception {
        HttpContext context = new BasicHttpContext();
        HttpResponse response = null;
        HttpGet getMethod = new HttpGet(requestUri);

        String responseString;
        int retryMax = 10;
        int retry = 0;
        // allow for slow system response with limited retries
        do {
            Thread.sleep(500);
            HttpClientUtils.closeQuietly(response); // if we are looping close the previous unwanted response
            response = httpClient.execute(getMethod, context);
            response.getEntity();
            responseString = new BasicResponseHandler().handleResponse(response);
            retry++;
        } while((!responseString.contains(expectedText)) &&  retry < retryMax);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Expected code == " + expectedStatusCode + " but got "
                    + statusCode + " for request=" + requestUri, statusCode, expectedStatusCode);
            assertTrue("Expected result [ " + expectedText + "] but was ["
                            + responseString + "]",
                    responseString.contains(expectedText));
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    // TODO prune this unused code if its non-use doesn't indicate something dropped
    /**
     * Check that the proper warning message is logged.
     */
    public boolean isWarningReported(String findString) {
        List<String> lines = readServerLogLines();
        for (String line : lines) {
            if (line.contains(findString)) {
                return true;
            }
        }
        return false;
    }

    // TODO prune this unused code if its non-use doesn't indicate something dropped
    public List<String> readServerLogLines() {
        String jbossHome = System.getProperty("jboss.install.dir");
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

    private CloseableHttpResponse simulateClickingOnButton(CloseableHttpClient client, Form form, String username, String password, String buttonValue) throws IOException {
        final URL url = new URL(form.getAction());
        final HttpPost request = new HttpPost(url.toString());
        final List<NameValuePair> params = new LinkedList<>();
        for (Input input : form.getInputFields()) {
            if (input.type == Input.Type.HIDDEN ||
                    (input.type == Input.Type.SUBMIT && input.getValue().equals(buttonValue))) {
                params.add(new BasicNameValuePair(input.getName(), input.getValue()));
            } else if (input.getName().equals(KEYCLOAK_USERNAME)) {
                params.add(new BasicNameValuePair(input.getName(), username));
            } else if (input.getName().equals(KEYCLOAK_PASSWORD)) {
                params.add(new BasicNameValuePair(input.getName(), password));
            }
        }
        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        return client.execute(request);
    }

    private static final class Form {

        static final String
                NAME = "name",
                VALUE = "value",
                INPUT = "input",
                TYPE = "type",
                ACTION = "action",
                FORM = "form";

        final HttpResponse response;
        final String action;
        final List<Input> inputFields = new LinkedList<>();

        public Form(HttpResponse response) throws IOException {
            this.response = response;
            final String responseString = new BasicResponseHandler().handleResponse(response);
            if (!responseString.startsWith("<!DOCTYPE html>")) {
                throw new IOException("Form is not the login doc");
            }
            final Document doc = Jsoup.parse(responseString);
            final Element form = doc.select(FORM).first();
            this.action = form.attr(ACTION);
            for (Element input : form.select(INPUT)) {
                Input.Type type = null;
                switch (input.attr(TYPE)) {
                    case "submit":
                        type = Input.Type.SUBMIT;
                        break;
                    case "hidden":
                        type = Input.Type.HIDDEN;
                        break;
                }
                inputFields.add(new Input(input.attr(NAME), input.attr(VALUE), type));
            }
        }

        public String getAction() {
            return action;
        }

        public List<Input> getInputFields() {
            return inputFields;
        }
    }

    private static final class Input {

        final String name, value;
        final Input.Type type;

        public Input(String name, String value, Input.Type type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public enum Type {
            HIDDEN, SUBMIT
        }
    }

    /* Data structure containing the URL path text to be registered with keycloak
       for logout support.
    */
    public static class LogoutChannelPaths {
        public String backChannelPath;
        public String frontChannelPath;
        public List<String> postLogoutRedirectPaths;

        public LogoutChannelPaths(final String backChannelPath,
                                 final String frontChannelPath,
                                 final List<String> postLogoutRedirectPaths) {
            this.backChannelPath = backChannelPath;
            this.frontChannelPath = frontChannelPath;
            this.postLogoutRedirectPaths = postLogoutRedirectPaths;
        }
    }

    /* This method retained for future debugging.  It can be helpful to
        review Keycloak's log file.

        To enable logging one must add stmt, withEnv("KC_LOG_LEVEL", "DEBUG"); ,
        in class testsuite/integration/elytron-oidc-client/src/test/java/
        org/wildfly/test/integration/elytron/oidc/client/KeycloakContainer
        method configure() there are like withEnv stmts there.

        Add a call to this method after the login, logout action of interest.
     */
    public void dumpKeycloakLog() {
        dumpKeycloakLog("x-keycloak-logout.log");
    }
    public void dumpKeycloakLog(String filename) {

        String console = KEYCLOAK_CONTAINER.getLogs();
        String fileName = "/tmp/"+filename;
        java.io.PrintWriter outLog = null;
        try {
            java.io.File file = new java.io.File(fileName);
            file.delete();
            outLog = new java.io.PrintWriter(fileName);
            outLog.println(console);
        } catch(Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (outLog != null) {
                outLog.close();
            }
        }
    }
}
