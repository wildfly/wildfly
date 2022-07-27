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
package org.jboss.as.test.clustering.single.ejb;

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
import org.jboss.as.test.clustering.single.ejb.bean.Incrementor;
import org.jboss.as.test.clustering.single.ejb.bean.IncrementorBean;
import org.jboss.as.test.clustering.single.ejb.bean.Result;
import org.jboss.as.test.clustering.single.ejb.bean.StatefulIncrementorBean;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validates legacy operation of EJB deployments when <distributable-ejb/> subsystem is removed.
 *
 * @author Richard Achmatowicz
 */
@RunWith(Arquillian.class)
@ServerSetup(DistributableEjbSubsystemLegacyOperationTestCase.ServerSetupTask.class)
public class DistributableEjbSubsystemLegacyOperationTestCase {

    private static final String MODULE_NAME = DistributableEjbSubsystemLegacyOperationTestCase.class.getSimpleName();
    private static final String APPLICATION_NAME = MODULE_NAME + ".jar";

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, APPLICATION_NAME);
        jar.addClasses(Result.class, Incrementor.class, IncrementorBean.class, StatefulIncrementorBean.class);
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
            Incrementor bean = directory.lookupStateful(StatefulIncrementorBean.class, Incrementor.class);

            // invoke on the bean to check that state is maintained using legacy cache support
            for (int i = 1; i <= 5; i++) {
                Result<Integer> invResult = bean.increment();

                Assert.assertEquals(i, invResult.getValue().intValue());
            }
        }
    }

    /*
     * A server setup task that does the following:
     * setup:
     * - add legacy cache support to ejb3 subsystem
     * - update the cache defaults to point to the legacy caches
     * - remove non-legacy caches from the ejb3 subsystem
     * - remove the distributable-ejb subsystem
     * teardown:
     * - restore distributable-ejb subsystem
     * - restore non-legacy cache support
     * - update the cache defaults to point again to the non-legacy caches
     * - remove legacy cache support from ejb3 subsystem
     */
    public static class ServerSetupTask extends ManagementServerSetupTask {
        public ServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                        .startBatch()
                            // add legacy cache support
                            .add("/subsystem=ejb3/cache=legacy-simple:add()")
                            .add("/subsystem=ejb3/passivation-store=infinispan:add(cache-container=ejb, max-size=10000")
                            .add("/subsystem=ejb3/cache=legacy-distributable:add(passivation-store=infinispan,aliases=[passivating,clustered])")
                            // update cache defaults, now to legacy caches
                            .add("/subsystem=ejb3:write-attribute(name=default-sfsb-cache, value=legacy-simple")
                            .add("/subsystem=ejb3:write-attribute(name=default-sfsb-passivation-disabled-cache, value=legacy-simple")
                            // remove non-legacy caches
                            .add("/subsystem=ejb3/simple-cache=simple:remove(){allow-resource-service-restart=true}")
                            .add("/subsystem=ejb3/distributable-cache=distributable:remove(){allow-resource-service-restart=true}")
                            // remove distributable-ejb subsystem
                            .add("/subsystem=distributable-ejb:remove()")
                        .endBatch()
                        .build())
                    .tearDownScript(createScriptBuilder()
                        .startBatch()
                            // add back distributable-ejb subsystem
                            .add("/subsystem=distributable-ejb:add(default-bean-management=default)")
                            .add("/subsystem=distributable-ejb/infinispan-bean-management=default:add(cache-container=ejb,cache=passivation,max-active-beans=10000)")
                            .add("/subsystem=distributable-ejb/client-mappings-registry=local:add()")
                            // add back non-legacy caches
                            .add("/subsystem=ejb3/simple-cache=simple:add()")
                            .add("/subsystem=ejb3/distributable-cache=distributable:add(bean-management=default)")
                            // reinstate cache defaults to non-legacy caches
                            .add("/subsystem=ejb3:write-attribute(name=default-sfsb-cache, value=simple")
                            .add("/subsystem=ejb3:write-attribute(name=default-sfsb-passivation-disabled-cache, value=simple")
                            // remove legacy cache support
                            .add("/subsystem=ejb3/cache=legacy-simple:remove(){allow-resource-service-restart=true}")
                            .add("/subsystem=ejb3/cache=legacy-distributable:remove(){allow-resource-service-restart=true}")
                            .add("/subsystem=ejb3/passivation-store=infinispan:remove(){allow-resource-service-restart=true}")
                        .endBatch()
                        .build())
                    .build());
        }
    }
}
