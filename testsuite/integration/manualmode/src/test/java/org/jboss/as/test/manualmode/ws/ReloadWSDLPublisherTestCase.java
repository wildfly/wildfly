/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.manualmode.ws;

import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import org.jboss.logging.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReloadWSDLPublisherTestCase {

    private static final String DEFAULT_JBOSSAS = "default-jbossas";
    private static final String DEPLOYMENT = "jaxws-manual-pojo";
    private static final String keepAlive = System.getProperty("http.keepAlive") == null ? "true" : System.getProperty("http.keepAlive");
    private static final String maxConnections = System.getProperty("http.maxConnections") == null ? "5" : System.getProperty("http.maxConnections");

    @ArquillianResource
    ContainerController containerController;

    @ArquillianResource
    Deployer deployer;

    @Deployment(name = DEPLOYMENT, testable = false, managed = false)
    public static WebArchive deployment() {
        WebArchive pojoWar = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war").addClasses(
                EndpointIface.class, PojoEndpoint.class);
        return pojoWar;
    }

    @Before
    public void endpointLookup() throws Exception {
        containerController.start(DEFAULT_JBOSSAS);
        if (containerController.isStarted(DEFAULT_JBOSSAS)) {
            deployer.deploy(DEPLOYMENT);
        }
        System.setProperty("http.keepAlive", "false");
        System.setProperty("http.maxConnections", "1");
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT)
    public void testHelloStringAfterReload() throws Exception {
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
        ManagementClient managementClient = new ManagementClient(TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.getServerAddress(), TestSuiteEnvironment.getServerPort(), "remote+http");
        QName serviceName = new QName("http://jbossws.org/basic", "POJOService");
        URL wsdlURL = new URL(managementClient.getWebUri().toURL(), '/' + DEPLOYMENT + "/POJOService?wsdl");
        checkWsdl(wsdlURL);
        Service service = Service.create(wsdlURL, serviceName);
        EndpointIface proxy = service.getPort(EndpointIface.class);
        Assert.assertEquals("Hello World!", proxy.helloString("World"));
        executeReloadAndWaitForCompletion(managementClient.getControllerClient(), 100000);
        checkWsdl(wsdlURL);
        serviceName = new QName("http://jbossws.org/basic", "POJOService");
        service = Service.create(wsdlURL, serviceName);
        proxy = service.getPort(EndpointIface.class);
        Assert.assertEquals("Hello World!", proxy.helloString("World"));
        Assert.assertTrue(containerController.isStarted(DEFAULT_JBOSSAS));
    }

    @After
    public void stopContainer() {
        System.setProperty("http.keepAlive", keepAlive);
        System.setProperty("http.maxConnections", maxConnections);
        if (containerController.isStarted(DEFAULT_JBOSSAS)) {
            deployer.undeploy(DEPLOYMENT);
        }
        if (containerController.isStarted(DEFAULT_JBOSSAS)) {
            containerController.stop(DEFAULT_JBOSSAS);
        }
    }

    private void checkWsdl(URL wsdlURL) throws IOException {
        StringBuilder proxyUsed = new StringBuilder();
        try {
            List<Proxy> proxies = ProxySelector.getDefault().select(wsdlURL.toURI());
            for(Proxy proxy : proxies) {
                //System.out.println("To connect to " + wsdlURL + " we are using proxy " + proxy);
                proxyUsed.append("To connect to ").append(wsdlURL).append(" we are using proxy ").append(proxy).append("\r\n");
            }
        } catch (URISyntaxException ex) {
            Logger.getLogger(ReloadWSDLPublisherTestCase.class.getName()).error(ex);
        }
        HttpURLConnection connection = (HttpURLConnection) wsdlURL.openConnection();
        try {
            connection.connect();
            Assert.assertEquals(proxyUsed.toString(), HttpServletResponse.SC_OK, connection.getResponseCode());
        } finally {
            connection.disconnect();
        }
    }
}
