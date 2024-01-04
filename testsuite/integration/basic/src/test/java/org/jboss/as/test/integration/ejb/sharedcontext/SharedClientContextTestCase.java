/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.sharedcontext;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Daniel Cihak
 *
 * https://issues.redhat.com/browse/WFLY-18040
 *
 * Tests if deployments can share client context if static interceptors are used.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SharedClientContextTestCase extends AbstractCliTestBase {

    private static final String EJB = "shared-client-context-ejb";
    private static final String FRONTEND = "shared-client-context-frontend";
    private static final String CLIENT_NAME_BASE = "shared-client-context-client-%02d";
    private static final int CLIENTS_NUMBER = 50;
    private static List<File> clientWarFiles = new ArrayList<>();

    @ArquillianResource
    @OperateOnDeployment(FRONTEND)
    private URL frontendUrl;

    @Deployment(name = EJB)
    public static WebArchive deployment1() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, EJB + ".war");
        archive.addClasses(TestEjbRemote.class, TestEjb.class);
        return archive;
    }

    @Deployment(name = FRONTEND)
    public static WebArchive deployment3() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, FRONTEND + ".war");
        archive.addClasses(FrontendServlet.class);
        return archive;
    }

    /**
     * Creates list of 50 clients trying to lookup remote bean from the other deployment
     *
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        File clientWarFile = null;
        for (int i = 0; i < CLIENTS_NUMBER; i++){
            String clientWarFileName = String.format(CLIENT_NAME_BASE, i);
            clientWarFile = exportClientWar(clientWarFileName + ".war");
            clientWarFiles.add(clientWarFile);
        }

        AbstractCliTestBase.initCLI();
    }

    private static File exportClientWar(String name) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, name + ".war");

        war.addClasses(EjbClientServlet.class, TestEjbRemote.class, TestSuiteEnvironment.class);
        String tempDir = TestSuiteEnvironment.getTmpDir();
        File warFile = new File(tempDir + File.separator + name);
        new ZipExporterImpl(war).exportTo(warFile, true);
        return warFile;
    }

    @Before
    public void before() throws MalformedURLException {
        for (int i = 0; i < clientWarFiles.size(); i++) {
            String clientWarFileName = String.format(CLIENT_NAME_BASE, i);
            cli.sendLine("deploy --url=" + clientWarFiles.get(i).toURI().toURL().toExternalForm() + " --name=" + clientWarFileName + ".war --runtime-name=" + clientWarFileName + ".war");
        }
    }

    @AfterClass
    public static void closeCli() throws Exception {
        for (int i = 0; i < clientWarFiles.size(); i++) {
            String clientWarFileName = String.format(CLIENT_NAME_BASE, i);
            cli.sendLine("undeploy --name=" + clientWarFileName + ".war");
        }

        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void testSharedClientContext() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = frontendUrl.toExternalForm() + "frontend";
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpClient.execute(httpget);
            assertEquals("FrontEndServlet could not process the shared client context or EJB lookup failed.", FrontendServlet.FRONTEND_SERVLET_OK, EntityUtils.toString(response.getEntity()));
        }
    }
}
