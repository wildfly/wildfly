/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb.remote;

import java.util.Properties;
import java.util.PropertyPermission;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that Remoting connectors which are *not* listed in the connectors list will not process EJB client invocations.
 * They throw a ServiceOpenExecption which is procssed by the client as a failed invocation attempt.
 *
 * @author Richard Achmatowicz
 */
@ServerSetup(ConnectorsListRemoteEJBTestCase.ServerSetupTask.class)
@RunWith(Arquillian.class)
public class ConnectorsListRemoteEJBTestCase extends AbstractClusteringTestCase {
    static final Logger LOGGER = Logger.getLogger(org.jboss.as.test.clustering.cluster.ejb.remote.ConnectorsListRemoteEJBTestCase.class);
    private static final String MODULE_NAME = ConnectorsListRemoteEJBTestCase.class.getSimpleName();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatelessIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    @Test
    public void testInvocationOnMisconfiguredConnector() throws Exception {
        LOGGER.info("testInvocationOnMisconfiguredConnector() - start");
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME, getProperties())) {
            Incrementor bean = directory.lookupStateless(StatelessIncrementorBean.class, Incrementor.class);
            // this invocation should fail on both nodes as it targets a port which is not correctly congigured
            LOGGER.info("Invoking on bean");
            bean.increment();
            Assert.fail("Expected Exception but didn't catch it");
        } catch (Exception e) {
            LOGGER.info("Got exception when invoking on bean: e =  " + e.getMessage());
            Throwable[] suppressed = e.getSuppressed();
            for (Throwable throwable : suppressed) {
                LOGGER.info("Got suppressed Throwable with exception: t = " + throwable);
            }
            // assert the correct number
            Assert.assertEquals("Should be one supressed exception", suppressed.length, 1);
            // assert the correct message
            Assert.assertTrue("Cause of Throwable should be service refused", suppressed[0].getMessage().contains("Service refused"));
        } finally {
            LOGGER.info("testInvocationOnMisconfiguredConnector() - end");
        }
    }

    /*
     * Set up JNDI properties to support EJB client invocation on a legacy Remoting URL remote://localhost:4447
     */
    private static Properties getProperties() {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, org.wildfly.naming.client.WildFlyInitialContextFactory.class.getName());
        props.put(Context.PROVIDER_URL, String.format("%s://%s:%s", "remote", "localhost", "4447"));
        return props ;
    }

    /*
     * Set up the server to add in a new Remoting connector on port 4447
     */
    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder.node(TWO_NODES)
                    .setup("/socket-binding-group=standard-sockets/socket-binding=remoting:add(port=4447)")
                    .setup("/subsystem=remoting/connector=remoting-connector:add(socket-binding=remoting, security-realm=ApplicationRealm)")
                    .setup("/subsystem=remoting/connector=remoting-connector/property=SSL_ENABLED:add(value=false)")
                    // don't add the new cpnnector to the connectors list; this is a deliberate misconfiguration
                    .teardown("/subsystem=ejb3/service=remote:list-remove(name=connectors, value=remoting-connector)")
                    .teardown("/subsystem=remoting/connector=remoting-connector:remove")
                    .teardown("/socket-binding-group=standard-sockets/socket-binding=remoting:remove")
            ;
        }
    }
}
