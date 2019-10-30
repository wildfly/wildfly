/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.testsuite.integration.secman.subsystem;

import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

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
