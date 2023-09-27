/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.propertypermission;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.testsuite.integration.secman.PermissionUtil;
import org.jboss.as.testsuite.integration.secman.servlets.CallPermissionUtilServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Abstract parent for tests which creates deployments with library JAR including {@link PermissionUtil}.
 *
 * @author Josef Cacek
 */
public abstract class AbstractPPTestsWithLibrary extends AbstractPropertyPermissionTests {

    private static Logger LOGGER = Logger.getLogger(AbstractPPTestsWithLibrary.class);

    /**
     * Check access to 'java.home' property.
     */
    @Override
    protected void checkJavaHomeProperty(URL webAppURL, int expectedStatus) throws Exception {
        checkProperty(webAppURL, "java.home", expectedStatus);
    }

    /**
     * Checks access to a system property on the server using {@link CallPermissionUtilServlet}.
     *
     * @param webAppURL
     * @param propertyName
     * @param expectedCode expected HTTP Status code
     * @throws Exception
     */
    protected void checkProperty(final URL webAppURL, final String propertyName, final int expectedCode) throws Exception {
        checkProperty(webAppURL, propertyName, expectedCode, null);
    }

    /**
     * Checks access to a system property on the server.
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
        final URI sysPropUri = new URI(webAppURL.toExternalForm() + CallPermissionUtilServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(CallPermissionUtilServlet.PARAM_PROPERTY_NAME, propertyName));
        LOGGER.debug("Checking if '" + propertyName + "' property is available: " + sysPropUri);
        final String respBody = Utils.makeCall(sysPropUri, expectedCode);
        if (expectedBody != null && HttpServletResponse.SC_OK == expectedCode) {
            assertEquals("System property value doesn't match the expected one.", expectedBody, respBody);
        }
    }

    /**
     * Create java archive with PropertyReadStaticMethodClass class
     *
     * @return created java archive
     */
    protected static JavaArchive createLibrary() {
        final JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "library.jar");
        lib.addClasses(PermissionUtil.class);
        return lib;
    }

}
