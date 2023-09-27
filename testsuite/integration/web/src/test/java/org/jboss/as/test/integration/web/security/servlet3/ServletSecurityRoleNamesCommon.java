/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.servlet3;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

/**
 * Common part for testing servlet role-names.
 *
 * @author Jan Stourac
 */
public abstract class ServletSecurityRoleNamesCommon {

    protected static final String WAR_SUFFIX = ".war";

    protected static final String SECURED_INDEX = "secured/index.html";
    protected static final String WEAKLY_SECURED_INDEX = "weakly-secured/index.html";
    protected static final String HARD_SECURED_INDEX = "hard-secured/index.html";

    protected static final String SECURED_INDEX_CONTENT = "GOOD - secured";
    protected static final String WEAKLY_SECURED_INDEX_CONTENT = "GOOD weakly secured";
    protected static final String HARD_SECURED_INDEX_CONTENT = "GOOD - hard secured";

    /**
     * Test with user "anil" who has the right password and the right role to access the servlet.
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedSuccessfulAuth() throws Exception {
        makeCallSecured("anil", "anil", 200);
        makeCallWeaklySecured("anil", "anil", 200);
        makeCallHardSecured("anil", "anil", 403);
    }

    /**
     * <p>
     * Test with user "marcus" who has the right password but does not have the right role.
     * </p>
     * <p>
     * Should be a HTTP/403
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth() throws Exception {
        makeCallSecured("marcus", "marcus", 403);
        makeCallWeaklySecured("marcus", "marcus", 200);
        makeCallHardSecured("marcus", "marcus", 403);
    }

    /**
     * <p>
     * Test with non-existent user "non-existent-user".
     * </p>
     * <p>
     * Should be a HTTP/403
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuthNonExistentUser() throws Exception {
        makeCallSecured("non-existent-user", "non-existent-user", 401);
        makeCallWeaklySecured("non-existent-user", "non-existent-user", 401);
        makeCallHardSecured("non-existent-user", "non-existent-user", 403);
    }

    protected abstract void makeCallSecured(String user, String pass, int expectedCode) throws Exception;

    protected abstract void makeCallWeaklySecured(String user, String pass, int expectedCode) throws Exception;

    protected abstract void makeCallHardSecured(String user, String pass, int expectedCode) throws Exception;

    /**
     * Method that needs to be overridden with the HTTPClient code.
     *
     * @param user         username
     * @param pass         password
     * @param expectedCode http status code
     * @throws Exception
     */
    protected void makeCall(String user, String pass, int expectedCode, URL url) throws Exception {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(url.getHost(), url.getPort()),
                new UsernamePasswordCredentials(user, pass));
        try (CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build()) {

            HttpGet httpget = new HttpGet(url.toExternalForm());

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(expectedCode, statusLine.getStatusCode());

            EntityUtils.consume(entity);
        }
    }
}
