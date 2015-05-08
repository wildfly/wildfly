/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.security.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.negotiation.JBossNegotiateSchemeFactory;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.security.auth.callback.UsernamePasswordHandler;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * Common utilities for JBoss AS security tests.
 *
 * @author Jan Lanik
 * @author Josef Cacek
 */
public class Utils extends CoreUtils {

    private static final Logger LOGGER = Logger.getLogger(Utils.class);

    public static final String UTF_8 = "UTF-8";

    public static final boolean IBM_JDK = StringUtils.startsWith(SystemUtils.JAVA_VENDOR, "IBM");
    public static final boolean OPEN_JDK = StringUtils.startsWith(SystemUtils.JAVA_VM_NAME, "OpenJDK");
    public static final boolean ORACLE_JDK = StringUtils.startsWith(SystemUtils.JAVA_VM_NAME, "Java HotSpot");

    /** The REDIRECT_STRATEGY for Apache HTTP Client */
    public static final RedirectStrategy REDIRECT_STRATEGY = new DefaultRedirectStrategy() {

        @Override
        public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
            boolean isRedirect = false;
            try {
                isRedirect = super.isRedirected(request, response, context);
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            if (!isRedirect) {
                final int responseCode = response.getStatusLine().getStatusCode();
                isRedirect = (responseCode == 301 || responseCode == 302);
            }
            return isRedirect;
        }
    };

    /**
     * Return MD5 hash of the given string value, encoded with given {@link Coding}. If the value or coding is <code>null</code>
     * then original value is returned.
     *
     * @param value
     * @param coding
     * @return encoded MD5 hash of the string or original value if some of parameters is null
     */
    public static String hashMD5(String value, Coding coding) {
        return (coding == null || value == null) ? value : hash(value, "MD5", coding);
    }

    public static String hash(String target, String algorithm, Coding coding) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] bytes = target.getBytes();
        byte[] byteHash = md.digest(bytes);

        String encodedHash = null;

        switch (coding) {
            case BASE_64:
                encodedHash = Base64.getEncoder().encodeToString(byteHash);
                break;
            case HEX:
                encodedHash = toHex(byteHash);
                break;
            default:
                throw new IllegalArgumentException("Unsuported coding:" + coding.name());
        }

        return encodedHash;
    }

    public static String toHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            // top 4 bits
            char c = (char) ((b >> 4) & 0xf);
            if (c > 9)
                c = (char) ((c - 10) + 'a');
            else
                c = (char) (c + '0');
            sb.append(c);
            // bottom 4 bits
            c = (char) (b & 0xf);
            if (c > 9)
                c = (char) ((c - 10) + 'a');
            else
                c = (char) (c + '0');
            sb.append(c);
        }
        return sb.toString();
    }

    public static URL getResource(String name) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return tccl.getResource(name);
    }

    private static final long STOP_DELAY_DEFAULT = 0;

    /**
     * stops execution of the program indefinitely useful in testsuite debugging
     */
    public static void stop() {
        stop(STOP_DELAY_DEFAULT);
    }

    /**
     * stop test execution for a given time interval useful for debugging
     *
     * @param delay interval (milliseconds), if delay<=0, interval is considered to be infinite (Long.MAX_VALUE)
     */
    public static void stop(long delay) {
        long currentTime = System.currentTimeMillis();
        long remainingTime = 0 < delay ? currentTime + delay - System.currentTimeMillis() : Long.MAX_VALUE;
        while (remainingTime > 0) {
            try {
                Thread.sleep(remainingTime);
            } catch (InterruptedException ex) {
                remainingTime = currentTime + delay - System.currentTimeMillis();
                continue;
            }
        }
    }

    public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (ModelNode update : updates) {
            applyUpdate(update, client);
        }
    }

    public static void applyUpdate(ModelNode update, final ModelControllerClient client) throws Exception {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Client update: " + update);
            LOGGER.info("Client update result: " + result);
        }
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            LOGGER.debug("Operation succeeded.");
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    /**
     * Read the contents of an HttpResponse's entity and return it as a String. The content is converted using the character set
     * from the entity (if any), failing that, "ISO-8859-1" is used.
     *
     * @param response
     * @return
     * @throws IOException
     */
    public static String getContent(HttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity());
    }

    /**
     * Makes HTTP call with FORM authentication.
     *
     * @param URL
     * @param user
     * @param pass
     * @param expectedStatusCode
     * @throws Exception
     */
    public static void makeCall(String URL, String user, String pass, int expectedStatusCode) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            HttpGet httpget = new HttpGet(URL);

            HttpResponse response = httpclient.execute(httpget);

            HttpEntity entity = response.getEntity();
            if (entity != null)
                EntityUtils.consume(entity);

            // We should get the Login Page
            StatusLine statusLine = response.getStatusLine();
            System.out.println("Login form get: " + statusLine);
            assertEquals(200, statusLine.getStatusCode());

            System.out.println("Initial set of cookies:");
            List<Cookie> cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("- " + cookies.get(i).toString());
                }
            }

            // We should now login with the user name and password
            HttpPost httpost = new HttpPost(URL + "/j_security_check");

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", pass));

            httpost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

            response = httpclient.execute(httpost);
            entity = response.getEntity();
            if (entity != null)
                EntityUtils.consume(entity);

            statusLine = response.getStatusLine();

            // Post authentication - we have a 302
            assertEquals(302, statusLine.getStatusCode());
            Header locationHeader = response.getFirstHeader("Location");
            String location = locationHeader.getValue();

            HttpGet httpGet = new HttpGet(location);
            response = httpclient.execute(httpGet);

            entity = response.getEntity();
            if (entity != null)
                EntityUtils.consume(entity);

            System.out.println("Post logon cookies:");
            cookies = httpclient.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                System.out.println("None");
            } else {
                for (int i = 0; i < cookies.size(); i++) {
                    System.out.println("- " + cookies.get(i).toString());
                }
            }

            // Either the authentication passed or failed based on the expected status code
            statusLine = response.getStatusLine();
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

    /**
     * Exports given archive to the given file path.
     *
     * @param archive
     * @param filePath
     */
    public static void saveArchive(Archive<?> archive, String filePath) {
        archive.as(ZipExporter.class).exportTo(new File(filePath), true);
    }

    /**
     * Exports given archive to the given folder.
     *
     * @param archive archive to export (not-<code>null</code>)
     * @param folderPath
     */
    public static void saveArchiveToFolder(Archive<?> archive, String folderPath) {
        final File exportFile = new File(folderPath, archive.getName());
        LOGGER.info("Exporting archive: " + exportFile.getAbsolutePath());
        archive.as(ZipExporter.class).exportTo(exportFile, true);
    }

    /**
     * Returns "secondary.test.address" system property if such exists. If not found, then there is a fallback to
     * {@link ManagementClient#getMgmtAddress()}. Returned value can be converted to canonical hostname if
     * useCanonicalHost==true. Returned value is not formatted for URLs (i.e. square brackets are not placed around IPv6 addr -
     * for instance "::1")
     *
     * @param mgmtClient management client instance (may be <code>null</code>)
     * @param useCanonicalHost
     * @return
     */
    public static String getSecondaryTestAddress(final ManagementClient mgmtClient, final boolean useCanonicalHost) {
        String address = System.getProperty("secondary.test.address");
        if (StringUtils.isBlank(address) && mgmtClient != null) {
            address = mgmtClient.getMgmtAddress();
        }
        if (useCanonicalHost) {
            address = getCannonicalHost(address);
        }
        return stripSquareBrackets(address);
    }

    /**
     * Returns "secondary.test.address" system property if such exists. If not found, then there is a fallback to
     * {@link ManagementClient#getMgmtAddress()}. Returned value is formatted to use in URLs (i.e. if it's IPv6 address, then
     * square brackets are placed around - e.g. "[::1]")
     *
     * @param mgmtClient management client instance (may be <code>null</code>)
     * @return
     */
    public static String getSecondaryTestAddress(final ManagementClient mgmtClient) {
        return NetworkUtils.formatPossibleIpv6Address(getSecondaryTestAddress(mgmtClient, false));
    }

    /**
     * Requests given URL and checks if the returned HTTP status code is the expected one. Returns HTTP response body
     *
     * @param url URL to which the request should be made
     * @param httpClient DefaultHttpClient to test multiple access
     * @param expectedStatusCode expected status code returned from the requested server
     * @return HTTP response body
     * @throws IOException
     * @throws URISyntaxException
     */
    public static String makeCallWithHttpClient(URL url, HttpClient httpClient, int expectedStatusCode) throws IOException,
            URISyntaxException {

        String httpResponseBody = null;
        HttpGet httpGet = new HttpGet(url.toURI());
        HttpResponse response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        LOGGER.info("Request to: " + url + " responds: " + statusCode);

        assertEquals("Unexpected status code", expectedStatusCode, statusCode);

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            httpResponseBody = EntityUtils.toString(response.getEntity());
            EntityUtils.consume(entity);
        }
        return httpResponseBody;
    }

    /**
     * Returns response body for the given URL request as a String. It also checks if the returned HTTP status code is the
     * expected one. If the server returns {@link HttpServletResponse#SC_UNAUTHORIZED} and username is provided, then a new
     * request is created with the provided credentials (basic authentication).
     *
     * @param url URL to which the request should be made
     * @param user Username (may be null)
     * @param pass Password (may be null)
     * @param expectedStatusCode expected status code returned from the requested server
     * @return HTTP response body
     * @throws IOException
     * @throws URISyntaxException
     */
    public static String makeCallWithBasicAuthn(URL url, String user, String pass, int expectedStatusCode) throws IOException,
            URISyntaxException {
        LOGGER.info("Requesting URL " + url);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            final HttpGet httpGet = new HttpGet(url.toURI());
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpServletResponse.SC_UNAUTHORIZED != statusCode || StringUtils.isEmpty(user)) {
                assertEquals("Unexpected HTTP response status code.", expectedStatusCode, statusCode);
                return EntityUtils.toString(response.getEntity());
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTTP response was SC_UNAUTHORIZED, let's authenticate the user " + user);
            }
            HttpEntity entity = response.getEntity();
            if (entity != null)
                EntityUtils.consume(entity);

            final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user, pass);
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort()), credentials);

            response = httpClient.execute(httpGet);
            statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
            return EntityUtils.toString(response.getEntity());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Returns response body for the given URL request as a String. It also checks if the returned HTTP status code is the
     * expected one. If the server returns {@link HttpServletResponse#SC_UNAUTHORIZED} and an username is provided, then the
     * given user is authenticated against Kerberos and a new request is executed under the new subject.
     *
     * @param uri URI to which the request should be made
     * @param user Username
     * @param pass Password
     * @param expectedStatusCode expected status code returned from the requested server
     * @return HTTP response body
     * @throws IOException
     * @throws URISyntaxException
     * @throws PrivilegedActionException
     * @throws LoginException
     */
    public static String makeCallWithKerberosAuthn(final URI uri, final String user, final String pass,
            final int expectedStatusCode) throws IOException, URISyntaxException, PrivilegedActionException, LoginException {
        LOGGER.info("Requesting URI: " + uri);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, new JBossNegotiateSchemeFactory(true));
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, -1, null), new NullHCCredentials());

            final HttpGet httpGet = new HttpGet(uri);
            final HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpServletResponse.SC_UNAUTHORIZED != statusCode || StringUtils.isEmpty(user)) {
                assertEquals("Unexpected HTTP response status code.", expectedStatusCode, statusCode);
                return EntityUtils.toString(response.getEntity());
            }
            final HttpEntity entity = response.getEntity();
            final Header[] authnHeaders = response.getHeaders("WWW-Authenticate");
            assertTrue("WWW-Authenticate header is present", authnHeaders != null && authnHeaders.length > 0);
            final Set<String> authnHeaderValues = new HashSet<String>();
            for (final Header header : authnHeaders) {
                authnHeaderValues.add(header.getValue());
            }
            assertTrue("WWW-Authenticate: Negotiate header is missing", authnHeaderValues.contains("Negotiate"));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("HTTP response was SC_UNAUTHORIZED, let's authenticate the user " + user);
            }
            if (entity != null)
                EntityUtils.consume(entity);

            // Use our custom configuration to avoid reliance on external config
            Configuration.setConfiguration(new Krb5LoginConfiguration());
            // 1. Authenticate to Kerberos.
            final LoginContext lc = new LoginContext(CoreUtils.class.getName(), new UsernamePasswordHandler(user, pass));
            lc.login();

            // 2. Perform the work as authenticated Subject.
            final String responseBody = Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<String>() {
                public String run() throws Exception {
                    final HttpResponse response = httpClient.execute(httpGet);
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
                    return EntityUtils.toString(response.getEntity());
                }
            });
            lc.logout();
            return responseBody;
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Creates request against SPNEGO protected web-app with FORM fallback. It tries to login using SPNEGO first - if it fails,
     * FORM is used.
     *
     * @param contextUrl
     * @param page
     * @param user
     * @param pass
     * @param expectedStatusCode
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws PrivilegedActionException
     * @throws LoginException
     */
    public static String makeHttpCallWithFallback(final String contextUrl, final String page, final String user,
            final String pass, final int expectedStatusCode) throws IOException, URISyntaxException, PrivilegedActionException,
            LoginException {
        final String strippedContextUrl = StringUtils.stripEnd(contextUrl, "/");
        final String url = strippedContextUrl + page;
        LOGGER.info("Requesting URL: " + url);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.setRedirectStrategy(REDIRECT_STRATEGY);
        String unauthorizedPageBody = null;
        try {
            httpClient.getAuthSchemes().register(AuthPolicy.SPNEGO, new JBossNegotiateSchemeFactory(true));
            httpClient.getCredentialsProvider().setCredentials(new AuthScope(null, -1, null), new NullHCCredentials());

            final HttpGet httpGet = new HttpGet(url);
            final HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpServletResponse.SC_UNAUTHORIZED != statusCode || StringUtils.isEmpty(user)) {
                assertEquals("Unexpected HTTP response status code.", expectedStatusCode, statusCode);
                return EntityUtils.toString(response.getEntity());
            }
            final Header[] authnHeaders = response.getHeaders("WWW-Authenticate");
            assertTrue("WWW-Authenticate header is present", authnHeaders != null && authnHeaders.length > 0);
            final Set<String> authnHeaderValues = new HashSet<String>();
            for (final Header header : authnHeaders) {
                authnHeaderValues.add(header.getValue());
            }
            assertTrue("WWW-Authenticate: Negotiate header is missing", authnHeaderValues.contains("Negotiate"));

            LOGGER.debug("HTTP response was SC_UNAUTHORIZED, let's authenticate the user " + user);
            unauthorizedPageBody = EntityUtils.toString(response.getEntity());

            // Use our custom configuration to avoid reliance on external config
            Configuration.setConfiguration(new Krb5LoginConfiguration());
            // 1. Authenticate to Kerberos.
            final LoginContext lc = new LoginContext(CoreUtils.class.getName(), new UsernamePasswordHandler(user, pass));
            lc.login();

            // 2. Perform the work as authenticated Subject.
            final String responseBody = Subject.doAs(lc.getSubject(), new PrivilegedExceptionAction<String>() {
                public String run() throws Exception {
                    final HttpResponse response = httpClient.execute(httpGet);
                    int statusCode = response.getStatusLine().getStatusCode();
                    assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
                    return EntityUtils.toString(response.getEntity());
                }
            });
            lc.logout();
            return responseBody;
        } catch (LoginException e) {
            assertNotNull(unauthorizedPageBody);
            assertTrue(unauthorizedPageBody.contains("j_security_check"));

            HttpPost httpPost = new HttpPost(strippedContextUrl + "/j_security_check");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("j_username", user));
            nameValuePairs.add(new BasicNameValuePair("j_password", pass));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            final HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
            return EntityUtils.toString(response.getEntity());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Creates request against SPNEGO protected web-app with FORM fallback. It doesn't try to login using SPNEGO - it uses FORM
     * authn directly.
     *
     * @param contextUrl
     * @param page
     * @param user
     * @param pass
     * @param expectedStatusCode
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws PrivilegedActionException
     * @throws LoginException
     */
    public static String makeHttpCallWoSPNEGO(final String contextUrl, final String page, final String user, final String pass,
            final int expectedStatusCode) throws IOException, URISyntaxException, PrivilegedActionException, LoginException {
        final String strippedContextUrl = StringUtils.stripEnd(contextUrl, "/");
        final String url = strippedContextUrl + page;
        LOGGER.info("Requesting URL: " + url);
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        httpClient.setRedirectStrategy(REDIRECT_STRATEGY);
        String unauthorizedPageBody = null;
        try {
            final HttpGet httpGet = new HttpGet(url);
            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (HttpServletResponse.SC_UNAUTHORIZED != statusCode || StringUtils.isEmpty(user)) {
                assertEquals("Unexpected HTTP response status code.", expectedStatusCode, statusCode);
                return EntityUtils.toString(response.getEntity());
            }
            final Header[] authnHeaders = response.getHeaders("WWW-Authenticate");
            assertTrue("WWW-Authenticate header is present", authnHeaders != null && authnHeaders.length > 0);
            final Set<String> authnHeaderValues = new HashSet<String>();
            for (final Header header : authnHeaders) {
                authnHeaderValues.add(header.getValue());
            }
            assertTrue("WWW-Authenticate: Negotiate header is missing", authnHeaderValues.contains("Negotiate"));

            LOGGER.debug("HTTP response was SC_UNAUTHORIZED, let's authenticate the user " + user);
            unauthorizedPageBody = EntityUtils.toString(response.getEntity());

            assertNotNull(unauthorizedPageBody);
            LOGGER.info(unauthorizedPageBody);
            assertTrue(unauthorizedPageBody.contains("j_security_check"));

            HttpPost httpPost = new HttpPost(strippedContextUrl + "/j_security_check");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("j_username", user));
            nameValuePairs.add(new BasicNameValuePair("j_password", pass));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            response = httpClient.execute(httpPost);
            statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
            return EntityUtils.toString(response.getEntity());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
     * Sets or removes (in case value==null) a system property. It's only a helper method, which avoids
     * {@link NullPointerException} thrown from {@link System#setProperty(String, String)} method, when the value is
     * <code>null</code>.
     *
     * @param key property name
     * @param value property value
     * @return the previous string value of the system property
     */
    public static String setSystemProperty(final String key, final String value) {
        return value == null ? System.clearProperty(key) : System.setProperty(key, value);
    }

    /**
     * Generates content of jboss-ejb3.xml file as a ShrinkWrap asset with the given security domain name.
     *
     * @param securityDomain security domain name
     * @return Asset instance
     */
    public static Asset getJBossEjb3XmlAsset(final String securityDomain) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss:ejb-jar xmlns:jboss='http://www.jboss.com/xml/ns/javaee'");
        sb.append("\n\txmlns='http://java.sun.com/xml/ns/javaee'");
        sb.append("\n\txmlns:s='urn:security'");
        sb.append("\n\tversion='3.1'");
        sb.append("\n\timpl-version='2.0'>");
        sb.append("\n\t<assembly-descriptor><s:security>");
        sb.append("\n\t\t<ejb-name>*</ejb-name>");
        sb.append("\n\t\t<s:security-domain>").append(securityDomain).append("</s:security-domain>");
        sb.append("\n\t</s:security></assembly-descriptor>");
        sb.append("\n</jboss:ejb-jar>");
        return new StringAsset(sb.toString());
    }

    /**
     * Generates content of jboss-web.xml file as a ShrinkWrap asset with the given security domain name and given valve class.
     *
     * @param securityDomain security domain name (not-<code>null</code>)
     * @param valveClassNames valve class (e.g. an Authenticator) which should be added to jboss-web file (may be
     *        <code>null</code>)
     * @return Asset instance
     */
    public static Asset getJBossWebXmlAsset(final String securityDomain, final String... valveClassNames) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss-web>");
        sb.append("\n\t<security-domain>").append(securityDomain).append("</security-domain>");
        if (valveClassNames != null) {
            for (String valveClassName : valveClassNames) {
                if (StringUtils.isNotEmpty(valveClassName)) {
                    sb.append("\n\t<valve><class-name>").append(valveClassName).append("</class-name></valve>");
                }
            }
        }
        sb.append("\n</jboss-web>");
        return new StringAsset(sb.toString());
    }

    /**
     * Generates content of the jboss-deployment-structure.xml deployment descriptor as a ShrinkWrap asset. It fills the given
     * dependencies (module names) into it.
     *
     * @param dependencies AS module names
     * @return
     */
    public static Asset getJBossDeploymentStructure(String... dependencies) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<jboss-deployment-structure><deployment><dependencies>");
        if (dependencies != null) {
            for (String moduleName : dependencies) {
                sb.append("\n\t<module name='").append(moduleName).append("'/>");
            }
        }
        sb.append("\n</dependencies></deployment></jboss-deployment-structure>");
        return new StringAsset(sb.toString());
    }

    /**
     * Creates content of permissions.xml (or jboss-permissions.xml) which placed in META-INF of the deployment specifies
     * security permissions granted to the deployment.
     *
     * @param permissions instances from which are &lt;permission&gt; elements generated
     * @return not-<code>null</code> content of permissions.xml (version=7)
     */
    public static Asset getPermissionsXml(final Permission... permissions) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<permissions xmlns='http://xmlns.jcp.org/xml/ns/javaee' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/permissions_7.xsd' version='7'>");
        if (permissions != null) {
            for (Permission permission : permissions) {
                if (permission != null) {
                    sb.append("\n\t<permission>");
                    sb.append("\n\t\t<class-name>").append(permission.getClass().getName()).append("</class-name>");
                    if (permission.getName() != null) {
                        sb.append("\n\t\t<name>").append(permission.getName()).append("</name>");
                    }
                    if (permission.getActions() != null) {
                        sb.append("\n\t\t<actions>").append(permission.getActions()).append("</actions>");
                    }
                    sb.append("\n\t</permission>");
                }
            }
        }
        sb.append("\n</permissions>");
        return new StringAsset(sb.toString());
    }

    /**
     * Creates content of users.properties and/or roles.properties files for given array of role names.
     * <p>
     * For instance if you provide 2 roles - "role1", "role2" then the result will be:
     *
     * <pre>
     * role1=role1
     * role2=role2
     * </pre>
     *
     * If you use it as users.properties and roles.properties, then <code>roleName == userName == password</code>
     *
     * @param roles role names (used also as user names and passwords)
     * @return not-<code>null</code> content of users.properties and/or roles.properties
     */
    public static String createUsersFromRoles(String... roles) {
        final StringBuilder sb = new StringBuilder();
        if (roles != null) {
            for (String role : roles) {
                sb.append(role).append("=").append(role).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Strips square brackets - '[' and ']' from the given string. It can be used for instance to remove the square brackets
     * around IPv6 address in a URL.
     *
     * @param str string to strip
     * @return str without square brackets in it
     */
    public static String stripSquareBrackets(final String str) {
        return StringUtils.strip(str, "[]");
    }

    /**
     * Fixes/replaces LDAP bind address in the CreateTransport annotation of ApacheDS.
     *
     * @param createLdapServer
     * @param address
     */
    public static void fixApacheDSTransportAddress(ManagedCreateLdapServer createLdapServer, String address) {
        final CreateTransport[] createTransports = createLdapServer.transports();
        for (int i = 0; i < createTransports.length; i++) {
            final ManagedCreateTransport mgCreateTransport = new ManagedCreateTransport(createTransports[i]);
            // localhost is a default used in original CreateTransport annotation. We use it as a fallback.
            mgCreateTransport.setAddress(address != null ? address : "localhost");
            createTransports[i] = mgCreateTransport;
        }
    }

    /**
     * Copies server and clients keystores and truststores from this package to the given folder. Server truststore has accepted
     * certificate from client keystore and vice-versa
     *
     * @param workingFolder folder to which key material should be copied
     * @throws IOException copying of keystores fails
     * @throws IllegalArgumentException workingFolder is null or it's not a directory
     */
    public static void createKeyMaterial(final File workingFolder) throws IOException, IllegalArgumentException {
        if (workingFolder == null || !workingFolder.isDirectory()) {
            throw new IllegalArgumentException("Provide an existing folder as the method parameter.");
        }
        createTestResource(new File(workingFolder, SecurityTestConstants.SERVER_KEYSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.SERVER_TRUSTSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.SERVER_CRT));
        createTestResource(new File(workingFolder, SecurityTestConstants.CLIENT_KEYSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.CLIENT_TRUSTSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.CLIENT_CRT));
        createTestResource(new File(workingFolder, SecurityTestConstants.UNTRUSTED_KEYSTORE));
        createTestResource(new File(workingFolder, SecurityTestConstants.UNTRUSTED_CRT));
        LOGGER.info("Key material created in " + workingFolder.getAbsolutePath());
    }

    /**
     * Copies a resource file from current package to location denoted by given {@link File} instance.
     *
     * @param file
     *
     * @throws IOException
     */
    private static void createTestResource(File file) throws IOException {
        FileOutputStream fos = null;
        LOGGER.info("Creating test file " + file.getAbsolutePath());
        try {
            fos = new FileOutputStream(file);
            IOUtils.copy(CoreUtils.class.getResourceAsStream(file.getName()), fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    public static String propertiesReplacer(String originalFile, File keystoreFile, File trustStoreFile, String keystorePassword) {
        return propertiesReplacer(originalFile, keystoreFile.getAbsolutePath(), trustStoreFile.getAbsolutePath(),
                keystorePassword, null);
    }

    public static String propertiesReplacer(String originalFile, File keystoreFile, File trustStoreFile,
            String keystorePassword, String vaultConfig) {
        return propertiesReplacer(originalFile, keystoreFile.getAbsolutePath(), trustStoreFile.getAbsolutePath(),
                keystorePassword, vaultConfig);
    }

    /**
     * Replace keystore paths and passwords variables in original configuration file with given values and set ${hostname}
     * variable from system property: node0
     *
     * @param originalFile String
     * @param keystoreFile File
     * @param trustStoreFile File
     * @param keystorePassword String
     * @param vaultConfig - path to vault settings
     * @return String content
     */
    public static String propertiesReplacer(String originalFile, String keystoreFile, String trustStoreFile,
            String keystorePassword, String vaultConfig) {
        String hostname = getDefaultHost(false);

        // expand possible IPv6 address
        try {
            hostname = NetworkUtils.formatPossibleIpv6Address(InetAddress.getByName(hostname).getHostAddress());
        } catch (UnknownHostException ex) {
            String message = "Cannot resolve host address: " + hostname + " , error : " + ex.getMessage();
            LOGGER.error(message);
            throw new RuntimeException(ex);
        }

        final Map<String, String> map = new HashMap<String, String>();
        String content = "";
        if (vaultConfig == null) {
            map.put("vaultConfig", "");
        } else {
            map.put("vaultConfig", vaultConfig);
        }
        map.put("hostname", hostname);
        map.put("keystore", keystoreFile);
        map.put("truststore", trustStoreFile);
        map.put("password", keystorePassword);

        try {
            content = StrSubstitutor.replace(IOUtils.toString(CoreUtils.class.getResourceAsStream(originalFile), "UTF-8"), map);
        } catch (IOException ex) {
            String message = "Cannot find or modify configuration file " + originalFile + " , error : " + ex.getMessage();
            LOGGER.error(message);
            throw new RuntimeException(ex);
        }

        return content;
    }

    /**
     * Makes HTTP call without authentication. Returns response body as a String.
     *
     * @param uri requested URL
     * @param expectedStatusCode expected status code - it's checked after the request is executed
     * @throws Exception
     */
    public static String makeCall(URI uri, int expectedStatusCode) throws Exception {
        final DefaultHttpClient httpclient = new DefaultHttpClient();
        try {
            final HttpGet httpget = new HttpGet(uri);
            final HttpResponse response = httpclient.execute(httpget);
            int statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode, statusCode);
            return EntityUtils.toString(response.getEntity());
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    /**
     * Returns param/value pair in form "urlEncodedName=urlEncodedValue". It can be used for instance in HTTP get queries.
     *
     * @param paramName parameter name
     * @param paramValue parameter value
     * @return "[urlEncodedName]=[urlEncodedValue]" string
     */
    public static String encodeQueryParam(final String paramName, final String paramValue) {
        String response = null;
        try {
            response = StringUtils.isEmpty(paramValue) ? null : (URLEncoder.encode(paramName, UTF_8) + "=" + URLEncoder.encode(
                    StringUtils.defaultString(paramValue, StringUtils.EMPTY), UTF_8));
        } catch (UnsupportedEncodingException e) {
            // should never happen - everybody likes the "UTF-8" :)
        }
        return response;
    }

    /**
     * Returns management address (host) from the givem {@link org.jboss.as.arquillian.container.ManagementClient}. If the
     * returned value is IPv6 address then square brackets around are stripped.
     *
     * @param managementClient
     * @return
     */
    public static final String getHost(final ManagementClient managementClient) {
        return CoreUtils.stripSquareBrackets(managementClient.getMgmtAddress());
    }

    /**
     * Returns canonical hostname retrieved from management address of the givem
     * {@link org.jboss.as.arquillian.container.ManagementClient}.
     *
     * @param managementClient
     * @return
     */
    public static final String getCannonicalHost(final ManagementClient managementClient) {
        return getCannonicalHost(managementClient.getMgmtAddress());
    }

    /**
     * Returns servlet URL, as concatenation of webapp URL and servlet path.
     *
     * @param webAppURL web application context URL (e.g. injected by Arquillian)
     * @param servletPath Servlet path starting with slash (must be not-<code>null</code>)
     * @param mgmtClient Management Client (may be null)
     * @param useCanonicalHost flag which says if host in URI should be replaced by the canonical host.
     * @return
     * @throws java.net.URISyntaxException
     */
    public static final URI getServletURI(final URL webAppURL, final String servletPath, final ManagementClient mgmtClient,
            boolean useCanonicalHost) throws URISyntaxException {
        URI resultURI = new URI(webAppURL.toExternalForm() + servletPath.substring(1));
        if (useCanonicalHost) {
            resultURI = replaceHost(resultURI, getCannonicalHost(mgmtClient));
        }
        return resultURI;
    }

    /**
     * Returns hostname - either read from the "node0" system property or the loopback address "127.0.0.1".
     *
     * @param canonical return hostname in canonical form
     *
     * @return
     */
    public static String getDefaultHost(boolean canonical) {
        final String hostname = System.getProperty("node0", "127.0.0.1");
        return canonical ? getCannonicalHost(hostname) : hostname;
    }
}
