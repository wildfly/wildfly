/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.subsystem;

import java.net.URI;
import java.net.URL;

import jakarta.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.testsuite.integration.secman.servlets.PrintSystemPropertyServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This class contains test for minimum-permissions attribute in security-manager subsystem. The permissions listed in
 * minimum-permissions should be granted to all deployed applications.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MinimumPermissionsTestCase extends ReloadableCliTestBase {

    private static final String DEPLOYMENT = "deployment";
    private static Logger LOGGER = Logger.getLogger(MinimumPermissionsTestCase.class);

    @ArquillianResource
    private URL webAppURL;

    /**
     * Test deployment with {@link PrintSystemPropertyServlet} and without any permissions deployment descriptor.
     *
     * @return
     */
    @Deployment(name = DEPLOYMENT, testable = false)
    public static WebArchive deployment() {
        LOGGER.debug("Start WAR deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war");
        war.addClasses(PrintSystemPropertyServlet.class);
        return war;
    }

    /**
     * Tests that permissions are not granted if the minimum-permissions is empty.
     *
     * @throws Exception
     */
    @Test
    public void testEmptyMinPerm() throws Exception {
        assertPropertyNonReadable();
    }

    /**
     * Tests that property permissions are not granted if the minimum-permissions contains only a {@link java.io.FilePermission}
     * entry.
     *
     * @throws Exception
     */
    @Test
    public void testFilePerm(@ArquillianResource URL webAppURL) throws Exception {
        CLIOpResult opResult = doCliOperation(
                "/subsystem=security-manager/deployment-permissions=default:write-attribute(name=minimum-permissions, value=[{class=java.io.FilePermission, actions=read, name=\"/-\"}])");
        assertOperationRequiresReload(opResult);
        reloadServer();

        assertPropertyNonReadable();

        opResult = doCliOperation(
                "/subsystem=security-manager/deployment-permissions=default:undefine-attribute(name=minimum-permissions)");
        assertOperationRequiresReload(opResult);
        reloadServer();
    }

    /**
     * Tests that permission for reading system property is granted if the minimum-permissions contains PropertyPermission with
     * wildcard '*'. entry.
     *
     * @throws Exception
     */
    @Test
    public void testPropertyPerm(@ArquillianResource URL webAppURL) throws Exception {
        doCliOperation(
                "/subsystem=security-manager/deployment-permissions=default:write-attribute(name=minimum-permissions, value=[{class=java.util.PropertyPermission, actions=read, name=\"*\"}])");
        reloadServer();

        assertPropertyReadable();

        doCliOperation(
                "/subsystem=security-manager/deployment-permissions=default:undefine-attribute(name=minimum-permissions)");
        reloadServer();
    }

    /**
     * Tests that property permissions are granted if the minimum-permissions contains a {@link java.security.AllPermission}
     * entry.
     *
     * @param webAppURL
     * @throws Exception
     */
    @Test
    public void testAllPerm(@ArquillianResource URL webAppURL) throws Exception {
        doCliOperation(
                "/subsystem=security-manager/deployment-permissions=default:write-attribute(name=minimum-permissions, value=[{class=java.security.AllPermission}])");
        reloadServer();

        assertPropertyReadable();

        doCliOperation(
                "/subsystem=security-manager/deployment-permissions=default:undefine-attribute(name=minimum-permissions)");
        reloadServer();
    }

    /**
     * Checks access to a system property on the server using {@link PrintSystemPropertyServlet}.
     *
     * @param expectedCode expected HTTP Status code
     * @throws Exception
     */
    protected void checkPropertyAccess(boolean successExpected) throws Exception {
        final String propertyName = "java.home";

        final URI sysPropUri = new URI(webAppURL.toExternalForm() + PrintSystemPropertyServlet.SERVLET_PATH.substring(1) + "?"
                + Utils.encodeQueryParam(PrintSystemPropertyServlet.PARAM_PROPERTY_NAME, propertyName));

        Utils.makeCall(sysPropUri, successExpected ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Asserts that a system property is readable from the test deployment.
     *
     * @throws Exception
     */
    protected void assertPropertyReadable() throws Exception {
        checkPropertyAccess(true);
    }

    /**
     * Asserts that a system property is not readable from the test deployment.
     *
     * @throws Exception
     */
    protected void assertPropertyNonReadable() throws Exception {
        checkPropertyAccess(false);
    }
}
