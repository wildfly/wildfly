/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.remote;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Incrementor;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.IncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.Result;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.function.ExceptionSupplier;

import java.util.PropertyPermission;
import java.util.Set;

/**
 * Validates failover behavior of a remotely accessed @Stateful EJB behind a load balancer.
 *
 * The test scenario is as follows:
 * - an EJB/HTTP client
 * - an Undertow-based modcluster load balancer
 * - two clustered Wildfly servers with Stateful EJBs deployed
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
@ServerSetup(AbstractLoadBalancedRemoteStatefulEJBFailoverTestCase.ServerSetupTask.class)
public abstract class AbstractLoadBalancedRemoteStatefulEJBFailoverTestCase extends AbstractClusteringTestCase {
    protected static final Logger log = Logger.getLogger(AbstractLoadBalancedRemoteStatefulEJBFailoverTestCase.class);

    protected static final int LB_OFFSET = 500;
    private static final int COUNT = 20;
    private static final long LOAD_BALANCER_WAIT = TimeoutUtil.adjust(5000);
    private static final long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);

    static Archive<?> createDeployment(String moduleName) {
        return ShrinkWrap.create(JavaArchive.class, moduleName + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatefulIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    private final ExceptionSupplier<EJBDirectory, Exception> directoryProvider;

    /**
     * Constructs a clustering test case consisting of a load balancer and two backend nodes.
     *
     * The load balancer and its two backend nodes are configured in the inner class ServerSetupTask.
     * The directory provider must provide a proxy that is configured for EJB/HTTP and which points to the load balancer.
     *
     * @param directoryProvider a provider for the EJBDirectory instance used to create the test case proxies.
     */
    protected AbstractLoadBalancedRemoteStatefulEJBFailoverTestCase(ExceptionSupplier<EJBDirectory, Exception> directoryProvider) {
        super(Set.of(LOAD_BALANCER_1, NODE_1, NODE_2), Set.of(DEPLOYMENT_1, DEPLOYMENT_2));
        this.directoryProvider = directoryProvider;
    }

    @Test
    public void test() throws Exception {
        log.info("Running test case test()");

        // Allow sufficient time for the load balancer + cluster configuration to initialise
        Thread.sleep(LOAD_BALANCER_WAIT);

        try (EJBDirectory directory = this.directoryProvider.get()) {

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

    /**
     * A server setup script to configure each backend node to register with the load balancer.
     */
    static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=modcluster/proxy=default:write-attribute(name=advertise, value=false)")
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy1:add(host=localhost, port=8590)")
                            .add("/subsystem=modcluster/proxy=default:list-add(name=proxies, value=proxy1)")
                            .add("/subsystem=undertow/configuration=filter/custom-filter=request-dumper:add(class-name=\"io.undertow.server.handlers.RequestDumpingHandler\", module=\"io.undertow.core\")")
                            .add("/subsystem=undertow/server=default-server/host=default-host/filter-ref=request-dumper:add")
                            .endBatch()
                            .build()
                    )
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=modcluster/proxy=default:list-remove(name=proxies, value=proxy1)")
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy1:remove")
                            .add("/subsystem=modcluster/proxy=default:write-attribute(name=advertise, value=true)")
                            .add("/subsystem=undertow/server=default-server/host=default-host/filter-ref=request-dumper:remove")
                            .add("/subsystem=undertow/configuration=filter/custom-filter=request-dumper:remove")
                            .endBatch()
                            .build()
                    )
                    .build()
            );
        }
    }
}
