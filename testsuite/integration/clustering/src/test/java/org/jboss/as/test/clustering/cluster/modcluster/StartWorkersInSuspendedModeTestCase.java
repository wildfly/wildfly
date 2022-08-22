/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.modcluster;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADDRESS;
import static org.jboss.as.controller.client.helpers.ClientConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.RECURSIVE;
import static org.jboss.as.controller.client.helpers.ClientConstants.STATUS;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies behavior of mod_cluster interaction when a worker node is started in suspended mode.
 *
 * @author Bartosz Spyrko-Smietanko
 * @author Radoslav Husar
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(StartWorkersInSuspendedModeTestCase.ServerSetupTask.class)
public class StartWorkersInSuspendedModeTestCase extends AbstractClusteringTestCase {

    private static final String MODULE_NAME = StartWorkersInSuspendedModeTestCase.class.getSimpleName();
    private static final String DEPLOYMENT_NAME = MODULE_NAME + ".war";
    public static final long STATUS_REFRESH_TIMEOUT = 30_000;
    public static final int LB_OFFSET = 500;

    @Deployment(name = DEPLOYMENT_1, testable = false, managed = false)
    @TargetsContainer(NODE_1)
    public static WebArchive deployment() {
        WebArchive pojoWar = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME);
        pojoWar.add(new StringAsset("Hello World"), "index.html");
        return pojoWar;
    }

    public StartWorkersInSuspendedModeTestCase() {
        super(Set.of(LOAD_BALANCER_1, NODE_1), Set.of(DEPLOYMENT_1));
    }

    @Test
    public void testWorkerNodeIsRegisteredStopped() throws Exception {
        ServerReload.executeReloadAndWaitForCompletion(TestSuiteEnvironment.getModelControllerClient(), ServerReload.TIMEOUT,
                                                       false, true,
                                                       null,
                                                       -1, null);

        assertWorkerNodeContextIsStopped();
    }

    private void assertWorkerNodeContextIsStopped() throws Exception {

        ModelNode op = createOpNode("subsystem=undertow/configuration=filter/mod-cluster=load-balancer/balancer=mycluster/node=" + NODE_1, READ_RESOURCE_OPERATION);
        op.get(ADDRESS).add("context", "/" + MODULE_NAME);
        op.get(RECURSIVE).set(true);
        op.get(INCLUDE_RUNTIME).set(true);

        final ModelControllerClient client = TestSuiteEnvironment.getModelControllerClient(
           null,
           TestSuiteEnvironment.getServerAddress(),
           TestSuiteEnvironment.getServerPort() + LB_OFFSET);

        // might need to wait for mod_cluster nodes to be registered
        long start = System.currentTimeMillis();
        ModelNode modelNode = null;
        while (System.currentTimeMillis() - start < STATUS_REFRESH_TIMEOUT) {
            modelNode = client.execute(op);
            if (modelNode.has(RESULT) && modelNode.get(RESULT).has(STATUS)) {
                break;
            }
            Thread.sleep(100);
        }

        Assert.assertEquals(SUCCESS, modelNode.get(OUTCOME).asString());
        Assert.assertEquals("stopped", modelNode.get(RESULT).get(STATUS).asString());
    }

    static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(NODE_1, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=modcluster/proxy=default:write-attribute(name=advertise, value=false)")
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy1:add(host=localhost, port=8590)")
                            .add("/subsystem=modcluster/proxy=default:list-add(name=proxies, value=proxy1)")
                            .endBatch()
                            .build()
                    )
                    .tearDownScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=modcluster/proxy=default:list-remove(name=proxies, value=proxy1)")
                            .add("/socket-binding-group=standard-sockets/remote-destination-outbound-socket-binding=proxy1:remove")
                            .add("/subsystem=modcluster/proxy=default:write-attribute(name=advertise, value=true)")
                            .endBatch()
                            .build())
                    .build()
            );
        }
    }

}
