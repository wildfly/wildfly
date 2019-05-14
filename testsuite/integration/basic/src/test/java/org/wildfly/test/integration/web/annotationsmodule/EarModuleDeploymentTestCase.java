/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.web.annotationsmodule;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class EarModuleDeploymentTestCase {

    public static final String DEPLOYMENT_1 = "deployment1";
    public static final String DEPLOYMENT_2 = "deployment2";

    @Deployment(name = DEPLOYMENT_1)
    public static EnterpriseArchive deployment() throws Exception {
        EnterpriseArchive enterpriseArchive = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT_1 + ".ear");
        enterpriseArchive
                .addAsModule(webArchive())
                .addAsModule(jarArchive("library.jar", ModuleServlet.class, TestEjb.class))
                .addAsModule(jarArchive("library2.jar", ModuleServlet2.class))
                .addAsManifestResource(EarModuleDeploymentTestCase.class.getResource("application1.xml"), "application.xml")
                .addAsManifestResource(EarModuleDeploymentTestCase.class.getResource("jboss-deployment-structure1.xml"), "jboss-deployment-structure.xml");
        return enterpriseArchive;
    }

    @Deployment(name = DEPLOYMENT_2)
    public static EnterpriseArchive deployment2() throws Exception {
        EnterpriseArchive enterpriseArchive = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT_2 + ".ear");
        enterpriseArchive
                .addAsModule(webArchive())
                .addAsModule(jarArchive("library.jar", ModuleServlet.class, TestEjb.class))
                .addAsManifestResource(EarModuleDeploymentTestCase.class.getResource("application2.xml"), "application.xml")
                .addAsManifestResource(EarModuleDeploymentTestCase.class.getResource("jboss-deployment-structure2.xml"), "jboss-deployment-structure.xml");
        return enterpriseArchive;
    }

    public static WebArchive webArchive() {
        return ShrinkWrap.create(WebArchive.class, "web.war")
                .addClass(TestServlet.class)
                .addAsWebInfResource(new StringAsset(""), "beans.xml");
    }

    public static JavaArchive jarArchive(String name, Class<?>... classes) throws Exception {
        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class, name)
                .addAsResource(new StringAsset("<ejb-jar version=\"3.0\" metadata-complete=\"true\"></ejb-jar>"), "META-INF/ejb-jar.xml");

        Indexer indexer = new Indexer();
        for (Class<?> aClass : classes) {
            javaArchive.addClass(aClass);
            try (InputStream resource = aClass.getResourceAsStream(aClass.getSimpleName() + ".class")) {
                indexer.index(resource);
            }
        }

        Index index = indexer.complete();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        IndexWriter writer = new IndexWriter(data);
        writer.write(index);
        javaArchive.addAsManifestResource(new ByteArrayAsset(data.toByteArray()), "jandex.idx");

        return javaArchive;
    }

    @ArquillianResource
    private URL url;

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    @RunAsClient
    public void testServlet() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "/servlet");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            assertEquals(ModuleServlet.MODULE_SERVLET, result);
        }
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    @RunAsClient
    public void testServletInAdditinalJar() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "/servlet2");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            assertEquals(ModuleServlet2.MODULE_SERVLET, result);
        }
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    @RunAsClient
    public void testEjbScan() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "/test");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            assertEquals(TestEjb.TEST_EJB, result);
        }
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_2)
    @RunAsClient
    public void testServletInDeployment() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "/servlet");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            assertEquals(ModuleServlet.MODULE_SERVLET, result);
        }
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_2)
    @RunAsClient
    public void testEjbScanInSubdeployment() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpget = new HttpGet(url.toExternalForm() + "/test");

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();

            StatusLine statusLine = response.getStatusLine();
            assertEquals(200, statusLine.getStatusCode());

            String result = EntityUtils.toString(entity);
            assertEquals(TestEjb.TEST_EJB, result);
        }
    }
}
