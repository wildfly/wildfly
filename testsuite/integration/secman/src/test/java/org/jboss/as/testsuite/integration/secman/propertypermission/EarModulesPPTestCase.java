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

package org.jboss.as.testsuite.integration.secman.propertypermission;

import static org.jboss.as.testsuite.integration.secman.propertypermission.SystemPropertiesSetup.PROPERTY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URL;
import java.security.AccessControlException;

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
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyBean;
import org.jboss.as.testsuite.integration.secman.ejbs.ReadSystemPropertyRemote;
import org.jboss.as.testsuite.integration.secman.servlets.PrintSystemPropertyServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case, which checks PropertyPermissions assigned to sub-deployment of deployed ear applications. The applications try to
 * do a protected action and it should either complete successfully if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author Ondrej Lukas
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup(SystemPropertiesSetup.class)
@RunAsClient
public class EarModulesPPTestCase extends AbstractPPTestsWithJSP {

    private static final String EJBAPP_BASE_NAME = "ejb-module";
    private static final String WEBAPP_BASE_NAME = "web-module";

    private static final String APP_NO_PERM = "read-props-noperm";
    private static final String APP_EMPTY_PERM = "read-props-emptyperm";

    private static Logger LOGGER = Logger.getLogger(EarModulesPPTestCase.class);

    @ArquillianResource
    private InitialContext iniCtx;

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = APP_GRANT, testable = false)
    public static EnterpriseArchive createDeployment1() {
        return earDeployment(APP_GRANT, AbstractPropertyPermissionTests.GRANT_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = APP_LIMITED, testable = false)
    public static EnterpriseArchive createDeployment2() {
        return earDeployment(APP_LIMITED, AbstractPropertyPermissionTests.LIMITED_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = APP_DENY, testable = false)
    public static EnterpriseArchive createDeployment3() {
        return earDeployment(APP_DENY, AbstractPropertyPermissionTests.EMPTY_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = APP_NO_PERM, testable = false)
    public static EnterpriseArchive createNoPermDeployment() {
        return earDeployment(APP_NO_PERM, null, ALL_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link EnterpriseArchive} instance
     */
    @Deployment(name = APP_EMPTY_PERM, testable = false)
    public static EnterpriseArchive createEmptyPermDeployment() {
        return earDeployment(APP_EMPTY_PERM, AbstractPropertyPermissionTests.EMPTY_PERMISSIONS_XML, ALL_PERMISSIONS_XML);
    }

    /**
     * Check standard java property access for EJB in ear, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_GRANT)
    public void testJavaHomePropertyEjbInJarGrant() throws Exception {
        checkJavaHomePropertyEjb(APP_GRANT, false);
    }

    /**
     * Check standard java property access for EJB in ear, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_LIMITED)
    public void testJavaHomePropertyEjbInJarLimited() throws Exception {
        checkJavaHomePropertyEjb(APP_LIMITED, false);
    }

    /**
     * Check standard java property access for EJB in ear, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_DENY)
    public void testJavaHomePropertyEjbInJarDeny() throws Exception {
        checkJavaHomePropertyEjb(APP_DENY, true);
    }

    /**
     * Check standard java property access for EJB in ear, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_GRANT)
    public void testASLevelPropertyEjbInJarGrant() throws Exception {
        checkTestPropertyEjb(APP_GRANT, false);
    }

    /**
     * Check standard java property access for EJB in ear, where not all PropertyPermissions are granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_LIMITED)
    public void testASLevelPropertyEjbInJarLimited() throws Exception {
        checkTestPropertyEjb(APP_LIMITED, true);
    }

    /**
     * Check standard java property access for EJB in ear, where no PropertyPermission is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_DENY)
    public void testASLevelPropertyEjbInJarDeny() throws Exception {
        checkTestPropertyEjb(APP_DENY, true);
    }

    /**
     * Check permission.xml overrides in ear deployments.
     */
    @Test
    @OperateOnDeployment(APP_NO_PERM)
    public void testASLevelPropertyEjbInJarNoPerm() throws Exception {
        checkTestPropertyEjb(APP_NO_PERM, true);
    }

    /**
     * Check permission.xml overrides in ear deployments.
     */
    @Test
    @OperateOnDeployment(APP_EMPTY_PERM)
    public void testASLevelPropertyEjbInJarEmptyPerm() throws Exception {
        checkTestPropertyEjb(APP_EMPTY_PERM, true);
    }

    /**
     * Check permission.xml overrides in ear deployments.
     */
    @Test
    @OperateOnDeployment(APP_NO_PERM)
    public void testJavaHomePropertyInJSPNoPerm(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check permission.xml overrides in ear deployments.
     */
    @Test
    @OperateOnDeployment(APP_EMPTY_PERM)
    public void testJavaHomePropertyInJSPEmptyPerm(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check permission.xml overrides in ear deployments.
     */
    @Test
    @OperateOnDeployment(APP_NO_PERM)
    public void testASLevelPropertyInJSPNoPerm(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check permission.xml overrides in ear deployments.
     */
    @Test
    @OperateOnDeployment(APP_EMPTY_PERM)
    public void testASLevelPropertyInJSPEmptyPerm(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check permission.xml overrides in ear deployments.
     */
    @Test
    @OperateOnDeployment(APP_NO_PERM)
    public void testASLevelPropertyNoPerm(@ArquillianResource URL webAppURL) throws Exception {
        checkTestProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check permission.xml overrides in ear deployments.
     */
    @Test
    @OperateOnDeployment(APP_EMPTY_PERM)
    public void testASLevelPropertyEmptyPerm(@ArquillianResource URL webAppURL) throws Exception {
        checkTestProperty(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
        checkPropertyEjb(moduleNameSuffix, PROPERTY_NAME, exceptionExpected, PROPERTY_NAME);
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
    @Override
    protected void checkProperty(final URL webAppURL, final String propertyName, final int expectedCode,
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
    @Override
    protected void checkPropertyInJSP(final URL webAppURL, final String propertyName, final int expectedCode,
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
     * @param moduleName
     * @param propertyName
     * @param exceptionExpected
     * @param expectedValue
     * @throws Exception
     */
    private void checkPropertyEjb(final String moduleName, final String propertyName, final boolean exceptionExpected,
            final String expectedValue) throws Exception {
        LOGGER.debug("Checking if '" + propertyName + "' property is available");
        ReadSystemPropertyRemote bean = lookupEjb(moduleName, EJBAPP_BASE_NAME + moduleName,
                ReadSystemPropertyBean.class.getSimpleName(), ReadSystemPropertyRemote.class);
        assertNotNull(bean);

        Exception ex = null;
        String propertyValue = null;
        try {
            propertyValue = bean.readSystemProperty(propertyName);
        } catch (Exception e) {
            ex = e;
        }

        if (ex instanceof EJBException && ex.getCause() instanceof AccessControlException) {
            assertTrue("AccessControlException came, but it was not expected", exceptionExpected);
        } else if (ex != null) {
            throw ex;
        } else if (exceptionExpected) {
            fail("AccessControlException was expected");
        }

        if (ex == null && expectedValue != null) {
            assertEquals("System property value doesn't match the expected one.", expectedValue, propertyValue);
        }
    }

    private <T> T lookupEjb(final String appName, final String moduleName, final String beanName, final Class<T> interfaceType)
            throws NamingException {
        return interfaceType.cast(iniCtx.lookup("ejb:" + appName + "/" + moduleName + "//" + beanName + "!"
                + interfaceType.getName()));
    }

    private static EnterpriseArchive earDeployment(final String suffix, final Asset permissionsXml) {
        return earDeployment(suffix, permissionsXml, null);
    }

    private static EnterpriseArchive earDeployment(final String suffix, final Asset permissionsXml, final Asset modulesPermXml) {
        JavaArchive jar = ejbDeployment(suffix);
        WebArchive war = warDeployment(suffix);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, suffix + ".ear");
        addPermissionsXml(jar, modulesPermXml, null);
        addPermissionsXml(war, modulesPermXml, null);
        ear.addAsModule(jar);
        ear.addAsModule(war);
        addPermissionsXml(ear, permissionsXml, null);
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
        addJSMCheckServlet(war);
        return war;
    }

}
