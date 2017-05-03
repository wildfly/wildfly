/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.deployment.dependencies.ear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Hashtable;

import javax.ejb.NoSuchEJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ArchiveDeployer;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for inter-deployment dependencies in EAR files. It also contains a module dependency simple test - EJB module depends on
 * WEB module in app1.ear.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class InterDeploymentDependenciesEarTestCase {

    private static Logger LOGGER = Logger.getLogger(InterDeploymentDependenciesEarTestCase.class);

    private static final String DEP_APP1 = "app1";
    private static final String DEP_APP2 = "app2";
    private static final String MODULE_EJB = "hello";
    private static final String MODULE_WEB = "staller";

    @ArquillianResource
    public ManagementClient managementClient;

    // We don't inject this via @ArquillianResource because ARQ can't fully control
    // DEPA_APP1 and DEP_APP2 and things go haywire if we try. But we use ArchiveDeployer
    // because it's a convenient API for handling deploy/undeploy of Shrinkwrap archives
    private ArchiveDeployer deployer;

    // Public methods --------------------------------------------------------

    // Dummy deployment so arq will be able to inject a ManagementClient
    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "dummy.jar");
    }

    private static EnterpriseArchive DEPENDEE = ShrinkWrap
                .create(EnterpriseArchive.class, DEP_APP1 + ".ear")
                .addAsModule(createWar())
                .addAsModule(createBeanJar())
                .addAsLibrary(createLogLibrary())
                .addAsManifestResource(InterDeploymentDependenciesEarTestCase.class.getPackage(), "application.xml",
                        "application.xml");

    private static EnterpriseArchive DEPENDENT = ShrinkWrap
                .create(EnterpriseArchive.class, DEP_APP2 + ".ear")
                .addAsLibrary(createLogLibrary())
                .addAsModule(createBeanJar())
                .addAsManifestResource(InterDeploymentDependenciesEarTestCase.class.getPackage(), "jboss-all.xml",
                        "jboss-all.xml").addAsModule(createBeanJar());

    @Before
    public void setup() {
        deployer = new ArchiveDeployer(managementClient);
    }

    @After
    public void cleanUp() {
        try {
            deployer.undeploy(DEPENDENT.getName());
        } catch (Exception e) {
            // Ignore
        }

        try {
            deployer.undeploy(DEPENDEE.getName());
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Tests enterprise application dependencies.
     */
    @Test
    public void test() throws NamingException, IOException, DeploymentException {
        try {
            deployer.deploy(DEPENDENT);
            fail("Application deployment must fail if the dependencies are not satisfied.");
        } catch (Exception e) {
            LOGGER.debug("Expected fail", e);
        }

        deployer.deploy(DEPENDEE);
        deployer.deploy(DEPENDENT);

        final LogAccess helloApp1 = lookupEJB(DEP_APP1);
        final LogAccess helloApp2 = lookupEJB(DEP_APP2);
        assertEquals(SleeperContextListener.class.getSimpleName() + LogAccessBean.class.getSimpleName(), helloApp1.getLog());
        assertEquals(LogAccessBean.class.getSimpleName(), helloApp2.getLog());

        forceDependeeUndeploy();
        try {
            helloApp2.getLog();
            fail("Calling EJB from dependent application should fail");
        } catch (IllegalStateException | NoSuchEJBException e) {
            //OK
        }
        // cleanUp will undeploy DEP_APP2
    }

    @Test
    public void testWithRestart() throws NamingException, IOException, DeploymentException, MgmtOperationException {
        try {
            deployer.deploy(DEPENDENT);
            fail("Application deployment must fail if the dependencies are not satisfied.");
        } catch (Exception e) {
            LOGGER.debug("Expected fail", e);
        }

        deployer.deploy(DEPENDEE);
        deployer.deploy(DEPENDENT);

        LogAccess helloApp1 = lookupEJB(DEP_APP1);
        LogAccess helloApp2 = lookupEJB(DEP_APP2);
        assertEquals(SleeperContextListener.class.getSimpleName() + LogAccessBean.class.getSimpleName(), helloApp1.getLog());
        assertEquals(LogAccessBean.class.getSimpleName(), helloApp2.getLog());

        ModelNode redeploy = Util.createEmptyOperation("redeploy", PathAddress.pathAddress("deployment", DEPENDEE.getName()));
        ManagementOperations.executeOperation(managementClient.getControllerClient(), redeploy);

        helloApp1 = lookupEJB(DEP_APP1);
        helloApp2 = lookupEJB(DEP_APP2);
        assertEquals(SleeperContextListener.class.getSimpleName() + LogAccessBean.class.getSimpleName(), helloApp1.getLog());
        assertEquals(LogAccessBean.class.getSimpleName(), helloApp2.getLog());

        forceDependeeUndeploy();
        try {
            helloApp2.getLog();
            fail("Calling EJB from dependent application should fail");
        } catch (IllegalStateException | NoSuchEJBException e) {
            //OK
        }
        // cleanUp will undeploy DEP_APP2
    }

    // Private methods -------------------------------------------------------

    /**
     * Lookups LogAccess bean for given application name.
     *
     * @param appName application name
     * @return the LogAccess bean
     * @throws NamingException if lookup fails
     */
    private LogAccess lookupEJB(String appName) throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<>();
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new InitialContext(jndiProperties);
        return (LogAccess) context.lookup("ejb:" + appName + "/" + MODULE_EJB + "/" + LogAccessBean.class.getSimpleName() + "!"
                + LogAccess.class.getName());
    }

    private void forceDependeeUndeploy() throws IOException {
        ModelNode forcedUndeploy = Util.createEmptyOperation("undeploy", PathAddress.pathAddress("deployment", DEPENDEE.getName()));
        forcedUndeploy.get("operation-headers", "rollback-on-runtime-failure").set(false);
        ModelNode response = managementClient.getControllerClient().execute(forcedUndeploy);
        // This will succeed until WFCORE-1762 is fixed; once it is check that the failure didn't
        // result in rollback
        if ("failed".equals(response.get("outcome").asString())) {
            Assert.assertFalse(response.toString(), response.get("rolled-back").asBoolean(true));
        }

    }

    /**
     * Creates a shared lib with logger.
     */
    private static JavaArchive createLogLibrary() {
        return ShrinkWrap.create(JavaArchive.class, "log.jar").addClass(Log.class);
    }

    /**
     * Creates testing web-app (module for app1.ear)
     */
    private static Archive<?> createWar() {
        return ShrinkWrap.create(WebArchive.class, MODULE_WEB + ".war")
                .setWebXML(InterDeploymentDependenciesEarTestCase.class.getPackage(), "web.xml")
                .addClass(SleeperContextListener.class);
    }

    /**
     * Creates a testing EJB module (for app1 and app2)
     */
    private static Archive<?> createBeanJar() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_EJB + ".jar")
                .addClasses(LogAccess.class, LogAccessBean.class).addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
}
