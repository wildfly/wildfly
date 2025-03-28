/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.oidc.client.logout;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE;
import static org.wildfly.test.integration.elytron.oidc.client.KeycloakConfiguration.ALICE_PASSWORD;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Tests for the OpenID Connect logout types.
 */
public class LoginLogoutBasics extends EnvSetupUtils {

    private HttpClient httpClient;

    private final String KEYCLOAK_USERNAME = "username";
    private final String KEYCLOAK_PASSWORD = "password";

    private Stability desiredStability = null;

    public LoginLogoutBasics() {}
    public LoginLogoutBasics(Stability desiredStability) {
        this.desiredStability = desiredStability;
    }

    private URL generateURL(String appName, String servletPath) {
        try {
            return new URL("http", TestSuiteEnvironment.getHttpAddress(),
                    TestSuiteEnvironment.getHttpPort(),
                    "/" + appName + servletPath);
        } catch (MalformedURLException e) {
            assertFalse(e.getMessage(), false);
        }
        return null;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void browserLoginToApp(WebClient webClient, String appName) throws Exception {
        browserLoginToApp(webClient, ALICE, ALICE_PASSWORD, SecuredFrontChannelServlet.SERVLET_PATH,
                generateURL(appName, SecuredFrontChannelServlet.SERVLET_PATH));

    }
    public void browserLoginToApp(WebClient webClient, String username, String password,
                           String expectedText, URL requestUrl) throws Exception {
        HtmlPage page = (HtmlPage)webClient.getPage(requestUrl);
        HtmlForm form = (HtmlForm)page.getElementById("kc-form-login");
        HtmlInput userName = (HtmlInput)form.getInputByName("username");
        userName.setValue(username);
        HtmlInput passwd = (HtmlInput)form.getInputByName("password");
        passwd.setValue(password);
        HtmlSubmitInput login = (HtmlSubmitInput)form.getInputByName("login");
        HtmlPage rtnPage = login.click();
        String rtnText = rtnPage.asXml();
        assertTrue("Expected result [ " + expectedText + " ] but was ["
                        + rtnText + "]", rtnText.contains(expectedText));
    }

    public void browserLogoutOfKeycloak(WebClient webClient, String appName) throws Exception {
        URL requestUrl = new URL(generateURL(appName, SecuredFrontChannelServlet.SERVLET_PATH).toString()+Constants.LOGOUT_PATH_VALUE);
        browserLogoutOfKeycloak(webClient, requestUrl);
    }

    public void browserLogoutOfKeycloak(WebClient webClient, URL requestUrL) throws Exception {
        HtmlPage pagelogout = (HtmlPage)webClient.getPage(requestUrL);
        Thread.sleep(3500); // give time for logout to complete
    }

    public void browserAssertUserLoggedIn(WebClient webClient, String appName, String expectedText) throws Exception {
        browserAccessPage(webClient, generateURL(appName, SecuredFrontChannelServlet.SERVLET_PATH), expectedText);
    }

    public void browserAssertUserLoggedOut(WebClient webClient, String appName, String expectedText) throws Exception {
        browserAccessPage(webClient, generateURL(appName, SecuredFrontChannelServlet.SERVLET_PATH), expectedText);
    }

    public void browserAccessPage(WebClient webClient, URL requestUrl, String expectedText) throws Exception {
        HtmlPage assertPage = (HtmlPage)webClient.getPage(requestUrl);
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
        HttpResponse response = null;
        Form keycloakLoginForm = null;

        int retryMax = 10;
        int retry = 0;
        boolean retryAgain = true;
        // allow for slow system response with limited retries
        do {
            Thread.sleep(500);
            response = httpClient.execute(getMethod, context);
            if (response.getStatusLine().getStatusCode() == expectedStatusCode) {
                try {
                    keycloakLoginForm = new Form(response);
                    retryAgain = false;
                } catch (IOException ee) {
                    // contiune retries
                }
            }
            retry++;
        } while(retryAgain &&  retry < retryMax);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (loginToKeycloak) {
                assertTrue("Expected code == OK but got " + statusCode
                        + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_OK);
                HttpResponse afterLoginClickResponse = simulateClickingOnButton(httpClient,
                        keycloakLoginForm, username, password, "Sign In");

                afterLoginClickResponse.getEntity().getContent();
                assertEquals(expectedStatusCode, afterLoginClickResponse.getStatusLine().getStatusCode());

                if (expectedText != null) {
                    String responseString = new BasicResponseHandler().handleResponse(afterLoginClickResponse);
                    assertTrue("Unexpected result " + responseString, responseString.contains(expectedText));
                }
            }
            else {
                assertTrue("Expected code == FORBIDDEN but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_FORBIDDEN);
            }
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public void logoutOfKeycloak(String appName, String expectedText) throws Exception {
        URI requestUri = new URL(generateURL(appName, SimpleSecuredServlet.SERVLET_PATH).toString()+Constants.LOGOUT_PATH_VALUE).toURI();
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
            response = httpClient.execute(getMethod, context);
            retry++;
        } while((response.getStatusLine().getStatusCode() != expectedStatusCode)
                &&  retry < retryMax);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (logoutFromKeycloak) {
                assertTrue("Expected code == OK but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_OK);
                response.getEntity();
                String responseString = new BasicResponseHandler().handleResponse(response);
                assertTrue("Unexpected result " + expectedText + " but result was [ "
                        + responseString +" ]", responseString.contains(expectedText));
            }
            else {
                assertTrue("Expected code == FORBIDDEN but got " + statusCode + " for request=" + requestUri, statusCode == HttpURLConnection.HTTP_FORBIDDEN);
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

        String responseString = null;
        int retryMax = 10;
        int retry = 0;
        // allow for slow system response with limited retries
        do {
            Thread.sleep(500);
            response = httpClient.execute(getMethod, context);
            response.getEntity();
            responseString = new BasicResponseHandler().handleResponse(response);
            retry++;
        } while((!responseString.contains(expectedText)) &&  retry < retryMax);

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            assertTrue("Expected code == " + expectedStatusCode + " but got "
                            + statusCode + " for request=" + requestUri,
                    statusCode == expectedStatusCode);
            assertTrue("Expected result [ " + expectedText + "] but was ["
                            + responseString + "]",
                    responseString.contains(expectedText));
        } finally {
            HttpClientUtils.closeQuietly(response);
        }
    }

    public HttpResponse simulateClickingOnButton(HttpClient client, Form form, String username, String password, String buttonValue) throws IOException {
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

    public final class Form {

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

    private final class Input {

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
        public String backChannelPath = null;
        public String frontChannelPath = null;
        public List<String> postLogoutRedirectPaths = null;

        public LogoutChannelPaths(final String backChannelPath,
                                 final String frontChannelPath,
                                 final List<String> postLogoutRedirectPaths) {
            this.backChannelPath = backChannelPath;
            this.frontChannelPath = frontChannelPath;
            this.postLogoutRedirectPaths = postLogoutRedirectPaths;
        }
    }

    protected static <T extends LoginLogoutBasics> void addSystemProperty(ManagementClient client, Class<T> clazz) throws Exception {
        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, LoginLogoutBasics.class.getName()));
        add.get(VALUE).set(clazz.getName());
        ManagementOperations.executeOperation(client.getControllerClient(), add);
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
