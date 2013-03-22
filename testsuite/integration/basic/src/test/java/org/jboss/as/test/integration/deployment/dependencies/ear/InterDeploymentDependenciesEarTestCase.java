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

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
    public Deployer deployer;

    // Public methods --------------------------------------------------------

    /**
     * Creates app1.ear deployment.
     *
     * @return
     */
    @Deployment(name = DEP_APP1, managed = false)
    public static Archive<?> createApp1Deployment() {
        final EnterpriseArchive archive = ShrinkWrap
                .create(EnterpriseArchive.class, DEP_APP1 + ".ear")
                .addAsModule(createWar())
                .addAsModule(createBeanJar())
                .addAsLibrary(createLogLibrary())
                .addAsManifestResource(InterDeploymentDependenciesEarTestCase.class.getPackage(), "application.xml",
                        "application.xml");
        return archive;
    }

    /**
     * Creates app2.ear deployment.
     *
     * @return
     */
    @Deployment(name = DEP_APP2, managed = false)
    public static Archive<?> createApp2Deployment() {
        final EnterpriseArchive archive = ShrinkWrap
                .create(EnterpriseArchive.class, DEP_APP2 + ".ear")
                .addAsLibrary(createLogLibrary())
                .addAsModule(createBeanJar())
                .addAsManifestResource(InterDeploymentDependenciesEarTestCase.class.getPackage(), "jboss-all.xml",
                        "jboss-all.xml").addAsModule(createBeanJar());
        return archive;
    }

    /**
     * Tests enterprise application dependencies.
     *
     * @throws NamingException
     */
    @Test
    public void test() throws NamingException {
        try {
            deployer.deploy(DEP_APP2);
            fail("Application deployment must fail if the dependencies are not satifsied.");
        } catch (Exception e) {
            LOGGER.debug("Expected fail", e);
        } finally {
            deployer.undeploy(DEP_APP2);
        }
        deployer.deploy(DEP_APP1);
        deployer.deploy(DEP_APP2);

        final LogAccess helloApp1 = lookupEJB(DEP_APP1);
        final LogAccess helloApp2 = lookupEJB(DEP_APP2);
        assertEquals(SleeperContextListener.class.getSimpleName() + LogAccessBean.class.getSimpleName(), helloApp1.getLog());
        assertEquals(LogAccessBean.class.getSimpleName(), helloApp2.getLog());
        deployer.undeploy(DEP_APP1);
        try {
            helloApp2.getLog();
            fail("Calling EJB from dependent application should fail");
        } catch (IllegalStateException e) {
            //OK
        }
        deployer.undeploy(DEP_APP2);
    }

    @Test
    public void testWithRestart() throws NamingException {
        try {
            deployer.deploy(DEP_APP2);
            fail("Application deployment must fail if the dependencies are not satifsied.");
        } catch (Exception e) {
            LOGGER.debug("Expected fail", e);
        } finally {
            deployer.undeploy(DEP_APP2);
        }
        deployer.deploy(DEP_APP1);
        deployer.deploy(DEP_APP2);

        LogAccess helloApp1 = lookupEJB(DEP_APP1);
        LogAccess helloApp2 = lookupEJB(DEP_APP2);
        assertEquals(SleeperContextListener.class.getSimpleName() + LogAccessBean.class.getSimpleName(), helloApp1.getLog());
        assertEquals(LogAccessBean.class.getSimpleName(), helloApp2.getLog());

        deployer.undeploy(DEP_APP1);
        deployer.deploy(DEP_APP1);

        helloApp1 = lookupEJB(DEP_APP1);
        helloApp2 = lookupEJB(DEP_APP2);
        assertEquals(SleeperContextListener.class.getSimpleName() + LogAccessBean.class.getSimpleName(), helloApp1.getLog());
        assertEquals(LogAccessBean.class.getSimpleName(), helloApp2.getLog());

        deployer.undeploy(DEP_APP1);
        try {
            helloApp2.getLog();
            fail("Calling EJB from dependent application should fail");
        } catch (IllegalStateException e) {
            //OK
        }
        deployer.undeploy(DEP_APP2);
    }

    // Private methods -------------------------------------------------------

    /**
     * Lookups LogAccess bean for given application name.
     *
     * @param appName application name
     * @return
     * @throws NamingException
     */
    private LogAccess lookupEJB(String appName) throws NamingException {
        final Hashtable<String, String> jndiProperties = new Hashtable<String, String>();
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new InitialContext(jndiProperties);
        return (LogAccess) context.lookup("ejb:" + appName + "/" + MODULE_EJB + "/" + LogAccessBean.class.getSimpleName() + "!"
                + LogAccess.class.getName());
    }

    /**
     * Creates a shared lib with logger.
     *
     * @return
     */
    private static JavaArchive createLogLibrary() {
        return ShrinkWrap.create(JavaArchive.class, "log.jar").addClass(Log.class);
    }

    /**
     * Creates testing web-app (module for app1.ear)
     *
     * @return
     */
    private static Archive<?> createWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, MODULE_WEB + ".war")
                .setWebXML(InterDeploymentDependenciesEarTestCase.class.getPackage(), "web.xml")
                .addClass(SleeperContextListener.class);
        return archive;
    }

    /**
     * Creates a testing EJB module (for app1 and app2)
     *
     * @return
     */
    private static Archive<?> createBeanJar() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, MODULE_EJB + ".jar")
                .addClasses(LogAccess.class, LogAccessBean.class).addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }
}
