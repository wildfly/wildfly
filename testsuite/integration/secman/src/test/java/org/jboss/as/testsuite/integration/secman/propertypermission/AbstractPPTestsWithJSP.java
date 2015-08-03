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

import java.net.URL;

import javax.servlet.http.HttpServletResponse;

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
