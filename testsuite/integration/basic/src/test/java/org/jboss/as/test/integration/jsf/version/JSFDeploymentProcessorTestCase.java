/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.version;

import java.net.URL;
import java.util.Collection;
import java.util.List;

import jakarta.faces.context.FacesContext;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jsf.version.ejb.JSFVersionEJB;
import org.jboss.as.test.integration.jsf.version.war.JSFVersion;
import org.jboss.as.test.shared.TestLogHandlerSetupTask;
import org.jboss.as.test.shared.logging.LoggingUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests different ways to add Jakarta Server Faces implementation in ear files
 *
 * @author tmiyar
 */
@RunWith(Arquillian.class)
@ServerSetup({JSFDeploymentProcessorTestCase.TestLogHandlerSetup.class})
@RunAsClient
public class JSFDeploymentProcessorTestCase {

    private static final String TEST_HANDLER_NAME = "test-" + JSFDeploymentProcessorTestCase.class.getSimpleName();
    private static final String TEST_LOG_FILE_NAME  = TEST_HANDLER_NAME + ".log";
    private static final String LOG_MESSAGE = "WFLYJSF0005";
    private static final String WEB_FACES_CONFIG_XML = "faces-config-xml";

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private URL facesConfigXml;

    /**
     * Creates an ear with only the faces-config to indicate it is using Jakarta Server Faces, that way it will load the
     * one provided by WildFly
     *
     * @return deployment archive
     */
    @Deployment
    public static EnterpriseArchive createDeployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, WEB_FACES_CONFIG_XML + ".ear");
        ear.addAsManifestResource(JSFDeploymentProcessorTestCase.class.getPackage(),
                WEB_FACES_CONFIG_XML + "-application.xml", "application.xml");

        JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejb.addClasses(JSFVersionEJB.class);
        //add the ejb
        ear.addAsModule(ejb);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_FACES_CONFIG_XML + "-webapp.war");
        war.addClasses(JSFVersion.class);
        war.addAsWebInfResource(JSFVersion.class.getPackage(), WEB_FACES_CONFIG_XML + "-faces-config.xml",
                "faces-config.xml");
        war.addAsWebResource(JSFVersion.class.getPackage(), "jsfversion.xhtml", "jsfversion.xhtml");
        // add the .war
        ear.addAsModule(war);

        return ear;
    }

    @Test
    public void facesConfigXmlTest() throws Exception {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try (CloseableHttpClient client = httpClientBuilder.build()) {

            HttpUriRequest getVarRequest = new HttpGet(facesConfigXml.toExternalForm() + "jsfversion.xhtml");
            try (CloseableHttpResponse getVarResponse = client.execute(getVarRequest)) {
                String text = EntityUtils.toString(getVarResponse.getEntity());
                String facesVersion = FacesContext.class.getPackage().getSpecificationTitle();
                Assert.assertTrue("Text should contain [" + facesVersion + "] but it contains [" + text + "]",
                        text.contains(facesVersion));
            }
        }
        Assert.assertFalse("Unexpected log message: " + LOG_MESSAGE, LoggingUtil.hasLogMessage(managementClient.getControllerClient(),
                TEST_HANDLER_NAME, LOG_MESSAGE));
    }

    public static class TestLogHandlerSetup extends TestLogHandlerSetupTask {

        @Override
        public Collection<String> getCategories() {
            return List.of("org.jboss.as.jsf");
        }

        @Override
        public String getLevel() {
            return "WARN";
        }

        @Override
        public String getHandlerName() {
            return TEST_HANDLER_NAME;
        }

        @Override
        public String getLogFileName() {
            return TEST_LOG_FILE_NAME;
        }
    }
}
