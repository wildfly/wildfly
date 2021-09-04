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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.function.ExceptionSupplier;


import javax.naming.Context;

/**
 * A test of failover when both legacy remoting connector and HTTP Upgrade connector are enabled.
 * @author Richard Achmatowicz
 */

@ServerSetup(TwoConnectorsEJBFailoverTestCase.ServerSetupTask.class)
@RunWith(Arquillian.class)
public class TwoConnectorsEJBFailoverTestCase extends AbstractClusteringTestCase {

    private static final int COUNT = 20;
    private static final long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);
    private static final String MODULE_NAME = TwoConnectorsEJBFailoverTestCase.class.getSimpleName();

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
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatefulIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    public TwoConnectorsEJBFailoverTestCase() {
        super();
    }

    /*
     * Set up JNDI properties to support HTTP based Jakarta Enterprise Beans client invocations via HTTP Upgrade
     *
     * NOTE: there are several ways to connect to the Jakarta Enterprise Beans container on the server:
     *   protocol           URL
     *   remoting           remote://localhost:4447
     *   HTTP Upgrade       remote+http://localhost:8080
     *   pure HTTP          http://localhost:8080/wildfly-sevices
     */
    private static Properties getProperties(boolean legacy) {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, org.wildfly.naming.client.WildFlyInitialContextFactory.class.getName());
        if (legacy) {
            props.put(Context.PROVIDER_URL, String.format("%s://%s:%s", "remote", "localhost", "4447"));
        } else {
            //props.put(Context.PROVIDER_URL, String.format("%s://%s:%s/wildfly-services", "https", "localhost", "8080"));
            props.put(Context.PROVIDER_URL, String.format("%s://%s:%s", "remote+http", "localhost", "8080"));
        }
        return props ;
    }


    /*
     * Run a failover test where the client communicates with the server via HTTP Upgrade over port 8080/8081
     */
    @Test
    public void testEJBClientUsingHttpUpgradeProtocol() throws Exception {
        log.infof(MODULE_NAME+ " : testing failover with client using HTTP Upgrade");
        test(() -> new RemoteEJBDirectory(MODULE_NAME, getProperties(false)));
    }

    /*
     * Run a failover test where the client communicates with the server via legacy Remoting protocol over port 4447/4448
     */
    @Test
    public void testEJBClientUsingLegacyRemotingProtocol() throws Exception {
        log.infof(MODULE_NAME + " : testing failover with client using legacy Remoting");
        test(() -> new RemoteEJBDirectory(MODULE_NAME, getProperties(true)));
    }


    /*
     * A SFSB failover test which accepts an EJBDirectory provider parameter to adjust client behaviour
     */
    public void test(ExceptionSupplier<EJBDirectory, Exception> directoryProvider) throws Exception {
        try (EJBDirectory directory = directoryProvider.get()) {
            Incrementor bean = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);

            Result<Integer> result = bean.increment();
            String target = result.getNode();
            int count = 1;

            Assert.assertEquals(count++, result.getValue().intValue());

            // Bean should retain weak affinity for this node
            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals(count++, result.getValue().intValue());
                Assert.assertEquals(String.valueOf(i), target, result.getNode());
            }

            undeploy(this.findDeployment(target));

            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            result = bean.increment();
            // Bean should failover to other node
            String failoverTarget = result.getNode();

            Assert.assertEquals(count++, result.getValue().intValue());
            Assert.assertNotEquals(target, failoverTarget);

            deploy(this.findDeployment(target));

            // Allow sufficient time for client to receive new topology
            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            result = bean.increment();
            String failbackTarget = result.getNode();
            Assert.assertEquals(count++, result.getValue().intValue());
            // Bean should retain weak affinity for this node
            Assert.assertEquals(failoverTarget, failbackTarget);

            result = bean.increment();
            // Bean may have acquired new weak affinity
            target = result.getNode();
            Assert.assertEquals(count++, result.getValue().intValue());

            // Bean should retain weak affinity for this node
            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals(count++, result.getValue().intValue());
                Assert.assertEquals(String.valueOf(i), target, result.getNode());
            }

            stop(target);

            // Allow sufficient time for client to receive new topology
            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            result = bean.increment();
            // Bean should failover to other node
            failoverTarget = result.getNode();

            Assert.assertEquals(count++, result.getValue().intValue());
            Assert.assertNotEquals(target, failoverTarget);

            start(target);

            // Allow sufficient time for client to receive new topology
            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            result = bean.increment();
            failbackTarget = result.getNode();
            Assert.assertEquals(count++, result.getValue().intValue());
            // Bean should retain weak affinity for this node
            Assert.assertEquals(failoverTarget, failbackTarget);

            result = bean.increment();
            // Bean may have acquired new weak affinity
            target = result.getNode();
            Assert.assertEquals(count++, result.getValue().intValue());

            // Bean should retain weak affinity for this node
            for (int i = 0; i < COUNT; ++i) {
                result = bean.increment();
                Assert.assertEquals(count++, result.getValue().intValue());
                Assert.assertEquals(String.valueOf(i), target, result.getNode());
            }

            bean.remove();
        }
    }

        /*
     * Set up the server to use both legacy and HTTP Upgrade remoting connectors
     */
    public static class ServerSetupTask extends CLIServerSetupTask {
        public ServerSetupTask() {
            this.builder.node(TWO_NODES)
                    .setup("/socket-binding-group=standard-sockets/socket-binding=remoting:add(port=4447)")
                    .setup("/subsystem=remoting/connector=remoting-connector:add(socket-binding=remoting, sasl-authentication-factory=application-sasl-authentication)")
                    .setup("/subsystem=remoting/connector=remoting-connector/property=SSL_ENABLED:add(value=false)")
                    // this step results in a capabilities error if the list is not formatted correctly for CLI
                    .setup("/subsystem=ejb3/service=remote:list-add(name=connectors, value=remoting-connector)")
                    .teardown("/subsystem=ejb3/service=remote:list-remove(name=connectors, value=remoting-connector)")
                    .teardown("/subsystem=remoting/connector=remoting-connector:remove")
                    .teardown("/socket-binding-group=standard-sockets/socket-binding=remoting:remove")
            ;
        }
    }
}
