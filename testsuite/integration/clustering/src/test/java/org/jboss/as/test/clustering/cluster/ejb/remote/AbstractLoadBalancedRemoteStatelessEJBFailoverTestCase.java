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
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.SecureStatelessIncrementorBean;
import org.jboss.as.test.clustering.cluster.ejb.remote.bean.StatelessIncrementorBean;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PropertyPermission;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Validates failover behavior of a remotely accessed @Stateless EJB behind a load balancer.
 *
 * The test scenario is as follows:
 * - an EJB/HTTP client
 * - an Undertow-based modcluster load balancer
 * - two clustered Wildfly servers with Stateless EJBs deployed
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
@ServerSetup(AbstractLoadBalancedRemoteStatelessEJBFailoverTestCase.ServerSetupTask.class)
public abstract class AbstractLoadBalancedRemoteStatelessEJBFailoverTestCase extends AbstractClusteringTestCase {

    protected static final Logger log = Logger.getLogger(AbstractLoadBalancedRemoteStatelessEJBFailoverTestCase.class.getSimpleName());

    protected static final int LB_OFFSET = 500;
    private static final int COUNT = 20;
    private static final long LOAD_BALANCER_WAIT = TimeoutUtil.adjust(5000);
    private static final long CLIENT_TOPOLOGY_UPDATE_WAIT = TimeoutUtil.adjust(5000);
    private static final long INVOCATION_WAIT = TimeoutUtil.adjust(10);

    static Archive<?> createDeployment(String moduleName) {
        return ShrinkWrap.create(JavaArchive.class, moduleName + ".jar")
                .addPackage(EJBDirectory.class.getPackage())
                .addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatelessIncrementorBean.class, SecureStatelessIncrementorBean.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new PropertyPermission(NODE_NAME_PROPERTY, "read")), "permissions.xml")
                ;
    }

    private final ExceptionSupplier<EJBDirectory, Exception> directoryProvider;

    AbstractLoadBalancedRemoteStatelessEJBFailoverTestCase(ExceptionSupplier<EJBDirectory, Exception> directoryProvider) {
        super(Set.of(LOAD_BALANCER_1, NODE_1, NODE_2), Set.of(DEPLOYMENT_1, DEPLOYMENT_2));
        this.directoryProvider = directoryProvider;
    }

    @Test
    public void test() throws Exception {
        log.info("Running test case test()");

        // Allow sufficient time for the load balancer + cluster configuration to initialise
        Thread.sleep(LOAD_BALANCER_WAIT);

        try (EJBDirectory directory = this.directoryProvider.get()) {
            Incrementor bean = directory.lookupStateless(StatelessIncrementorBean.class, Incrementor.class);

            // Allow sufficient time for client to receive full topology
            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            List<String> results = new ArrayList<>(COUNT);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.add(result.getNode());
                Thread.sleep(INVOCATION_WAIT);
            }

            for (String node : NODE_1_2) {
                int frequency = Collections.frequency(results, node);
                assertTrue(frequency + " invocations were routed to " + node, frequency > 0);
            }

            undeploy(DEPLOYMENT_1);

            // Allow sufficient time for client to receive new topology
            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
                Thread.sleep(INVOCATION_WAIT);
            }

            Assert.assertEquals(0, Collections.frequency(results, NODE_1));
            Assert.assertEquals(COUNT, Collections.frequency(results, NODE_2));

            deploy(DEPLOYMENT_1);

            // Allow sufficient time for client to receive new topology
            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
                Thread.sleep(INVOCATION_WAIT);
            }

            for (String node : NODE_1_2) {
                int frequency = Collections.frequency(results, node);
                assertTrue(frequency + " invocations were routed to " + node, frequency > 0);
            }

            stop(NODE_2);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
                Thread.sleep(INVOCATION_WAIT);
            }

            Assert.assertEquals(COUNT, Collections.frequency(results, NODE_1));
            Assert.assertEquals(0, Collections.frequency(results, NODE_2));

            start(NODE_2);

            // Allow sufficient time for client to receive new topology
            Thread.sleep(CLIENT_TOPOLOGY_UPDATE_WAIT);

            for (int i = 0; i < COUNT; ++i) {
                Result<Integer> result = bean.increment();
                results.set(i, result.getNode());
                Thread.sleep(INVOCATION_WAIT);
            }

            for (String node : NODE_1_2) {
                int frequency = Collections.frequency(results, node);
                assertTrue(frequency + " invocations were routed to " + node, frequency > 0);
            }
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
