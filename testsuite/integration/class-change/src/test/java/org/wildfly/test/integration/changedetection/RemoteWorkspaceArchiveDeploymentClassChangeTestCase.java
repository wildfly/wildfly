/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.changedetection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.util.AbstractClassReplacer;
import org.wildfly.test.integration.util.AbstractWorkspaceReplacement;
import org.wildfly.test.integration.util.ProtocolClassReplacer;
import org.wildfly.test.integration.util.TestProtocol;

import io.undertow.util.StatusCodes;

/**
 * Tests that replacement works for archives when using the remote websocket protocol
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteWorkspaceArchiveDeploymentClassChangeTestCase {

    private static final String WEB_FILE_TXT = "web-file.txt";
    private static final String CLASS = "class";
    private static final String JAVA = "java";
    private static final String REMOTE_PASSWORD = "remote.password";
    private static final String PASSWORD = "test";


    @Deployment(name = CLASS)
    public static WebArchive classDeployment() {
        return baseDeployment(CLASS + ".war");
    }

    @Deployment(name = JAVA)
    public static WebArchive javaDeployment() {
        return baseDeployment(JAVA + ".war")
                .addAsWebResource(new StringAsset("data"), "somefile.txt");
    }

    private static WebArchive baseDeployment(String name) {
        return ShrinkWrap.create(WebArchive.class, name)
                .addClass(TestServlet.class)
                .addAsWebInfResource(new StringAsset(REMOTE_PASSWORD + "=" + PASSWORD), "class-change.properties")
                .addAsWebResource(RemoteWorkspaceArchiveDeploymentClassChangeTestCase.class.getPackage(), WEB_FILE_TXT, WEB_FILE_TXT);
    }

    public void runTest(String path, AbstractWorkspaceReplacement replacement, TestProtocol protocol) throws Exception {

        String deploymentUrl = "http://" + TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort() + "/" + path + "/";


        try (ModelControllerClient mc = TestSuiteEnvironment.getModelControllerClient()) {
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet(deploymentUrl + WEB_FILE_TXT);
                CloseableHttpResponse response = client.execute(get);
                String test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Web File", test);
                protocol.addChangedWebResource(WEB_FILE_TXT, getResource("web-file1.txt"));
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Replaced Web File", test);

                get = new HttpGet(deploymentUrl + PASSWORD);
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Original", test);
                String uuid = response.getFirstHeader("uuid").getValue();
                replacement.queueClassReplacement(TestServlet.class, TestServlet1.class);
                replacement.queueAddClass(MessageClass.class);
                replacement.doReplacement();
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Replaced", test);
                Assert.assertEquals(uuid, response.getFirstHeader("uuid").getValue());

                get = new HttpGet(deploymentUrl + "dir/new-file.txt");
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals(StatusCodes.NOT_FOUND, response.getStatusLine().getStatusCode());
                protocol.addChangedWebResource("dir/new-file.txt", getResource("dir/new-file.txt"));
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("New File", test);
                replacement.close();
                ServerReload.executeReloadAndWaitForCompletion(mc);
                //make sure changes persist after reload

                get = new HttpGet(deploymentUrl + WEB_FILE_TXT);
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Replaced Web File", test);

                get = new HttpGet(deploymentUrl + PASSWORD);
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Replaced", test);

                get = new HttpGet(deploymentUrl + "dir/new-file.txt");
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("New File", test);

            }
        }
    }

    @Test
    public void testClassFileReplacement() throws Exception {
        TestProtocol protocol = createConnection(CLASS);
        AbstractClassReplacer cr = new ProtocolClassReplacer(protocol);
        AbstractWorkspaceReplacement replacement = new AbstractWorkspaceReplacement(RemoteWorkspaceArchiveDeploymentClassChangeTestCase.class, Collections.singletonList(WEB_FILE_TXT), Collections.singletonList(TestServlet.class)) {
            @Override
            public void queueClassReplacement(Class<?> original, Class<?> replacement) {
                cr.queueClassForReplacement(original, replacement);
            }

            @Override
            public void queueAddClass(Class<?> theClass) {
                cr.addNewClass(theClass);
            }

            @Override
            public void doReplacement() {
                cr.replaceQueuedClasses();
            }
        };
        try {
            runTest(CLASS, replacement, protocol);
        } finally {
            replacement.close();
        }
    }

    @Test
    public void testJavaFileReplacement() throws Exception {
        TestProtocol protocol = createConnection(JAVA);
        JavaFileWorkspaceReplacementStrategy strategy = new JavaFileWorkspaceReplacementStrategy(RemoteWorkspaceArchiveDeploymentClassChangeTestCase.class, Collections.singletonList(WEB_FILE_TXT), Collections.singletonList(TestServlet.class)) {

            @Override
            public void doReplacement() {
            }

            @Override
            protected void doReplacement(Class<?> original, byte[] contentsBytes) throws IOException {
                protocol.addChangedSrc(original.getName().replace('.', '/') + ".java", contentsBytes);
            }

            @Override
            protected void doAdd(Class<?> theClass, byte[] bytes) throws IOException {
                protocol.addChangedSrc(theClass.getName().replace('.', '/') + ".java", bytes);
            }
        };
        try {
            runTest(JAVA, strategy, protocol);
        } finally {
            strategy.close();
        }
    }

    TestProtocol createConnection(String path) throws URISyntaxException, IOException, DeploymentException {

        String deploymentUrl = "http://" + TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort() + "/" + path + "/";
        TestProtocol protocol = new TestProtocol();
        ContainerProvider.getWebSocketContainer().connectToServer(protocol, ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                headers.put(REMOTE_PASSWORD, Collections.singletonList(PASSWORD));
            }
        }).build(), new URI(deploymentUrl));
        return protocol;
    }

    static byte[] getResource(String resource) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream stream = RemoteWorkspaceArchiveDeploymentClassChangeTestCase.class.getResourceAsStream(resource)) {
            byte[] buf = new byte[1024];
            int r;
            while ((r = stream.read(buf)) > 0) {
                outputStream.write(buf, 0, r);
            }
        }
        return outputStream.toByteArray();
    }

}
