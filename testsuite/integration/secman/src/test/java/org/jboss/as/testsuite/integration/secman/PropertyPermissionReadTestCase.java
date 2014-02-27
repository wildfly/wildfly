/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.secman;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.testsuite.integration.secman.servlets.JSMCheckServlet;
import org.jboss.as.testsuite.integration.secman.servlets.PrintSystemPropertyServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case, which checks PropertyPermissions assigned to deployed applications. The applications try to do a protected action
 * and it should either complete successfully if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup(PropertyPermissionReadTestCase.SystemPropertiesSetup.class)
@RunAsClient
public class PropertyPermissionReadTestCase {

    private static final String WEBAPP_BASE_NAME = "read-props";
    private static final String WEBAPP_SFX_GRANT = "-grant";
    private static final String WEBAPP_SFX_LIMITED = "-limited";
    private static final String WEBAPP_SFX_DENY = "-deny";

    private static Logger LOGGER = Logger.getLogger(PropertyPermissionReadTestCase.class);

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = WEBAPP_SFX_GRANT, testable = false)
    public static WebArchive grantDeployment() {
        return warDeployment(WEBAPP_SFX_GRANT);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = WEBAPP_SFX_LIMITED, testable = false)
    public static WebArchive limitedDeployment() {
        return warDeployment(WEBAPP_SFX_LIMITED);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = WEBAPP_SFX_DENY, testable = false)
    public static WebArchive denyDeployment() {
        return warDeployment(WEBAPP_SFX_DENY);
    }

    /**
     * Checks if the AS runs with security manager enabled.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_GRANT)
    public void testJSMEnabled(@ArquillianResource URL webAppURL) throws Exception {
        final URI checkJSMuri = new URI(webAppURL.toExternalForm() + JSMCheckServlet.SERVLET_PATH.substring(1));
        LOGGER.debug("Checking if JSM is enabled: " + checkJSMuri);
        assertEquals("JSM should be enabled.", Boolean.toString(true), Utils.makeCall(checkJSMuri, 200));
    }

    /**
     * Check standard java property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_GRANT)
    public void testJavaHomePropertyGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomeProperty(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_GRANT)
    public void testJavaHomePropertyInJSPGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_LIMITED)
    public void testJavaHomePropertyLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomeProperty(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application, where JSP don't get PropertyPermissions (servlets get them).
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_LIMITED)
    public void testJavaHomePropertyInJSPLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check standard java property access in application, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_DENY)
    public void testJavaHomePropertyDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomeProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check standard java property access in application.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_DENY)
    public void testJavaHomePropertyInJSPDeny(@ArquillianResource URL webAppURL) throws Exception {
        // XXX what is the correct codebase VFS URL for JSPs (look at exact match in security.policy)
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_GRANT)
    public void testASLevelPropertyGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkTestProperty(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_GRANT)
    public void testASLevelPropertyInJSPGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInJSP(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_LIMITED)
    public void testASLevelPropertyLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkTestProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_LIMITED)
    public void testASLevelPropertyInJSPLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_DENY)
    public void testASLevelPropertyDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkTestProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(WEBAPP_SFX_DENY)
    public void testASLevelPropertyInJSPDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private static WebArchive warDeployment(final String suffix) {
        LOGGER.info("Start WAR deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEBAPP_BASE_NAME + suffix + ".war");
        war.addClasses(PrintSystemPropertyServlet.class, JSMCheckServlet.class);
        war.addAsWebResource(PrintSystemPropertyServlet.class.getPackage(), "readproperty.jsp", "readproperty.jsp");
        LOGGER.info(war.toString(true));
        return war;
    }

    /**
     * Check access to 'java.home' property.
     */
    private void checkJavaHomeProperty(URL webAppURL, int expectedStatus) throws Exception {
        checkProperty(webAppURL, "java.home", expectedStatus, null);
    }

    /**
     * Check access to 'java.home' property.
     */
    private void checkJavaHomePropertyInJSP(URL webAppURL, int expectedStatus) throws Exception {
        checkPropertyInJSP(webAppURL, "java.home", expectedStatus, "java.home=");
    }

    /**
     * Check access to {@value #WEBAPP_BASE_NAME} property.
     */
    private void checkTestProperty(URL webAppURL, final int expectedStatus) throws Exception {
        checkProperty(webAppURL, WEBAPP_BASE_NAME, expectedStatus, WEBAPP_BASE_NAME);
    }

    /**
     * Check access to {@value #WEBAPP_BASE_NAME} property.
     */
    private void checkTestPropertyInJSP(URL webAppURL, final int expectedStatus) throws Exception {
        checkPropertyInJSP(webAppURL, WEBAPP_BASE_NAME, expectedStatus, WEBAPP_BASE_NAME);
    }

    /**
     * Checks access to a system property on the server using {@link PrintSystemPropertyServlet}.
     *
     * @param webAppURL
     * @param propertyName
     * @param expectedCode expected HTTP Status code
     * @param expectedBody expected response value; if null then response body is not checked
     * @throws Exception
     */
    private void checkProperty(final URL webAppURL, final String propertyName, final int expectedCode, final String expectedBody)
            throws Exception {
        final URI sysPropUri = new URI(webAppURL.toExternalForm() + PrintSystemPropertyServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(PrintSystemPropertyServlet.PARAM_PROPERTY_NAME, propertyName));
        LOGGER.debug("Checking if '" + propertyName + "' property is available: " + sysPropUri);
        final String respBody = Utils.makeCall(sysPropUri, expectedCode);
        if (expectedBody != null && HttpServletResponse.SC_OK == expectedCode) {
            assertEquals("System property value doesn't match the expected one.", expectedBody, respBody);
        }
    }

    /**
     * Checks access to a system property on the server using <code>readproperty.jsp</code>.
     *
     * @param webAppURL
     * @param propertyName
     * @param expectedCode expected HTTP Status code
     * @param expectedBodyStart expected beginning of response value; if null then response body is not checked
     * @throws Exception
     */
    private void checkPropertyInJSP(final URL webAppURL, final String propertyName, final int expectedCode,
            final String expectedBodyStart) throws Exception {
        final URI sysPropUri = new URI(webAppURL.toExternalForm() + "readproperty.jsp" + "?"
                + Utils.encodeQueryParam(PrintSystemPropertyServlet.PARAM_PROPERTY_NAME, propertyName));
        LOGGER.debug("Checking if '" + propertyName + "' property is available: " + sysPropUri);
        final String respBody = Utils.makeCall(sysPropUri, expectedCode);
        if (expectedBodyStart != null && HttpServletResponse.SC_OK == expectedCode) {
            assertNotNull("Response from JSP should not be null.", respBody);
            assertTrue("The readproperty.jsp response doesn't start with the expected value.",
                    respBody.startsWith(expectedBodyStart));
        }
    }

    /**
     * Server setup task, which adds a system property to AS configuration.
     */
    public static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {
        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[] { new DefaultSystemProperty(WEBAPP_BASE_NAME, WEBAPP_BASE_NAME) };
        }
    }

}
