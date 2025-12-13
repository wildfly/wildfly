/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.Result;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.TransientStatefulIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates legacy operation of EJB deployments when the distributable-ejb subsystem is removed.
 *
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
@ServerSetup({
        SnapshotRestoreSetupTask.class,
        DistributableEjbSubsystemLegacyOperationTestCase.ServerSetupTask.class
})
public class DistributableEjbSubsystemLegacyOperationTestCase {

    private static final String MODULE_NAME = DistributableEjbSubsystemLegacyOperationTestCase.class.getSimpleName();
    private static final String APPLICATION_NAME = MODULE_NAME + ".jar";

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, APPLICATION_NAME);
        jar.addPackage(Incrementor.class.getPackage());
        return jar;
    }

    @Test
    public void test(@ArquillianResource ManagementClient managementClient) throws Exception {
        // Confirm absence of distributable-ejb subsystem
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "distributable-ejb"));
        ModelNode operation = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_OPERATION, address);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        Assert.assertEquals(ModelDescriptionConstants.FAILED, result.get(ModelDescriptionConstants.OUTCOME).asString());

        // lookup the deployed stateful session bean
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            validate(directory, StatefulIncrementorBean.class);
            validate(directory, TransientStatefulIncrementorBean.class);
        }
    }

    private static void validate(EJBDirectory directory, Class<? extends Incrementor> beanClass) throws Exception {
        Incrementor bean = directory.lookupStateful(beanClass, Incrementor.class);

        // invoke on the bean to check that state is maintained using legacy cache support
        for (int i = 1; i <= 5; i++) {
            Result<Integer> invResult = bean.increment();

            Assert.assertEquals(i, invResult.getValue().intValue());
        }
    }

    /**
     * A server setup task that does the following setup:
     * - add legacy cache support to ejb3 subsystem
     * - update the cache defaults to point to the legacy caches
     * - remove non-legacy caches from the ejb3 subsystem
     * - remove the distributable-ejb subsystem
     */
    public static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                        .startBatch()
                            // add legacy cache support
                            .add("/subsystem=ejb3/cache=legacy-simple:add()")
                            .add("/subsystem=ejb3/passivation-store=infinispan:add(cache-container=ejb, max-size=10000)")
                            .add("/subsystem=ejb3/cache=legacy-distributable:add(passivation-store=infinispan, aliases=[passivating, clustered])")
                            // update cache defaults, now to legacy caches
                            .add("/subsystem=ejb3:write-attribute(name=default-sfsb-cache, value=legacy-distributable)")
                            .add("/subsystem=ejb3:write-attribute(name=default-sfsb-passivation-disabled-cache, value=legacy-simple)")
                            // remove non-legacy caches
                            .add("/subsystem=ejb3/simple-cache=simple:remove(){allow-resource-service-restart=true}")
                            .add("/subsystem=ejb3/distributable-cache=distributable:remove(){allow-resource-service-restart=true}")
                            // remove distributable-ejb subsystem
                            .add("/subsystem=distributable-ejb:remove()")
                        .endBatch()
                        .build())
                    .build());
        }
    }
}
