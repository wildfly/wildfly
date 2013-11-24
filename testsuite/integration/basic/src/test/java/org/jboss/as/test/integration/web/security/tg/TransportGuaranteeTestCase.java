/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.web.security.tg;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.ws.rs.HEAD;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.jboss.as.test.integration.management.Listener;
import org.jboss.as.test.integration.management.ServerManager;
import org.jboss.as.test.integration.web.security.WebTestsSecurityDomainSetup;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;


/**
 * This test case check if transport-guarantee security constraint works properly.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({WebTestsSecurityDomainSetup.class, TransportGuaranteeTestCase.ListenerSetup.class})
@Category(CommonCriteria.class)
public class TransportGuaranteeTestCase {


    private static final Logger log = Logger.getLogger(TransportGuaranteeTestCase.class);
    private static final String WAR = ".war";
    private static final String TG_ANN = "tg-annotated";
    private static final String TG_DD = "tg-dd";
    private static final String TG_MIXED = "tg-mixed";
    private static final File keyStoreFile = new File(System.getProperty("java.io.tmpdir"), "tg-test.keystore");
    private static final int httpsPort = 8447;
    private static String httpsTestURL = null;
    private static String httpTestURL = null;
    @ArquillianResource
    @OperateOnDeployment(TG_ANN + WAR)
    URL deploymentUrl;
    /*@ArquillianResource
    @OperateOnDeployment(TG_ANN + WAR)
    ManagementClient managementClient;*/
    //private boolean beforeServerManagerInitialized = false;

    @Deployment(name = TG_ANN + WAR, order = 1, testable = false)
    public static WebArchive deployAnnWar() throws Exception {

        log.info("starting deployAnnWar()");

        WebArchive war = ShrinkWrap.create(WebArchive.class, TG_ANN + WAR);
        war.addClass(TransportGuaranteeAnnotatedServlet.class);

        war.addAsResource(TransportGuaranteeTestCase.class.getPackage(), "users.properties", "users.properties");
        war.addAsResource(TransportGuaranteeTestCase.class.getPackage(), "roles.properties", "roles.properties");
        war.setWebXML(TransportGuaranteeTestCase.class.getPackage(), "annotated-web.xml");
        war.addAsWebInfResource(TransportGuaranteeTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");

        log.info(war.toString());
        return war;
    }

    @Deployment(name = TG_DD + WAR, order = 2, testable = false)
    public static WebArchive deployDdWar() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, TG_DD + WAR);
        war.addClass(TransportGuaranteeServlet.class);

        war.addAsResource(TransportGuaranteeTestCase.class.getPackage(), "users.properties", "users.properties");
        war.addAsResource(TransportGuaranteeTestCase.class.getPackage(), "roles.properties", "roles.properties");
        war.setWebXML(TransportGuaranteeTestCase.class.getPackage(), "dd-web.xml");
        war.addAsWebInfResource(TransportGuaranteeTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");

        log.info(war.toString());
        return war;
    }

    @Deployment(name = TG_MIXED + WAR, order = 3, testable = false)
    public static WebArchive deployMixedWar() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, TG_MIXED + WAR);
        war.addClass(TransportGuaranteeMixedServlet.class);

        war.addAsResource(TransportGuaranteeTestCase.class.getPackage(), "users.properties", "users.properties");
        war.addAsResource(TransportGuaranteeTestCase.class.getPackage(), "roles.properties", "roles.properties");

        war.setWebXML(TransportGuaranteeTestCase.class.getPackage(), "mixed-web.xml");
        war.addAsWebInfResource(TransportGuaranteeTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");

        log.info(war.toString());
        return war;
    }

    @Before
    public void before() throws IOException {
        // set test URL
        httpsTestURL = "https://" + deploymentUrl.getHost() + ":" + Integer.toString(httpsPort);
        httpTestURL = "http://" + deploymentUrl.getHost() + ":" + deploymentUrl.getPort();
    }

    @AfterClass
    public static void after()throws IOException{
        if (keyStoreFile.exists()){
            keyStoreFile.delete();
        }
    }

    /**
     * Check response on given url
     *
     * @param url
     * @param responseSubstring - if null we are checking response code only
     * @return
     * @throws Exception
     */
    private boolean checkGetURL(String url, String responseSubstring, String user, String pass) throws Exception {

        log.info("Checking URL=" + url);

        HttpClient httpClient;
        if (url.startsWith("https")) {
            httpClient = HttpClientUtils.wrapHttpsClient(new DefaultHttpClient());
        } else {
            httpClient = new DefaultHttpClient();
        }

        ((DefaultHttpClient) httpClient).getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                new UsernamePasswordCredentials(user, pass));

        HttpGet get = new HttpGet(url);
        HttpResponse hr;
        try {
            hr = httpClient.execute(get);
        } catch (Exception e) {
            if (responseSubstring == null)
                return false;
            else // in case substring is defined, rethrow exception so, we can easier analyze the cause
                throw new Exception(e);
        }

        int statusCode = hr.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            log.info("statusCode not expected. statusCode=" + statusCode + ", URL=" + url);
            return false;
        }

        if (responseSubstring == null) {
            // this indicates that negative test had problems
            log.info("statusCode==200 on URL=" + url);
            return true;
        }

        String response = EntityUtils.toString(hr.getEntity());
        if (response.indexOf(responseSubstring) != -1) {
            return true;
        } else {
            log.info("Response doesn't contain expected substring (" + responseSubstring + ")");
            return false;
        }

    }

    @Test
    public void testTransportGuaranteedAnnotation() throws Exception {

        String testURLContext = "/" + TG_ANN + TransportGuaranteeAnnotatedServlet.servletContext;

        boolean result = checkGetURL(
                httpsTestURL + testURLContext,
                "TransportGuaranteedGet",
                "anil",
                "anil");
        Assert.assertTrue("Not expected response", result);


        result = checkGetURL(
                httpTestURL + testURLContext,
                null,
                "anil",
                "anil");
        Assert.assertFalse("Non secure transport on URL has to be prevented, but was not", result);

    }

    @Test
    public void testTransportGuaranteedDD() throws Exception {

        String testURLContext = "/" + TG_DD + TransportGuaranteeServlet.servletContext;

        boolean result = checkGetURL(
                httpsTestURL + testURLContext,
                "TransportGuaranteedGet",
                "anil",
                "anil");
        Assert.assertTrue("Not expected response", result);


        result = checkGetURL(
                httpTestURL + testURLContext,
                null,
                "anil",
                "anil");
        Assert.assertFalse("Non secure transport on URL has to be prevented, but was not", result);


    }

    @Test
    public void testTransportGuaranteedMixed() throws Exception {

        String testURLContext = "/" + TG_MIXED
                + "/tg_mixed_override/srv";

        boolean result = checkGetURL(
                httpsTestURL + testURLContext,
                "TransportGuaranteedGet",
                "anil",
                "anil");
        Assert.assertTrue("Not expected response", result);


        result = checkGetURL(
                httpTestURL + testURLContext,
                null,
                "anil",
                "anil");
        Assert.assertFalse("Non secure transport on URL has to be prevented, but was not", result);


    }

    static class ListenerSetup implements ServerSetupTask {
        private ServerManager serverManager;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            /*if (beforeServerManagerInitialized)
                        return;
                    beforeServerManagerInitialized = true;*/
            serverManager = new ServerManager(managementClient);

            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            FileUtils.copyURLToFile(TransportGuaranteeTestCase.class.getResource("localhost.keystore"), keyStoreFile);
            try {
                serverManager.addListener(Listener.HTTPSJIO, httpsPort, null, null, keyStoreFile.getAbsolutePath(), "password");
            } catch (Exception e) {
                log.error("Cannot create https connector - HTTPSJIO", e);
                Assert.fail("Cannot create https connector - HTTPSJIO, cause " + e.getMessage());
            }


        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            log.info("begin tidy up");
            serverManager.removeListener(Listener.HTTPSJIO, httpsTestURL);
        }
    }

}
