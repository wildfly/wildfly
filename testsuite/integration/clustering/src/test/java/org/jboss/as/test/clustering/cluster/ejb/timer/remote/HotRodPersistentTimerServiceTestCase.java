/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.remote;

import static org.jboss.as.test.clustering.InfinispanServerUtil.infinispanServerTestRule;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.cluster.ejb.timer.AbstractTimerServiceTestCase;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.ClassRule;
import org.junit.rules.TestRule;

/**
 * @author Paul Ferraro
 */
@ServerSetup({ InfinispanServerSetupTask.class, HotRodPersistentTimerServiceTestCase.TimerManagementSetupTask.class })
public class HotRodPersistentTimerServiceTestCase extends AbstractTimerServiceTestCase {

    @ClassRule
    public static final TestRule INFINISPAN_SERVER_RULE = infinispanServerTestRule();

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(NODE_1)
    public static Archive<?> deployment0() {
        return createArchive();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(NODE_2)
    public static Archive<?> deployment1() {
        return createArchive();
    }

    private static Archive<?> createArchive() {
        return createArchive(HotRodPersistentTimerServiceTestCase.class).addAsWebInfResource(HotRodPersistentTimerServiceTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
    }

    static class TimerManagementSetupTask extends ManagementServerSetupTask {
        TimerManagementSetupTask() {
            super(NODE_1_2, createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                        .startBatch()
                            .add("/subsystem=infinispan/cache-container=ejb/invalidation-cache=hotrod-persistent:add")
                            .add("/subsystem=infinispan/cache-container=ejb/invalidation-cache=hotrod-persistent/component=expiration:add(interval=0)")
                            .add("/subsystem=infinispan/cache-container=ejb/invalidation-cache=hotrod-persistent/component=locking:add(isolation=REPEATABLE_READ)")
                            .add("/subsystem=infinispan/cache-container=ejb/invalidation-cache=hotrod-persistent/component=transaction:add(mode=BATCH)")
                            .add("/subsystem=infinispan/cache-container=ejb/invalidation-cache=hotrod-persistent/store=hotrod:add(remote-cache-container=ejb, cache-configuration=default, fetch-state=false, shared=true, segmented=false)")
                            .add("/subsystem=distributable-ejb/infinispan-timer-management=hotrod:add(cache-container=ejb, cache=hotrod-persistent, marshaller=PROTOSTREAM)")
                        .endBatch()
                        .build())
                    .tearDownScript(createScriptBuilder()
                        .startBatch()
                            .add("/subsystem=distributable-ejb/infinispan-timer-management=hotrod:remove")
                            .add("/subsystem=infinispan/cache-container=ejb/invalidation-cache=hotrod-persistent:remove")
                        .endBatch()
                        .build())
                    .build());
        }
    }
}
