package org.jboss.as.test.integration.security.picketlink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
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
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.Krb5LoginConfiguration;
import org.jboss.as.test.integration.security.common.NullHCCredentials;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.negotiation.JBossNegotiateSchemeFactory;
import org.jboss.logging.Logger;
import org.jboss.security.auth.callback.UsernamePasswordHandler;

/**
 * Base class with common utilities for PicketLink integration tests
 * 
 * @author Filip Bogyai
 */
public class PicketLinkTestBase {

    public static final String ANIL = "anil";
    public static final String MARCUS = "marcus";

    public static final String USERS = ANIL + "=" + ANIL + "\n" + MARCUS + "=" + MARCUS;
    public static final String ROLES = ANIL + "=" + "gooduser" + "\n" + MARCUS + "=baduser";

    private static final Logger LOGGER = Logger.getLogger(PicketLinkTestBase.class);

    /**
     * Requests given URL and checks if the returned HTTP status code is the expected one. Returns HTTP response body
     * 
     * @param URL url to which the request should be made
     * @param DefaultHttpClient httpClient to test multiple access
     * @param expectedStatusCode expected status code returned from the requested server
     * @return HTTP response body
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    public static String makeCall(URL url, DefaultHttpClient httpClient, int expectedStatusCode)
            throws ClientProtocolException, IOException, URISyntaxException {

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
     * Requests given URL and returns redirect location URL from response header. If response is not redirected then returns the
     * same URL which was requested
     * 
     * @param URL url to which the request should be made
     * @param DefaultHttpClient httpClient to test multiple access
     * @return URL redirect location
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    public static URL makeCallWithoutRedirect(URL url, DefaultHttpClient httpClient) throws ClientProtocolException,
            IOException, URISyntaxException {

        HttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        String redirectLocation = url.toExternalForm();

        HttpGet httpGet = new HttpGet(url.toURI());
        httpGet.setParams(params);
        HttpResponse response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        LOGGER.info("Request to: " + url + " responds: " + statusCode);

        Header locationHeader = response.getFirstHeader("location");
        if (locationHeader != null) {
            redirectLocation = locationHeader.getValue();
        }

        HttpEntity entity = response.getEntity();
        if (entity != null) {
            EntityUtils.consume(entity);
        }
        return new URL(redirectLocation);
    }

    /**
     * Requests given SP and post SAMLRequest to IdP, then post back SAMLResponse. Returns HTTP response body
     * 
     * @param URL spURL of requested Service Provider
     * @param URL idpURL of Identity Provider
     * @param DefaultHttpClient httpClient to test multiple access
     * @return HTTP response body
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    public static String postSAML2Assertions(URL spURL, URL idpURL, DefaultHttpClient httpClient)
            throws ClientProtocolException, IOException, URISyntaxException {

        String httpResponseBody = makeCall(spURL, httpClient, 200);

        // parse SAMLRequest and post it to IdP
        String[] splitted = httpResponseBody.split("NAME=\"SAMLRequest\" VALUE=\"");
        String samlRequest = splitted[1].substring(0, splitted[1].indexOf("\""));
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("SAMLRequest", samlRequest));

        HttpPost httpPost = new HttpPost(idpURL.toURI());
        httpPost.setEntity(new UrlEncodedFormEntity(pairs));
        HttpResponse httpResponse = httpClient.execute(httpPost);

        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            httpResponseBody = EntityUtils.toString(httpResponse.getEntity());
            EntityUtils.consume(entity);
        }

        // parse SAMLResponse and post it back to SP
        splitted = httpResponseBody.split("NAME=\"SAMLResponse\" VALUE=\"");
        String samlResponse = splitted[1].substring(0, splitted[1].indexOf("\""));
        pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("SAMLResponse", samlResponse));

        httpPost = new HttpPost(spURL.toURI());
        httpPost.setEntity(new UrlEncodedFormEntity(pairs));
        httpResponse = httpClient.execute(httpPost);

        entity = httpResponse.getEntity();
        if (entity != null) {
            httpResponseBody = EntityUtils.toString(httpResponse.getEntity());
            EntityUtils.consume(entity);
        }

        return httpResponseBody;
    }

    /**
     * Replace variables in PicketLink configurations files with given values and set ${hostname} variable from system property:
     * node0
     * 
     * @param String resourceFile
     * @param String deploymentName
     * @param String bindingType
     * @return String content
     */
    public static String propertiesReplacer(String resourceFile, String deploymentName, String bindingType,
            String idpContextPath) {

        String hostname = System.getProperty("node0");

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
        map.put("hostname", hostname);
        map.put("deployment", deploymentName);
        map.put("bindingType", bindingType);
        map.put("idpContextPath", idpContextPath);

        try {
            content = StrSubstitutor.replace(
                    IOUtils.toString(SAML2BasicAuthenticationTestCase.class.getResourceAsStream(resourceFile), "UTF-8"), map);
        } catch (IOException ex) {
            String message = "Cannot find or modify configuration file " + resourceFile + " , error : " + ex.getMessage();
            LOGGER.error(message);
            throw new RuntimeException(ex);
        }
        return content;
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
    public static String makeCallWithKerberosAuthn(final URI uri, final DefaultHttpClient httpClient, final String user,
            final String pass, final int expectedStatusCode) throws IOException, URISyntaxException, PrivilegedActionException,
            LoginException {
        LOGGER.info("Requesting URI: " + uri);
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
        final LoginContext lc = new LoginContext(Utils.class.getName(), new UsernamePasswordHandler(user, pass));
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
    }

    /**
     * A {@link ServerSetupTask} instance which creates security domains for Identity Provider(IdP) and Service Provider(SP)
     * 
     * @author Filip Bogyai
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         * 
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() {

            final SecurityDomain idp = new SecurityDomain.Builder()
                    .name("idp")
                    .cacheType("default")
                    .loginModules(
                            new SecurityModule.Builder().name("UsersRoles").flag("required")
                                    .putOption("usersProperties", "users.properties")
                                    .putOption("rolesProperties", "roles.properties").build()) //
                    .build();
            final SecurityDomain sp = new SecurityDomain.Builder()
                    .name("sp")
                    .cacheType("default")
                    .loginModules(
                            new SecurityModule.Builder()
                                    .name("org.picketlink.identity.federation.bindings.jboss.auth.SAML2LoginModule")
                                    .flag("required").build()) //
                    .build();
            return new SecurityDomain[] { idp, sp };
        }
    }

}
