/*
 * JBoss, Home of Professional Open Source
 * Copyright 2021, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jsf.version;

import java.io.FilePermission;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.PropertyPermission;

import javax.faces.context.FacesContext;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jsf.version.ejb.JSFMyFacesEJB;
import org.jboss.as.test.integration.jsf.version.ejb.JSFVersionEJB;
import org.jboss.as.test.integration.jsf.version.war.JSFMyFaces;
import org.jboss.as.test.integration.jsf.version.war.JSFVersion;
import org.jboss.as.test.shared.TestLogHandlerSetupTask;
import org.jboss.as.test.shared.util.LoggingUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests different ways to add Jakarta Server Faces implementation in ear files
 * @author tmiyar
 *
 */
@RunWith(Arquillian.class)
@ServerSetup({JSFDeploymentProcessorTestCase.TestLogHandlerSetup.class})
@RunAsClient
public class JSFDeploymentProcessorTestCase {

    private static final String WEB_BUNDLED_JSF = "bundled-jsf";
    private static final String WEB_BUNDLED_JSF_WEB_XML = "bundled-jsf-web.xml";
    private static final String WEB_FACES_CONFIG_XML = "faces-config-xml";
    private static final String MYFACES = "MyFaces JSF-2.0 Core API";

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    @OperateOnDeployment(WEB_BUNDLED_JSF)
    private URL bundledJsf;

    @ArquillianResource
    @OperateOnDeployment(WEB_FACES_CONFIG_XML)
    private URL facesConfigXml;

    /**
     * Creates a war with all the libraries needed in the war/lib folder, this sample does not call the
     * ejb as it is not necessary to test if the bundled Jakarta Server Faces is loaded
     * @return
     */
    @Deployment(name = WEB_BUNDLED_JSF, testable = false)
    public static EnterpriseArchive createDeployment1() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, WEB_BUNDLED_JSF + ".ear");

        ear.addAsManifestResource(JSFDeploymentProcessorTestCase.class.getPackage(), WEB_BUNDLED_JSF + "-application.xml", "application.xml");
        ear.addAsManifestResource(createPermissionsXmlAsset(
                new RuntimePermission("getClassLoader"),
                new RuntimePermission("accessDeclaredMembers"),
                new PropertyPermission("*", "read"),
                new FilePermission("/-", "read")), "permissions.xml");

        JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, "bundled-jsf-ejb.jar");
        ejb.addClasses(JSFMyFacesEJB.class);
        //add the ejb
        ear.addAsModule(ejb);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_BUNDLED_JSF + "-webapp.war");
        war.addClasses(JSFMyFaces.class);
        war.addAsWebResource(JSFVersion.class.getPackage(), "jsfmyfacesversion.xhtml", "jsfmyfacesversion.xhtml");
        war.addAsWebInfResource(JSFVersion.class.getPackage(), WEB_BUNDLED_JSF_WEB_XML, "web.xml");

        //add Jakarta Server Faces as webapp lib
        war.addAsLibrary("myfaces/commons-beanutils-1.9.3.jar", "commons-beanutils-1.9.3.jar");
        war.addAsLibrary("myfaces/commons-collections-3.2.2.jar", "commons-collections-3.2.2.jar");
        war.addAsLibrary("myfaces/commons-digester-1.8.jar", "commons-digester-1.8.jar");
        war.addAsLibrary("myfaces/myfaces-api-2.0.24.jar", "myfaces-api-2.0.24.jar");
        war.addAsLibrary("myfaces/myfaces-impl-2.0.24.jar", "myfaces-impl-2.0.24.jar");
        // add the .war
        ear.addAsModule(war);
        return ear;
    }

    /**
     * Creates a war with only the faces-config to indicate it is using Jakarta Server Faces, that way it will load the one provided by Wildfly
     * @return
     */
    @Deployment(name = WEB_FACES_CONFIG_XML, testable = false)
    public static EnterpriseArchive createDeployment2() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, WEB_FACES_CONFIG_XML + ".ear");
        ear.addAsManifestResource(JSFDeploymentProcessorTestCase.class.getPackage(), WEB_FACES_CONFIG_XML + "-application.xml", "application.xml");

        JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        ejb.addClasses(JSFVersionEJB.class);
        //add the ejb
        ear.addAsModule(ejb);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEB_FACES_CONFIG_XML + "-webapp.war");
        war.addClasses(JSFVersion.class);
        war.addAsWebInfResource(JSFVersion.class.getPackage(), WEB_FACES_CONFIG_XML + "-faces-config.xml", "faces-config.xml");
        war.addAsWebResource(JSFVersion.class.getPackage(), "jsfversion.xhtml", "jsfversion.xhtml");
        // add the .war
        ear.addAsModule(war);

        return ear;
    }

    /**
     * Facing intermitent problem on myfaces 2.3 documented here https://issues.jboss.org/browse/WELD-1387
     * using myfaces 2.0 instead
     * @throws Exception
     */
    @Test
    public void bundledJsf() throws Exception {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try(CloseableHttpClient client = httpClientBuilder.build()) {

            HttpUriRequest getVarRequest = new HttpGet(bundledJsf.toExternalForm() + "jsfmyfacesversion.xhtml");
            try (CloseableHttpResponse getVarResponse = client.execute(getVarRequest)) {
                String text = EntityUtils.toString(getVarResponse.getEntity());
                Assert.assertTrue("Text should contain ["+ MYFACES + "] but it contains[" + text + "]", text.contains(MYFACES));
            }
        }
        Assert.assertFalse("Unexpected log message: " + LOG_MESSAGE, LoggingUtil.hasLogMessage(managementClient, TEST_HANDLER_NAME, LOG_MESSAGE));
    }

    @Test
    public void facesConfigXmlTest() throws Exception {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try(CloseableHttpClient client = httpClientBuilder.build()) {

            HttpUriRequest getVarRequest = new HttpGet(facesConfigXml.toExternalForm() + "jsfversion.xhtml");
            try (CloseableHttpResponse getVarResponse = client.execute(getVarRequest)) {
                String text = EntityUtils.toString(getVarResponse.getEntity());
                String facesVersion = FacesContext.class.getPackage().getSpecificationTitle();
                Assert.assertTrue("Text should contain ["+ facesVersion+ "] but it contains [" + text + "]", text.contains(facesVersion));
            }
        }
        Assert.assertFalse("Unexpected log message: " + LOG_MESSAGE, LoggingUtil.hasLogMessage(managementClient, TEST_HANDLER_NAME, LOG_MESSAGE));
    }
    private static final String TEST_HANDLER_NAME;
    private static final String TEST_LOG_FILE_NAME;
    private static final String LOG_MESSAGE;

    static {
        /*
         * Make both the test handler name and the test log file specific for this class and execution so that we do not
         * interfere with other test classes or multiple subsequent executions of this class against the same container
         */
        TEST_HANDLER_NAME = "test-" + JSFDeploymentProcessorTestCase.class.getSimpleName();
        TEST_LOG_FILE_NAME = TEST_HANDLER_NAME + ".log";
        LOG_MESSAGE = "WFLYJSF0005";
    }

    public static class TestLogHandlerSetup extends TestLogHandlerSetupTask {

        @Override
        public Collection<String> getCategories() {
            return Arrays.asList("org.jboss.as.jsf");
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
