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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.testsuite.integration.secman.servlets.PrintSystemPropertyServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Test case, which checks PropertyPermissions assigned to deployed web applications. The applications try to do a protected
 * action and it should either complete successfully if {@link java.util.PropertyPermission} is granted, or fail.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup(SystemPropertiesSetup.class)
@RunAsClient
public class WarPPTestCase extends AbstractPPTestsWithJSP {

    private static Logger LOGGER = Logger.getLogger(WarPPTestCase.class);

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = APP_GRANT, testable = false)
    public static WebArchive grantDeployment() {
        return warDeployment(APP_GRANT, GRANT_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = APP_LIMITED, testable = false)
    public static WebArchive limitedDeployment() {
        return warDeployment(APP_LIMITED, LIMITED_PERMISSIONS_XML);
    }

    /**
     * Creates archive with a tested application.
     *
     * @return {@link WebArchive} instance
     */
    @Deployment(name = APP_DENY, testable = false)
    public static WebArchive denyDeployment() {
        return warDeployment(APP_DENY, EMPTY_PERMISSIONS_XML);
    }

    private static WebArchive warDeployment(final String appName, Asset permissionsXmlAsset) {
        LOGGER.trace("Start WAR deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, appName + ".war");
        addJSMCheckServlet(war);
        addPermissionsXml(war, permissionsXmlAsset, null);
        war.addClasses(PrintSystemPropertyServlet.class);
        war.addAsWebResource(PrintSystemPropertyServlet.class.getPackage(), "readproperty.jsp", "readproperty.jsp");
        return war;
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

}
