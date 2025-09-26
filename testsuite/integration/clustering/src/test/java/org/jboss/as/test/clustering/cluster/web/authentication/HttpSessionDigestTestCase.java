/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.authentication;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(Arquillian.class)
@ServerSetup({HttpSessionDigestTestCase.ServerSetup.class})
public class HttpSessionDigestTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = HttpSessionDigestTestCase.class.getSimpleName();
    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return getDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return getDeployment();
    }

    private static Archive<?> getDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MODULE_NAME + ".war");
        war.addClass(DigestServlet.class);
        war.setWebXML(DigestServlet.class.getPackage(), "web-digest.xml");
        return war;
    }

    @Test
    @RunAsClient
    public void testHttpSessionDigestSameNonceCannotBeUsedTwice(@ArquillianResource(DigestServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1, @ArquillianResource(DigestServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws Exception {

        String server1 = baseURL1.toString();
        String server2 = baseURL2.toString();

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpFirstGetRequest = new HttpGet(server1);
            HttpResponse response = httpclient.execute(httpFirstGetRequest);
            Map<String, String> wwwAuth = Arrays.stream(response.getHeaders("WWW-Authenticate")[0].getElements())
                    .collect(Collectors.toMap(HeaderElement::getName, HeaderElement::getValue));
            String realm = wwwAuth.get("Digest realm");
            String nonce = wwwAuth.get("nonce");
            String uri = "/HttpSessionDigestTestCase/";

            // the first call always fails with a 401 and a requested nonce, realm, etc.
            Assert.assertEquals(401, response.getStatusLine().getStatusCode());
            httpFirstGetRequest.releaseConnection();

            // create response with headers
            HttpGet request = new HttpGet(server1);
            addAuthenticateHeader(request, realm, nonce, uri);

            // send a response to the server2 which did not send a challenge
            // the result is 200 because the nonce manager was configured to be persisted with "org.wildfly.security.http.use-session-based-digest-nonce-manager" option
            request.setURI(new URI(server2));
            Assert.assertEquals(200, httpclient.execute(request).getStatusLine().getStatusCode());
            request.releaseConnection();

            // try to send a same response to the server1 that have sent a challenge
            // 401 is returned because the same nonce cannot be used twice
            request.setURI(new URI(server1));
            response = httpclient.execute(request);
            Assert.assertEquals(401, response.getStatusLine().getStatusCode());
            request.releaseConnection();
        }
    }

    @Test
    @RunAsClient
    public void testDigestAuthenticationForTwoServers(@ArquillianResource(DigestServlet.class) @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1, @ArquillianResource(DigestServlet.class) @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2) throws IOException, NoSuchAlgorithmException, URISyntaxException {

        String server1 = baseURL1.toString();
        String server2 = baseURL2.toString();

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpFirstGetRequest = new HttpGet(server1);
            HttpResponse response = httpclient.execute(httpFirstGetRequest);
            Map<String, String> wwwAuth = Arrays.stream(response.getHeaders("WWW-Authenticate")[0].getElements())
                    .collect(Collectors.toMap(HeaderElement::getName, HeaderElement::getValue));
            String realm = wwwAuth.get("Digest realm");
            String nonce = wwwAuth.get("nonce");
            String uri = "/HttpSessionDigestTestCase/";

            // the first call always fails with a 401 and a requested nonce, realm, etc.
            Assert.assertEquals(401, response.getStatusLine().getStatusCode());
            httpFirstGetRequest.releaseConnection();

            // create response with headers
            HttpGet request = new HttpGet(server1);
            addAuthenticateHeader(request, realm, nonce, uri);

            // send a response to the server2 which did not send a challenge
            // the result is 200 because the nonce manager was configured to be persisted with "org.wildfly.security.http.use-session-based-digest-nonce-manager" option
            request.setURI(new URI(server2));
            int statusCode = httpclient.execute(request).getStatusLine().getStatusCode();
            Assert.assertEquals(200, statusCode);
            request.releaseConnection();
        }
    }

    private void addAuthenticateHeader(HttpGet httpGetRequestWithAuthHeader, String realm, String nonce, String uri) throws NoSuchAlgorithmException {
        httpGetRequestWithAuthHeader.setHeader("Authorization", "Digest " +
                "username=" + "\"user1\",\n" +
                "realm=\"" + realm + "\",\n" +
                "nonce=\"" + nonce + "\",\n" +
                "uri=\"" + uri + "\",\n" +
                "algorithm=\"" + "MD5" + "\",\n" +
                "response=\"" + computeDigest("/HttpSessionDigestTestCase/", nonce, "user1", "password1", "MD5", realm, "GET") +
                "\"");
    }

    private String computeDigest(String uri, String nonce, String username, String password, String algorithm, String realm, String method) throws NoSuchAlgorithmException, NoSuchAlgorithmException {
        String A1, HashA1, A2, HashA2;
        MessageDigest md = MessageDigest.getInstance(algorithm);
        A1 = username + ":" + realm + ":" + password;
        HashA1 = getMD5(A1);
        A2 = method + ":" + uri;
        HashA2 = getMD5(A2);
        String combo, finalHash;
        combo = HashA1 + ":" + nonce + ":" + HashA2;
        finalHash = DigestUtils.md5Hex(combo);
        return finalHash;
    }

    public String getMD5(String value) {
        return DigestUtils.md5Hex(value);
    }

    static class ServerSetup extends ManagementServerSetupTask {

        ServerSetup() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=elytron/configurable-http-server-mechanism-factory=configured-http:add(http-server-mechanism-factory=global,properties={org.wildfly.security.http.use-session-based-digest-nonce-manager=true})")
                            .add("/subsystem=elytron/http-authentication-factory=application-http-authentication:write-attribute(name=http-server-mechanism-factory,value=configured-http)")
                            .add("/subsystem=elytron/http-authentication-factory=application-http-authentication:write-attribute(name=mechanism-configurations,value=[{mechanism-name=DIGEST,mechanism-realm-configurations=[{realm-name=ApplicationRealm}]}])")
                            .add("/subsystem=undertow/application-security-domain=other:write-attribute(name=http-authentication-factory,value=application-http-authentication)")
                            .add("/subsystem=undertow/application-security-domain=other:undefine-attribute(name=security-domain)")
                            .add("/subsystem=logging/logger=org.wildfly.security:add(level=TRACE)")
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=logging/logger=org.wildfly.security:remove")
                            .add("/subsystem=undertow/application-security-domain=other:write-attribute(name=security-domain, value=ApplicationDomain)")
                            .add("/subsystem=undertow/application-security-domain=other:undefine-attribute(name=http-authentication-factory)")
                            .add("/subsystem=elytron/http-authentication-factory=application-http-authentication:write-attribute(name=mechanism-configurations,value=[{mechanism-name=BASIC,mechanism-realm-configurations=[{realm-name=ApplicationRealm}]}])")
                            .add("/subsystem=elytron/http-authentication-factory=application-http-authentication:write-attribute(name=http-server-mechanism-factory,value=global)")
                            .add("/subsystem=elytron/configurable-http-server-mechanism-factory=configured-http:remove")
                            .endBatch()
                            .build())
                    .build());
        }
    }
}
