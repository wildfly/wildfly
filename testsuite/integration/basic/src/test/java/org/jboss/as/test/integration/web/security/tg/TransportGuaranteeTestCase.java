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

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.junit.*;
import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.Connector;
import org.jboss.as.test.integration.management.ServerManager;
import org.jboss.as.test.integration.web.security.SecurityTest;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.URL;



/**
 * This test case check if transport-guarantee security constraint works properly.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */

@RunWith(Arquillian.class)
@RunAsClient
public class TransportGuaranteeTestCase {


    private static final Logger log = Logger.getLogger(TransportGuaranteeTestCase.class);

    private static final String WAR = ".war";
    private static final String TG_ANN = "tg-annotated";
    private static final String TG_DD = "tg-dd";
    private static final String TG_MIXED = "tg-mixed";

    @ArquillianResource @OperateOnDeployment(TG_ANN + WAR)
    URL deploymentUrl;

    private ServerManager serverManager = null;

    private final File keyStoreFile = new File(System.getProperty("java.io.tmpdir"), "tg-test.keystore");

    private static final int httpsPort = 8447;
    private String httpsTestURL = null;
    private String httpTestURL = null;

    private boolean beforeServerManagerInitialized = false;

    @Deployment(name = TG_ANN + WAR, order = 1, testable = false)
    public static WebArchive deployAnnWar() {

        log.info("starting deployAnnWar()");

        // FIXME hack to get things prepared before the deployment happens
        try {
            // create required security domains
            log.info("createSecurityDomain");
            SecurityTest.createSecurityDomain();
            log.info("Done");

        } catch (Exception e) {
            // ignore
            log.error(e);
        }


        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        WebArchive war = ShrinkWrap.create(WebArchive.class, TG_ANN + WAR);
        war.addClass(TransportGuaranteeAnnotatedServlet.class);

        war.addAsResource(tccl.getResource("web/sec/tg/users.properties"), "users.properties");
        war.addAsResource(tccl.getResource("web/sec/tg/roles.properties"), "roles.properties");
        war.setWebXML(tccl.getResource("web/sec/tg/annotated/web.xml"));
        war.addAsWebInfResource("web/sec/tg/jboss-web.xml", "jboss-web.xml");

        log.info(war.toString());
        return war;
    }


    @Deployment(name = TG_DD + WAR, order = 2, testable = false)
    public static WebArchive deployDdWar() {

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        WebArchive war = ShrinkWrap.create(WebArchive.class, TG_DD + WAR);
        war.addClass(TransportGuaranteeServlet.class);

        war.addAsResource(tccl.getResource("web/sec/tg/users.properties"), "users.properties");
        war.addAsResource(tccl.getResource("web/sec/tg/roles.properties"), "roles.properties");

        war.setWebXML(tccl.getResource("web/sec/tg/dd/web.xml"));
        war.addAsWebInfResource("web/sec/tg/jboss-web.xml", "jboss-web.xml");

        log.info(war.toString());
        return war;
    }

    @Deployment(name = TG_MIXED + WAR, order = 3, testable = false)
    public static WebArchive deployMixedWar() {

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        WebArchive war = ShrinkWrap.create(WebArchive.class, TG_MIXED + WAR);
        war.addClass(TransportGuaranteeMixedServlet.class);

        war.addAsResource(tccl.getResource("web/sec/tg/users.properties"), "users.properties");
        war.addAsResource(tccl.getResource("web/sec/tg/roles.properties"), "roles.properties");

        war.setWebXML(tccl.getResource("web/sec/tg/mixed/web.xml"));
        war.addAsWebInfResource("web/sec/tg/jboss-web.xml", "jboss-web.xml");

        log.info(war.toString());
        return war;
    }


    @Before
    public void before() throws IOException {

        if (beforeServerManagerInitialized)
            return;

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        FileUtils.copyURLToFile(tccl.getResource("web/sec/tg/localhost.keystore"), keyStoreFile);

        serverManager = new ServerManager(deploymentUrl.getHost());
        serverManager.initModelControllerClient();

        try {
            serverManager.addConnector(Connector.HTTPSJIO, httpsPort,
                null, null, keyStoreFile.getAbsolutePath(),
                "password");
        }
        catch (Exception e) {
            Assert.fail("Cannot create https connector - HTTPSJIO");
        }

        // set test URL
        httpsTestURL = "https://" + deploymentUrl.getHost() + ":" + Integer.toString(httpsPort);
        httpTestURL = "http://" + deploymentUrl.getHost() + ":" + deploymentUrl.getPort();
        
    }



    @After
    public void tidyUpConfiguration() throws Exception {
        log.info("begin tidy up");
        serverManager.removeConnector(Connector.HTTPSJIO, httpsTestURL);
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

        log.info("Checking URL="+url);

        HttpClient httpClient;
        if (url.startsWith("https")) {
            httpClient = HttpClientUtils.wrapHttpsClient(new DefaultHttpClient());
        }
        else {
            httpClient = new DefaultHttpClient();
        }

        ((DefaultHttpClient)httpClient).getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
              new UsernamePasswordCredentials(user, pass));

        HttpGet get = new HttpGet(url);
        HttpResponse hr;
        try {
            hr = httpClient.execute(get);
        }
        catch (Exception e) {
            if (responseSubstring == null)
                return false;
            else // in case substring is defined, rethrow exception so, we can easier analyze the cause
                throw new Exception(e);
        }

        int statusCode = hr.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            log.info("statusCode not expected. statusCode="+statusCode+", URL="+url);
            return false;
        }

        if (responseSubstring == null) {
            // this indicates that negative test had problems
            log.info("statusCode==200 on URL="+url);
            return true;
        }

        String response = EntityUtils.toString(hr.getEntity());
        if (response.indexOf(responseSubstring) != -1) {
            return true;
        }
        else {
            log.info("Response doesn't contain expected substring ("+responseSubstring+")");
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
              + "/tg_mixed_override/srv" ;

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


}
