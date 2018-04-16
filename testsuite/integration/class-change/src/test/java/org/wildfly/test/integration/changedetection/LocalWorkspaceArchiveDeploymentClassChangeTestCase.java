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

import java.util.Collections;

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
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.util.AbstractWorkspaceReplacement;

import io.undertow.util.StatusCodes;

/**
 * Tests that class and resource replacement works when deployment is an archive,
 * and workspace is local
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LocalWorkspaceArchiveDeploymentClassChangeTestCase {

    private static final String WEB_FILE_TXT = "web-file.txt";
    private static final String CLASS = "class";
    private static final String JAVA = "java";


    private static JavaFileLocalWorkspaceReplacementStrategy javaFileReplacement = new JavaFileLocalWorkspaceReplacementStrategy(
            LocalWorkspaceArchiveDeploymentClassChangeTestCase.class, Collections.singletonList(WEB_FILE_TXT), Collections.singletonList(TestServlet.class));

    private static JavaFileLocalWorkspaceReplacementStrategy classFileReplacement = new JavaFileLocalWorkspaceReplacementStrategy(
            LocalWorkspaceArchiveDeploymentClassChangeTestCase.class, Collections.singletonList(WEB_FILE_TXT), Collections.singletonList(TestServlet.class));

    @Deployment(name = CLASS)
    public static WebArchive classDeployment() {
        return baseDeployment(CLASS + ".war")
                .addAsWebInfResource(new ByteArrayAsset(classFileReplacement.getClassChangeProps()), "class-change.properties");
    }

    @Deployment(name = JAVA)
    public static WebArchive javaDeployment() {
        return baseDeployment(JAVA + ".war")
                .addAsWebInfResource(new ByteArrayAsset(javaFileReplacement.getClassChangeProps()), "class-change.properties");
    }

    private static WebArchive baseDeployment(String name) {
        return ShrinkWrap.create(WebArchive.class, name)
                .addClass(TestServlet.class)
                .addAsWebResource(LocalWorkspaceArchiveDeploymentClassChangeTestCase.class.getPackage(), WEB_FILE_TXT, WEB_FILE_TXT);
    }

    public void runTest(String path, AbstractWorkspaceReplacement replacement) throws Exception {
        Thread.sleep(10001); //nessesary for class change detection to work properly, as some file systems resolve last modified time in seconds
        String deploymentUrl = "http://" + TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort() + "/" + path + "/";

        try (ModelControllerClient mc = TestSuiteEnvironment.getModelControllerClient()) {
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet(deploymentUrl + WEB_FILE_TXT);
                CloseableHttpResponse response = client.execute(get);
                String test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Web File", test);
                replacement.replaceWebResource(WEB_FILE_TXT, "web-file1.txt");
                response = client.execute(get);
                test = EntityUtils.toString(response.getEntity());
                Assert.assertEquals("Replaced Web File", test);

                get = new HttpGet(deploymentUrl + "test");
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
                replacement.addWebResource("dir/new-file.txt");
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

                get = new HttpGet(deploymentUrl + "test");
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
        runTest(CLASS, classFileReplacement);
    }

    @Test
    public void testJavaFileReplacement() throws Exception {
        runTest(JAVA, javaFileReplacement);
    }
}
