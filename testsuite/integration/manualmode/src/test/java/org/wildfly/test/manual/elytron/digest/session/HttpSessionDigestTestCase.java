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
package org.wildfly.test.manual.elytron.digest.session;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jetbrains.annotations.NotNull;
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
public class HttpSessionDigestTestCase {

    public static final int MGMT_PORT_NODE_1 = 10090;
    public static final int MGMT_PORT_NODE_2 = 10190;
    public static final String LOCALHOST = "localhost";
    @ArquillianResource
    private static ContainerController serverController;
    private static final String CONAINER_NODE_1 = "session-digest-node1";
    private static final String CONAINER_NODE_2 = "session-digest-node2";

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = "DEPLOYMENT_NODE_1", managed = false, testable = false)
    @TargetsContainer(CONAINER_NODE_1)
    public static Archive<?> createNode1Deployment() {
        return getWebArchive();
    }

    @Deployment(name = "DEPLOYMENT_NODE_2", managed = false, testable = false)
    @TargetsContainer(CONAINER_NODE_2)
    public static Archive<?> createNode2Deployment() {
        return getWebArchive();
    }

    @NotNull
    private static WebArchive getWebArchive() {
        WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "test.war");
        webArchive.addAsWebResource(Thread.currentThread().getContextClassLoader().getResource("elytron/digest/index.html"), "index.html");
        webArchive.addAsWebInfResource("elytron/digest/web.xml", "web.xml");
        return webArchive;
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void setup() throws Exception {
        if (!serverController.isStarted(CONAINER_NODE_1)) {
            serverController.start(CONAINER_NODE_1);
        }
        if (!serverController.isStarted(CONAINER_NODE_2)) {
            serverController.start(CONAINER_NODE_2);
        }

        configureServerWithFSRealmAndSessionDigestProperty(new CLIWrapper(LOCALHOST, MGMT_PORT_NODE_1, true));
        configureServerWithFSRealmAndSessionDigestProperty(new CLIWrapper(LOCALHOST, MGMT_PORT_NODE_2, true));

        try {
            deployer.deploy("DEPLOYMENT_NODE_1");
            deployer.deploy("DEPLOYMENT_NODE_2");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private static void configureServerWithFSRealmAndSessionDigestProperty(CLIWrapper cli2) {
        cli2.sendLine("/subsystem=elytron/filesystem-realm=exampleFsRealm:add(path=fs-realm-users,relative-to=jboss.server.config.dir)");
        cli2.sendLine("/subsystem=elytron/filesystem-realm=exampleFsRealm:add-identity(identity=jane)");
        cli2.sendLine("/subsystem=elytron/filesystem-realm=exampleFsRealm:set-password(clear={password=\"passwordJane\"}, identity=jane)");
        cli2.sendLine("/subsystem=elytron/filesystem-realm=exampleFsRealm:add-identity-attribute(identity=jane, name=Roles, value=[\"Admin\"])");
        cli2.sendLine("/subsystem=elytron/configurable-http-server-mechanism-factory=configured-http:add(http-server-mechanism-factory=global,properties={org.wildfly.security.http.use-session-based-digest-nonce-manager=true})");
        cli2.sendLine("/subsystem=elytron/http-authentication-factory=application-http-authentication:write-attribute(name=http-server-mechanism-factory,value=configured-http)");
        cli2.sendLine("/subsystem=elytron/http-authentication-factory=application-http-authentication:write-attribute(name=mechanism-configurations,value=[{mechanism-name=DIGEST,mechanism-realm-configurations=[{realm-name=exampleFsRealm}]}])");
        cli2.sendLine("batch");
        cli2.sendLine("/subsystem=elytron/security-domain=ApplicationDomain:write-attribute(name=realms,value=[{realm=exampleFsRealm}])");
        cli2.sendLine("/subsystem=elytron/security-domain=ApplicationDomain:write-attribute(name=default-realm,value=exampleFsRealm)");
        cli2.sendLine("/subsystem=undertow/application-security-domain=other:write-attribute(name=http-authentication-factory,value=application-http-authentication)");
        cli2.sendLine("/subsystem=undertow/application-security-domain=other:undefine-attribute(name=security-domain)");
        cli2.sendLine("run-batch");
        cli2.sendLine("reload");
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testHttpSessionDigestPropertyWithTwoServers() throws Exception {
        testDigestAuthenticationForTwoServers();
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testHttpSessionDigestSameNonceCannotBeUsedTwice() throws Exception {
        String server1 = "http://localhost:8180/test/";
        String server2 = "http://localhost:8280/test/";

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpFirstGetRequest = new HttpGet(server1);
            HttpResponse response = httpclient.execute(httpFirstGetRequest);
            Map<String, String> wwwAuth = Arrays.stream(response.getHeaders("WWW-Authenticate")[0].getElements())
                    .collect(Collectors.toMap(HeaderElement::getName, HeaderElement::getValue));
            String realm = wwwAuth.get("Digest realm");
            String nonce = wwwAuth.get("nonce");
            String uri = "/test/";

            // the first call always fails with a 401 and a requested nonce, realm, etc.
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 401);
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
        String server1 = "http://localhost:8180/test/";
        String server2 = "http://localhost:8280/test/";

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpFirstGetRequest = new HttpGet(server1);
            HttpResponse response = httpclient.execute(httpFirstGetRequest);
            Map<String, String> wwwAuth = Arrays.stream(response.getHeaders("WWW-Authenticate")[0].getElements())
                    .collect(Collectors.toMap(HeaderElement::getName, HeaderElement::getValue));
            String realm = wwwAuth.get("Digest realm");
            String nonce = wwwAuth.get("nonce");
            String uri = "/test/";

            // the first call always fails with a 401 and a requested nonce, realm, etc.
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 401);
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
                "username=" + "\"jane\",\n" +
                "realm=\"" + realm + "\",\n" +
                "nonce=\"" + nonce + "\",\n" +
                "uri=\"" + uri + "\",\n" +
                "algorithm=\"" + "MD5" + "\",\n" +
                "response=\"" + computeDigest("/test/", nonce, "jane", "passwordJane", "MD5", realm, "GET") +
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
}
