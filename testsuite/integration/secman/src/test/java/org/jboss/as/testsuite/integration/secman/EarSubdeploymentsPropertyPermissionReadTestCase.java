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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URL;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyBean;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyRemote;
import org.jboss.as.testsuite.integration.secman.servlets.PrintSystemPropertyServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case, which checks PropertyPermissions assigned to sub-deployment of deployed ear applications. The applications try to
 * do a protected action and it should either complete successfully if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author olukas
 */
@RunWith(Arquillian.class)
@ServerSetup(EarSubdeploymentsPropertyPermissionReadTestCase.SystemPropertiesSetup.class)
@RunAsClient
public class EarSubdeploymentsPropertyPermissionReadTestCase {

    private static final String EARAPP_BASE_NAME = "ear-subdeployments-read-props";
    private static final String EJBAPP_BASE_NAME = "ear-subdeployments-ejb-read-props";
    private static final String WEBAPP_BASE_NAME = "ear-subdeployments-web-read-props";
    private static final String SFX_GRANT = "-grant";
    private static final String SFX_LIMITED = "-limited";
    private static final String SFX_DENY = "-deny";

    private static Logger LOGGER = Logger.getLogger(EarSubdeploymentsPropertyPermissionReadTestCase.class);

    @ArquillianResource
    private InitialContext iniCtx;

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = EARAPP_BASE_NAME + SFX_GRANT)
    public static EnterpriseArchive createDeployment1() {
        return earDeployment(SFX_GRANT);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = EARAPP_BASE_NAME + SFX_LIMITED)
    public static EnterpriseArchive createDeployment2() {
        return earDeployment(SFX_LIMITED);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = EARAPP_BASE_NAME + SFX_DENY)
    public static EnterpriseArchive createDeployment3() {
        return earDeployment(SFX_DENY);
    }

    /**
     * Check standard java property access in servlet in war in ear, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_GRANT)
    public void testJavaHomePropertyInWarGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInServlet(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in servlet in war in ear, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_LIMITED)
    public void testJavaHomePropertyInWarLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInServlet(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in servlet in war in ear, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_DENY)
    public void testJavaHomePropertyInWarDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInServlet(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check standard java property access in servlet in war in ear, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_GRANT)
    public void testASLevelPropertyInWarGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInServlet(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in servlet in war in ear, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_LIMITED)
    public void testASLevelPropertyInWarLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInServlet(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check standard java property access in servlet in war in ear, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_DENY)
    public void testASLevelPropertyInWarDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInServlet(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check standard java property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_GRANT)
    public void testJavaHomePropertyForJSPInWarGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application, where JSP don't get PropertyPermissions (servlets get them).
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_LIMITED)
    public void testJavaHomePropertyForJSPInWarLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check standard java property access in application.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_DENY)
    public void testJavaHomePropertyForJSPInWarDeny(@ArquillianResource URL webAppURL) throws Exception {
        // XXX what is the correct codebase VFS URL for JSPs (look at exact match in security.policy)
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_GRANT)
    public void testASLevelPropertyForJSPInWarGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInJSP(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application, where JSP don't get PropertyPermissions (servlets get them).
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_LIMITED)
    public void testASLevelPropertyForJSPInWarLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_DENY)
    public void testASLevelPropertyForJSPInWarDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check standard java property access for EJB in ear, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_GRANT)
    public void testJavaHomePropertyEjbInJarGrant() throws Exception {
        checkJavaHomePropertyEjb(SFX_GRANT, false);
    }

    /**
     * Check standard java property access for EJB in ear, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_LIMITED)
    public void testJavaHomePropertyEjbInJarLimited() throws Exception {
        checkJavaHomePropertyEjb(SFX_LIMITED, false);
    }

    /**
     * Check standard java property access for EJB in ear, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_DENY)
    public void testJavaHomePropertyEjbInJarDeny() throws Exception {
        checkJavaHomePropertyEjb(SFX_DENY, true);
    }

    /**
     * Check standard java property access for EJB in ear, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_GRANT)
    public void testASLevelPropertyEjbInJarGrant() throws Exception {
        checkTestPropertyEjb(SFX_GRANT, false);
    }

    /**
     * Check standard java property access for EJB in ear, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_LIMITED)
    public void testASLevelPropertyEjbInJarLimited() throws Exception {
        checkTestPropertyEjb(SFX_LIMITED, true);
    }

    /**
     * Check standard java property access for EJB in ear, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(EARAPP_BASE_NAME + SFX_DENY)
    public void testASLevelPropertyEjbInJarDeny() throws Exception {
        checkTestPropertyEjb(SFX_DENY, true);
    }

    /**
     * Check access to 'java.home' property.
     */
    private void checkJavaHomePropertyInServlet(URL webAppURL, int expectedStatus) throws Exception {
        checkPropertyInServlet(webAppURL, "java.home", expectedStatus, null);
    }

    /**
     * Check access to {@value #EARAPP_BASE_NAME} property.
     */
    private void checkTestPropertyInServlet(URL webAppURL, final int expectedStatus) throws Exception {
        checkPropertyInServlet(webAppURL, EARAPP_BASE_NAME, expectedStatus, EARAPP_BASE_NAME);
    }

    /**
     * Check access to 'java.home' property.
     */
    private void checkJavaHomePropertyInJSP(URL webAppURL, int expectedStatus) throws Exception {
        checkPropertyInJSP(webAppURL, "java.home", expectedStatus, "java.home=");
    }

    /**
     * Check access to {@value #EARAPP_BASE_NAME} property.
     */
    private void checkTestPropertyInJSP(URL webAppURL, final int expectedStatus) throws Exception {
        checkPropertyInJSP(webAppURL, EARAPP_BASE_NAME, expectedStatus, EARAPP_BASE_NAME);
    }

    /**
     * Check access to 'java.home' property.
     */
    private void checkJavaHomePropertyEjb(final String moduleNameSuffix, final boolean exceptionExpected) throws Exception {
        checkPropertyEjb(moduleNameSuffix, "java.home", exceptionExpected, null);
    }

    /**
     * Check access to {@value #EARAPP_BASE_NAME} property.
     */
    private void checkTestPropertyEjb(final String moduleNameSuffix, final boolean exceptionExpected) throws Exception {
        checkPropertyEjb(moduleNameSuffix, EARAPP_BASE_NAME, exceptionExpected, EARAPP_BASE_NAME);
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
    private void checkPropertyInServlet(final URL webAppURL, final String propertyName, final int expectedCode,
            final String expectedBody) throws Exception {
        final URI sysPropUri = new URI(webAppURL.toExternalForm() + PrintSystemPropertyServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(PrintSystemPropertyServlet.PARAM_PROPERTY_NAME, propertyName));
        LOGGER.debug("Checking if '" + propertyName + "' property is available: " + sysPropUri);
        final String respBody = Utils.makeCall(sysPropUri, expectedCode);
        if (expectedBody != null && HttpServletResponse.SC_OK == expectedCode) {
            assertThat("System property value doesn't match the expected one.", respBody,
                    CoreMatchers.containsString(expectedBody));
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
     * Checks access to a system property on the server using EJB.
     *
     * @param moduleNameSuffix
     * @param propertyName
     * @param exceptionExpected
     * @param expectedValue
     * @throws Exception
     */
    private void checkPropertyEjb(final String moduleNameSuffix, final String propertyName, final boolean exceptionExpected,
            final String expectedValue) throws Exception {
        LOGGER.debug("Checking if '" + propertyName + "' property is available");
        ReadSystemPropertyRemote bean = lookupEjb(EARAPP_BASE_NAME + moduleNameSuffix, EJBAPP_BASE_NAME + moduleNameSuffix,
                ReadSystemPropertyBean.class.getSimpleName(), ReadSystemPropertyRemote.class);
        assertNotNull(bean);

        try {
            String propertyValue = bean.readSystemProperty(propertyName);
            if (expectedValue != null) {
                assertEquals("System property value doesn't match the expected one.", expectedValue, propertyValue);
            }

            if (exceptionExpected) {
                fail("An exception was expected");
            }
        } catch (Throwable e) {
            if (exceptionExpected == false || !(e instanceof EJBException)
                    || !(e.getCause() instanceof java.security.AccessControlException)) {
                LOGGER.warn("Unexpected exception occurred", e);
                fail("Unexpected exception occurred " + e);
            }
        }
    }

    private <T> T lookupEjb(final String appName, final String moduleName, final String beanName, final Class<T> interfaceType)
            throws NamingException {
        return interfaceType.cast(iniCtx.lookup("ejb:" + appName + "/" + moduleName + "//" + beanName + "!"
                + interfaceType.getName()));
    }

    private static EnterpriseArchive earDeployment(final String suffix) {
        JavaArchive jar = ejbDeployment(suffix);
        WebArchive war = warDeployment(suffix);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EARAPP_BASE_NAME + suffix + ".ear");
        ear.addAsModule(jar);
        ear.addAsModule(war);
        return ear;
    }

    private static JavaArchive ejbDeployment(final String suffix) {
        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, EJBAPP_BASE_NAME + suffix + ".jar");
        ejb.addPackage(ReadSystemPropertyBean.class.getPackage());
        return ejb;
    }

    private static WebArchive warDeployment(final String suffix) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, WEBAPP_BASE_NAME + suffix + ".war");
        war.addClasses(PrintSystemPropertyServlet.class);
        war.addAsWebResource(PrintSystemPropertyServlet.class.getPackage(), "readproperty.jsp", "readproperty.jsp");
        return war;
    }

    /**
     * Server setup task, which adds a system property to AS configuration.
     */
    public static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {
        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[] { new DefaultSystemProperty(EARAPP_BASE_NAME, EARAPP_BASE_NAME) };
        }
    }

}
