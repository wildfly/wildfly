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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URL;
import java.util.PropertyPermission;

import javax.servlet.http.HttpServletResponse;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.as.testsuite.integration.secman.servlets.PrintSystemPropertyServlet;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This class contains test for maximum-permissions attribute in security-manager subsystem. The deployment should failed if the
 * deployed application asks more permissions than is allowed by the maximum-permissions.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RemoveDeploymentPermissionsServerSetupTask.class)
public class MaximumPermissionsTestCase extends ReloadableCliTestBase {

    private static final String DEPLOYMENT_PERM = "deployment-perm";
    private static final String DEPLOYMENT_JBOSS_PERM = "deployment-jboss-perm";
    private static final String DEPLOYMENT_NO_PERM = "deployment-no-perm";

    private static Logger LOGGER = Logger.getLogger(MaximumPermissionsTestCase.class);
    private static final String ADDRESS_WEB = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080/";

    private static final String INDEX_HTML = "OK";

    @ArquillianResource
    private Deployer deployer;

    /**
     * Returns Arquillian deployment which defines requested permissions in permissions.xml.
     */
    @Deployment(name = DEPLOYMENT_PERM, testable = false, managed = false)
    public static WebArchive deploymentPerm() {
        return createDeployment("permissions.xml", DEPLOYMENT_PERM);
    }

    /**
     * Returns Arquillian deployment which defines requested permissions in jboss-permissions.xml.
     */
    @Deployment(name = DEPLOYMENT_JBOSS_PERM, testable = false, managed = false)
    public static WebArchive deploymentJBossPerm() {
        return createDeployment("jboss-permissions.xml", DEPLOYMENT_JBOSS_PERM);
    }

    /**
     * Returns Arquillian deployment which doesn't define requested permissions.
     */
    @Deployment(name = DEPLOYMENT_NO_PERM, testable = false)
    public static WebArchive deploymentNoPerm() {
        return createDeployment(null, DEPLOYMENT_NO_PERM);
    }

    /**
     * Tests if deployment fails
     * <ul>
     * <li>when maximum-permissions is not defined and {@code permissions.xml} requests some permissions;</li>
     * <li>when maximum-permissions is not defined and {@code jboss-permissions.xml} requests some permissions.</li>
     * </ul>
     */
    @Test
    public void testMaximumPermissionsEmpty() throws Exception {
        try {
            doCliOperation(
                    "/subsystem=security-manager/deployment-permissions=default:add(maximum-permissions=[])");
            reloadServer();
            assertNotDeployable(DEPLOYMENT_PERM);
            assertNotDeployable(DEPLOYMENT_JBOSS_PERM);
        } finally {
            doCliOperationWithoutChecks("/subsystem=security-manager/deployment-permissions=default:remove()");
            reloadServer();
        }
    }

    /**
     * Tests if deployment succeeds but doing protected action fails, when maximum-permissions is not defined and requested
     * permissions declaration is not part of deployment.
     */
    @Test
    public void testNoPermEmptySet() throws Exception {
        assertPropertyNonReadable(DEPLOYMENT_NO_PERM);
    }

    /**
     * Tests if deployment fails, when maximum-permissions is contains another permissions than requested by deployment.
     */
    @Test
    public void testFilePerm(@ArquillianResource URL webAppURL) throws Exception {
        try {
            doCliOperation(
                    "/subsystem=security-manager/deployment-permissions=default:add(maximum-permissions=[{class=java.io.FilePermission, actions=read, name=\"/-\"}])");
            reloadServer();

            assertNotDeployable(DEPLOYMENT_PERM);
            assertNotDeployable(DEPLOYMENT_JBOSS_PERM);
            assertPropertyNonReadable(DEPLOYMENT_NO_PERM);
        } finally {
            doCliOperationWithoutChecks("/subsystem=security-manager/deployment-permissions=default:remove()");
            reloadServer();
        }
    }

    /**
     * Tests if deployment succeeds and permissions are granted, when maximum-permissions is contains permissions requested by
     * deployment.
     */
    @Test
    public void testPropertyPerm(@ArquillianResource URL webAppURL) throws Exception {
        try {
            CLIOpResult opResult = doCliOperation(
                    "/subsystem=security-manager/deployment-permissions=default:add(maximum-permissions=[{class=java.util.PropertyPermission, actions=read, name=\"*\"}])");
            assertOperationRequiresReload(opResult);
            reloadServer();

            assertDeployable(DEPLOYMENT_PERM, true);
            assertDeployable(DEPLOYMENT_JBOSS_PERM, true);
            assertPropertyNonReadable(DEPLOYMENT_NO_PERM);

        } finally {
            CLIOpResult opResult = doCliOperationWithoutChecks("/subsystem=security-manager/deployment-permissions=default:remove()");
            reloadServer();
            assertOperationRequiresReload(opResult);
        }
    }

    /**
     * Tests if deployments succeeds and permissions are granted, when maximum-permissions contains AllPermission entry.
     */
    @Test
    public void testAllPermAndEmptySet(@ArquillianResource URL webAppURL) throws Exception {
        try {
            doCliOperation(
                    "/subsystem=security-manager/deployment-permissions=default:add(maximum-permissions=[{class=java.security.AllPermission}])");
            reloadServer();

            // check the test apps are deployable and they have requested permissions
            try {
                deployer.deploy(DEPLOYMENT_PERM);
                assertPropertyReadable(DEPLOYMENT_PERM);
                deployer.deploy(DEPLOYMENT_JBOSS_PERM);
                assertPropertyReadable(DEPLOYMENT_JBOSS_PERM);

                assertPropertyNonReadable(DEPLOYMENT_NO_PERM);
                try {
                    // after removing permissions from maximum-set the deployment which requests non-granted permissions should
                    // fail.
                    CLIOpResult opResult = doCliOperation(
                            "/subsystem=security-manager/deployment-permissions=default:write-attribute(name=maximum-permissions, value=[]");
                    assertOperationRequiresReload(opResult);
                    reloadServer();

                    assertNotDeployed(DEPLOYMENT_PERM);
                    assertNotDeployed(DEPLOYMENT_JBOSS_PERM);
                    assertDeployed(DEPLOYMENT_NO_PERM);
                } finally {
                    // clean-up - undeploy
                    CLIOpResult opResult =  doCliOperation(
                            "/subsystem=security-manager/deployment-permissions=default:write-attribute(name=maximum-permissions, value=[{class=java.security.AllPermission}])");
                    reloadServer();
                    assertOperationRequiresReload(opResult);
                }
            } finally {
                deployer.undeploy(DEPLOYMENT_PERM);
                deployer.undeploy(DEPLOYMENT_JBOSS_PERM);
            }
        } finally {
            CLIOpResult opResult = doCliOperationWithoutChecks("/subsystem=security-manager/deployment-permissions=default:remove()");
            reloadServer();
            assertOperationRequiresReload(opResult);
        }
    }

    /**
     * Checks access to a system property on the server using {@link PrintSystemPropertyServlet}.
     *
     * @param deploymentName
     *
     * @param expectedCode expected HTTP Status code
     * @throws Exception
     */
    protected void checkPropertyAccess(boolean successExpected, String deploymentName) throws Exception {
        final String propertyName = "java.home";

        final URI sysPropUri = new URI(ADDRESS_WEB + deploymentName + PrintSystemPropertyServlet.SERVLET_PATH + "?"
                + Utils.encodeQueryParam(PrintSystemPropertyServlet.PARAM_PROPERTY_NAME, propertyName));

        Utils.makeCall(sysPropUri, successExpected ? HttpServletResponse.SC_OK : HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Asserts the system property is readable from deployment with given name.
     *
     * @param deploymentName
     * @throws Exception
     */
    protected void assertPropertyReadable(String deploymentName) throws Exception {
        checkPropertyAccess(true, deploymentName);
    }

    /**
     * Asserts the system property is not readable from deployment with given name.
     *
     * @param deploymentName
     * @throws Exception
     */
    protected void assertPropertyNonReadable(String deploymentName) throws Exception {
        checkPropertyAccess(false, deploymentName);
    }

    /**
     * Asserts the deployment of the application with given name succeeds and checks if system property is
     * readable/non-readable.
     *
     * @param deploymentName
     * @param expectedPropertyReadable expected "readability" of system property from deployment
     * @throws Exception
     */
    protected void assertDeployable(String deploymentName, boolean expectedPropertyReadable) throws Exception {
        deployer.deploy(deploymentName);
        LOGGER.debug("Manually deployed: " + deploymentName);
        if (expectedPropertyReadable) {
            assertPropertyReadable(deploymentName);
        } else {
            assertPropertyNonReadable(deploymentName);
        }
        deployer.undeploy(deploymentName);
    }

    /**
     * Asserts the deployment of the application with given name fails.
     *
     * @param deploymentName
     * @throws Exception
     */
    protected void assertNotDeployable(String deploymentName) {
        try {
            deployer.deploy(deploymentName);
            fail("Deployment failure expected for deployment: " + deploymentName);
        } catch (Exception e) {
            // expected
        } finally {
            try {
                deployer.undeploy(deploymentName);
            } catch (Exception e) {
                LOGGER.debug(e);
            }
        }
    }

    /**
     * Asserts the application with given name is deployed.
     *
     * @param deploymentName
     * @throws Exception
     */
    protected void assertDeployed(String deploymentName) throws Exception {
        final URI sysPropUri = new URI(ADDRESS_WEB + deploymentName + "/");
        final String strBody = Utils.makeCall(sysPropUri, HttpServletResponse.SC_OK);
        assertEquals("Unexpected message body returned.", INDEX_HTML, strBody);
    }

    /**
     * Asserts the application with given name is not-deployed.
     *
     * @param deploymentName
     * @throws Exception
     */
    protected void assertNotDeployed(String deploymentName) throws Exception {
        final URI sysPropUri = new URI(ADDRESS_WEB + deploymentName + "/");
        Utils.makeCall(sysPropUri, HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Creates deployment with {@link PrintSystemPropertyServlet} and index.html simple page. If permissionsFilename parameter
     * is not-<code>null</code>, then permission declaration file with given name is also generated to the deployment.
     *
     * @param permissionsFilename filename under META-INF where to store requested permissions (usually permissions.xml,
     *        jboss-permissions.xml or null to skip requesting permissions)
     * @param deploymentName
     * @return
     */
    private static WebArchive createDeployment(String permissionsFilename, String deploymentName) {
        LOGGER.debug("Start WAR deployment");
        final WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName + ".war");
        war.addClasses(PrintSystemPropertyServlet.class);
        war.addAsWebResource(new StringAsset(INDEX_HTML), "index.html");
        if (permissionsFilename != null) {
            war.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission("*", "read")),
                    permissionsFilename);
        }
        return war;
    }

}
