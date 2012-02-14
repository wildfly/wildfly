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

package org.jboss.as.test.integration.web.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.AUTHENTICATION;
import static org.jboss.as.security.Constants.CODE;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.JSSE;
import static org.jboss.as.security.Constants.MODULE_OPTIONS;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;
import static org.jboss.as.security.Constants.TRUSTSTORE_PASSWORD;
import static org.jboss.as.security.Constants.TRUSTSTORE_URL;
import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.security.JBossJSSESecurityDomain;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit test for CLIENT-CERT authentication.
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@Ignore("[AS7-734] Migrate to ARQ Beta1")
public class WebSecurityCERTTestCase {

    protected final String URL = "https://localhost:8380/" + getContextPath() + "/secured/";

    /**
     * Base method to create a {@link WebArchive}
     *
     * @param name Name of the war file
     * @param servletClass a class that is the servlet
     * @param webxml {@link URL} to the web.xml. This can be null
     * @return  the web archive
     */
    public static WebArchive create(String name, Class<?> servletClass, URL webxml) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, name);
        war.addClass(servletClass);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        war.addAsResource(tccl.getResource("web-secure-client-cert.war/roles.properties"), "roles.properties");
        war.addAsWebInfResource(tccl.getResource("web-secure-client-cert.war/jboss-web.xml"), "jboss-web.xml");

        if (webxml != null) {
            war.setWebXML(webxml);
        }

        return war;
    }

    @Deployment
    public static WebArchive deployment() {
        // FIXME hack to get things prepared before the deployment happens
        try {
            final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            // create the test connector
            createTestConnector(client);
            // create required security domains
            createSecurityDomains(client);
        } catch (Exception e) {
            // ignore
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL webxml = tccl.getResource("web-secure-client-cert.war/web.xml");
        WebArchive war = create("web-secure-client-cert.war", SecuredServlet.class, webxml);
        WebSecurityPasswordBasedBase.printWar(war);
        return war;
    }

    @AfterClass
    public static void after() throws Exception {
        final ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        // and remove the connector again
        removeTestConnector(client);
        // remove test security domains
        removeSecurityDomains(client);
    }

    @Test
    public void testClientCertSuccessfulAuth() throws Exception {
        makeCall("test", 200);
    }

    @Test
    public void testClientCertUnsuccessfulAuth() throws Exception {
        makeCall("test2", 403);
    }

    protected void makeCall(String alias, int expectedStatusCode) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient = wrapClient(httpclient, alias);
        try {
            HttpGet httpget = new HttpGet(URL);

            HttpResponse response = httpclient.execute(httpget);

            StatusLine statusLine = response.getStatusLine();
            System.out.println("Response: " + statusLine);
            assertEquals(expectedStatusCode, statusLine.getStatusCode());
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }
    }

    public static void createTestConnector(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add("socket-binding-group", "standard-sockets");
        op.get(OP_ADDR).add("socket-binding", "https-test");
        op.get("interface").set("default");
        op.get("port").set(8380);

        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add("connector", "testConnector");
        op.get("socket-binding").set("https-test");
        op.get("enabled").set(true);
        op.get("protocol").set("HTTP/1.1");
        op.get("scheme").set("https");
        op.get("secure").set(true);
        ModelNode ssl = new ModelNode();
        ssl.setEmptyObject();
        ssl.get("name").set("https-test");
        ssl.get("key-alias").set("test");
        ssl.get("password").set("changeit");
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL keystore = tccl.getResource("security/server.keystore");
        ssl.get("certificate-key-file").set(keystore.getPath());
        ssl.get("protocol").set("TLS");
        ssl.get("verify-client").set(true);
        op.get("ssl").set(ssl);

        updates.add(op);
        applyUpdates(updates, client);
    }

    public static void createSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "cert-test");
        ModelNode loginModule = op.get(AUTHENTICATION).add();
        loginModule.get(CODE).set("CertificateRoles");
        loginModule.get(FLAG).set("required");
        ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        moduleOptions.add("securityDomain", "cert");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "cert");
        ModelNode jsse = op.get(JSSE);
        jsse.get(TRUSTSTORE_PASSWORD).set("changeit");
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL keystore = tccl.getResource("security/jsse.keystore");
        jsse.get(TRUSTSTORE_URL).set(keystore.getPath());
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void removeTestConnector(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add("connector", "testConnector");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add("socket-binding-group", "standard-sockets");
        op.get(OP_ADDR).add("socket-binding", "https-test");
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void removeSecurityDomains(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "cert-test");
        updates.add(op);

        op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "security");
        op.get(OP_ADDR).add(SECURITY_DOMAIN, "cert");
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (ModelNode update : updates) {
            applyUpdate(update, client);
        }
    }

    public static void applyUpdate(ModelNode update, final ModelControllerClient client) throws Exception {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    public String getContextPath() {
        return "web-secure-client-cert";
    }

    public static HttpClient wrapClient(HttpClient base, String alias) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            JBossJSSESecurityDomain jsseSecurityDomain = new JBossJSSESecurityDomain("client-cert");
            jsseSecurityDomain.setKeyStorePassword("changeit");
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            URL keystore = tccl.getResource("security/client.keystore");
            jsseSecurityDomain.setKeyStoreURL(keystore.getPath());
            jsseSecurityDomain.setClientAlias(alias);
            jsseSecurityDomain.reloadKeyAndTrustStore();
            KeyManager[] keyManagers = jsseSecurityDomain.getKeyManagers();
            TrustManager[] trustManagers = jsseSecurityDomain.getTrustManagers();
            ctx.init(keyManagers, trustManagers, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = base.getConnectionManager();
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", 8380, ssf));
            return new DefaultHttpClient(ccm, base.getParams());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
