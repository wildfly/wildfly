/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.propertypermission;

import static org.jboss.as.testsuite.integration.secman.propertypermission.SystemPropertiesSetup.PROPERTY_NAME;

import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;

/**
 * Abstract parent for PropertyPermission testcases, which have also readproperty.jsp page included.
 *
 * @author Josef Cacek
 */
public abstract class AbstractPPTestsWithJSP extends AbstractPropertyPermissionTests {

    /**
     * Check standard java property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_GRANT)
    public void testJavaHomePropertyInJSPGrant(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application, where JSP don't get PropertyPermissions (servlets get them).
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_LIMITED)
    public void testJavaHomePropertyInJSPLimited(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_OK);
    }

    /**
     * Check standard java property access in application.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_DENY)
    public void testJavaHomePropertyInJSPDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkJavaHomePropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check AS defined (standalone.xml) property access in application, where PropertyPermission for all properties is granted.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    @OperateOnDeployment(APP_GRANT)
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
    @OperateOnDeployment(APP_LIMITED)
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
    @OperateOnDeployment(APP_DENY)
    public void testASLevelPropertyInJSPDeny(@ArquillianResource URL webAppURL) throws Exception {
        checkTestPropertyInJSP(webAppURL, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Check access to 'java.home' property.
     */
    protected void checkJavaHomePropertyInJSP(URL webAppURL, int expectedStatus) throws Exception {
        checkPropertyInJSP(webAppURL, "java.home", expectedStatus, "java.home=");
    }

    /**
     * Check access to {@value #WEBAPP_BASE_NAME} property.
     */
    protected void checkTestPropertyInJSP(URL webAppURL, final int expectedStatus) throws Exception {
        checkPropertyInJSP(webAppURL, PROPERTY_NAME, expectedStatus, PROPERTY_NAME);
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
    protected abstract void checkPropertyInJSP(final URL webAppURL, final String propertyName, final int expectedCode,
            final String expectedBodyStart) throws Exception;
}
