/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.clustering.cluster.web.authentication;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(Arquillian.class)
@ServerSetup({HttpSessionDigestTestCase.ServerSetup.class})
public class HttpSessionDigestTestCase extends AbstractClusteringTestCase {

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static WebArchive createNode1Deployment() {
        return getWebArchive();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static WebArchive createNode2Deployment() {
        return getWebArchive();
    }

    private static WebArchive getWebArchive() {
        WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "test.war");
        webArchive.addAsWebResource(Thread.currentThread().getContextClassLoader().getResource("elytron/digest/index.html"), "index.html");
        webArchive.addAsWebInfResource("elytron/digest/web.xml", "web.xml");
        return webArchive;
    }

    @Test
    @RunAsClient
    public void testHttpSessionDigestPropertyWithTwoServers() throws Exception {
        testDigestAuthenticationForTwoServers();
    }

    @Test
    @RunAsClient
    public void testHttpSessionDigestSameNonceCannotBeUsedTwice() throws Exception {
        String server1 = "http://localhost:8080/test/";
        String server2 = "http://localhost:8180/test/";

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpFirstGetRequest = new HttpGet(server1);
            HttpResponse response = httpclient.execute(httpFirstGetRequest);
            System.out.println(Arrays.stream(response.getAllHeaders()));
            Map<String, String> wwwAuth = Arrays.stream(response.getHeaders("WWW-Authenticate")[0].getElements())
                    .collect(Collectors.toMap(HeaderElement::getName, HeaderElement::getValue));
            String realm = wwwAuth.get("Digest realm");
            String nonce = wwwAuth.get("nonce");
            String uri = "/test/";

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

    private void testDigestAuthenticationForTwoServers() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        String server1 = "http://localhost:8080/test/";
        String server2 = "http://localhost:8180/test/";

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpFirstGetRequest = new HttpGet(server1);
            HttpResponse response = httpclient.execute(httpFirstGetRequest);
            Map<String, String> wwwAuth = Arrays.stream(response.getHeaders("WWW-Authenticate")[0].getElements())
                    .collect(Collectors.toMap(HeaderElement::getName, HeaderElement::getValue));
            String realm = wwwAuth.get("Digest realm");
            String nonce = wwwAuth.get("nonce");
            String uri = "/test/";

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
        }
    }

    private void addAuthenticateHeader(HttpGet httpGetRequestWithAuthHeader, String realm, String nonce, String uri) throws NoSuchAlgorithmException {
        httpGetRequestWithAuthHeader.setHeader("Authorization", "Digest " +
                "username=" + "\"user1\",\n" +
                "realm=\"" + realm + "\",\n" +
                "nonce=\"" + nonce + "\",\n" +
                "uri=\"" + uri + "\",\n" +
                "algorithm=\"" + "MD5" + "\",\n" +
                "response=\"" + computeDigest("/test/", nonce, "user1", "password1", "MD5", realm, "GET") +
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
                            .endBatch()
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
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
