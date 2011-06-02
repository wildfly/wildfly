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

package org.jboss.as.test.integration.internals.websecurity;

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
@Ignore // Currently failing during AS7-734 Migration
/*
 * javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
    at com.sun.net.ssl.internal.ssl.SSLSessionImpl.getPeerCertificates(SSLSessionImpl.java:352)
    at org.apache.http.conn.ssl.AbstractVerifier.verify(AbstractVerifier.java:128)
    at org.apache.http.conn.ssl.SSLSocketFactory.connectSocket(SSLSocketFactory.java:339)
    at org.apache.http.impl.conn.DefaultClientConnectionOperator.openConnection(DefaultClientConnectionOperator.java:123)
    at org.apache.http.impl.conn.AbstractPoolEntry.open(AbstractPoolEntry.java:147)
    at org.apache.http.impl.conn.AbstractPooledConnAdapter.open(AbstractPooledConnAdapter.java:108)
    at org.apache.http.impl.client.DefaultRequestDirector.execute(DefaultRequestDirector.java:415)
    at org.apache.http.impl.client.AbstractHttpClient.execute(AbstractHttpClient.java:641)
    at org.apache.http.impl.client.AbstractHttpClient.execute(AbstractHttpClient.java:576)
    at org.apache.http.impl.client.AbstractHttpClient.execute(AbstractHttpClient.java:554)
    at org.jboss.as.test.integration.internals.websecurity.WebSecurityCERTTestCase.makeCall(WebSecurityCERTTestCase.java:151)
    at org.jboss.as.test.integration.internals.websecurity.WebSecurityCERTTestCase.testClientCertSucessfulAuth(WebSecurityCERTTestCase.java:137)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
    at java.lang.reflect.Method.invoke(Method.java:597)
    at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:44)
    at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:15)
    at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:41)
    at org.jboss.arquillian.junit.Arquillian$6$1.invoke(Arquillian.java:246)
    at org.jboss.arquillian.container.test.impl.execution.LocalTestExecuter.execute(LocalTestExecuter.java:60)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
    at java.lang.reflect.Method.invoke(Method.java:597)
    at org.jboss.arquillian.core.impl.ObserverImpl.invoke(ObserverImpl.java:90)
    at org.jboss.arquillian.core.impl.EventContextImpl.invokeObservers(EventContextImpl.java:99)
    at org.jboss.arquillian.core.impl.EventContextImpl.proceed(EventContextImpl.java:81)
    at org.jboss.arquillian.core.impl.ManagerImpl.fire(ManagerImpl.java:134)
    at org.jboss.arquillian.core.impl.ManagerImpl.fire(ManagerImpl.java:114)
    at org.jboss.arquillian.core.impl.EventImpl.fire(EventImpl.java:67)
    at org.jboss.arquillian.container.test.impl.execution.ClientTestExecuter.execute(ClientTestExecuter.java:64)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
    at java.lang.reflect.Method.invoke(Method.java:597)
    at org.jboss.arquillian.core.impl.ObserverImpl.invoke(ObserverImpl.java:90)
    at org.jboss.arquillian.core.impl.EventContextImpl.invokeObservers(EventContextImpl.java:99)
    at org.jboss.arquillian.core.impl.EventContextImpl.proceed(EventContextImpl.java:81)
    at org.jboss.arquillian.container.test.impl.client.ContainerEventController.createContext(ContainerEventController.java:130)
    at org.jboss.arquillian.container.test.impl.client.ContainerEventController.createTestContext(ContainerEventController.java:117)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
    at java.lang.reflect.Method.invoke(Method.java:597)
    at org.jboss.arquillian.core.impl.ObserverImpl.invoke(ObserverImpl.java:90)
    at org.jboss.arquillian.core.impl.EventContextImpl.proceed(EventContextImpl.java:88)
    at org.jboss.arquillian.test.impl.TestContextHandler.createTestContext(TestContextHandler.java:82)
    at sun.reflect.GeneratedMethodAccessor3.invoke(Unknown Source)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
    at java.lang.reflect.Method.invoke(Method.java:597)
    at org.jboss.arquillian.core.impl.ObserverImpl.invoke(ObserverImpl.java:90)
    at org.jboss.arquillian.core.impl.EventContextImpl.proceed(EventContextImpl.java:88)
    at org.jboss.arquillian.test.impl.TestContextHandler.createClassContext(TestContextHandler.java:68)
    at sun.reflect.GeneratedMethodAccessor2.invoke(Unknown Source)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
    at java.lang.reflect.Method.invoke(Method.java:597)
    at org.jboss.arquillian.core.impl.ObserverImpl.invoke(ObserverImpl.java:90)
    at org.jboss.arquillian.core.impl.EventContextImpl.proceed(EventContextImpl.java:88)
    at org.jboss.arquillian.test.impl.TestContextHandler.createSuiteContext(TestContextHandler.java:54)
    at sun.reflect.GeneratedMethodAccessor1.invoke(Unknown Source)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
    at java.lang.reflect.Method.invoke(Method.java:597)
    at org.jboss.arquillian.core.impl.ObserverImpl.invoke(ObserverImpl.java:90)
    at org.jboss.arquillian.core.impl.EventContextImpl.proceed(EventContextImpl.java:88)
    at org.jboss.arquillian.core.impl.ManagerImpl.fire(ManagerImpl.java:134)
    at org.jboss.arquillian.test.impl.EventTestRunnerAdaptor.test(EventTestRunnerAdaptor.java:111)
    at org.jboss.arquillian.junit.Arquillian$6.evaluate(Arquillian.java:239)
    at org.jboss.arquillian.junit.Arquillian$4.evaluate(Arquillian.java:202)
    at org.jboss.arquillian.junit.Arquillian.multiExecute(Arquillian.java:290)
    at org.jboss.arquillian.junit.Arquillian.access$100(Arquillian.java:45)
    at org.jboss.arquillian.junit.Arquillian$5.evaluate(Arquillian.java:216)
    at org.junit.runners.BlockJUnit4ClassRunner.runNotIgnored(BlockJUnit4ClassRunner.java:79)
    at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:71)
    at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:49)
    at org.junit.runners.ParentRunner$3.run(ParentRunner.java:193)
    at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:52)
    at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:191)
    at org.junit.runners.ParentRunner.access$000(ParentRunner.java:42)
    at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:184)
    at org.jboss.arquillian.junit.Arquillian$2.evaluate(Arquillian.java:161)
    at org.jboss.arquillian.junit.Arquillian.multiExecute(Arquillian.java:290)
    at org.jboss.arquillian.junit.Arquillian.access$100(Arquillian.java:45)
    at org.jboss.arquillian.junit.Arquillian$3.evaluate(Arquillian.java:175)
    at org.junit.runners.ParentRunner.run(ParentRunner.java:236)
    at org.jboss.arquillian.junit.Arquillian.run(Arquillian.java:123)
    at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:49)
    at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:38)
    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:467)
    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:683)
    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:390)
    at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:197)


 */
@RunWith(Arquillian.class)
@RunAsClient
public class WebSecurityCERTTestCase {

    protected final String URL = "https://localhost:8380/" + getContextPath() + "/secured/";

    /**
     * Base method to create a {@link WebArchive}
     *
     * @param name Name of the war file
     * @param servletClass a class that is the servlet
     * @param webxml {@link URL} to the web.xml. This can be null
     * @return
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
    public void testClientCertSucessfulAuth() throws Exception {
        makeCall("test", 200);
    }

    @Test
    public void testClientCertUnsucessfulAuth() throws Exception {
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
        ModelNode result = client.execute(OperationBuilder.Factory.create(update).build());
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
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = base.getConnectionManager();
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", ssf, 8380));
            return new DefaultHttpClient(ccm, base.getParams());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
