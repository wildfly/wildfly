/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful.passivation;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.PassivationEventCollector;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.IncrementorBean;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.Result;
import org.jboss.as.test.clustering.single.ejb.stateful.passivation.bean.PassivatingIncrementor;
import org.jboss.as.test.clustering.single.ejb.stateful.passivation.bean.PassivatingIncrementorBean;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests idle time-based (idle-threshold) passivation for stateful session beans.
 * Validates that beans are passivated after the configured idle threshold and properly activated when accessed again.
 *
 * @author Radoslav Husar
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(IdleThresholdStatefulSessionBeanPassivationTestCase.ServerSetupTask.class)
public class IdleThresholdStatefulSessionBeanPassivationTestCase {
    private static final Duration IDLE_THRESHOLD = Duration.ofSeconds(2L);

    static class ServerSetupTask extends ManagementServerSetupTask {
        ServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=ejb3:write-attribute(name=default-sfsb-cache, value=distributable)")
                            .add("/subsystem=distributable-ejb/infinispan-bean-management=default:undefine-attribute(name=max-active-beans)")
                            .add("/subsystem=distributable-ejb/infinispan-bean-management=default:write-attribute(name=idle-threshold, value=%s)", IDLE_THRESHOLD)
                            .endBatch()
                            .build())
                    .build());
        }
    }

    private static final String MODULE_NAME = IdleThresholdStatefulSessionBeanPassivationTestCase.class.getSimpleName();
    private static final String APPLICATION_NAME = MODULE_NAME + ".jar";

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class, APPLICATION_NAME)
                .addClasses(Result.class, Incrementor.class, PassivationEventCollector.class, PassivatingIncrementor.class, PassivatingIncrementorBean.class, IncrementorBean.class)
                ;
    }

    @Test
    void test(@ArquillianResource ManagementClient managementClient) throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
            PassivatingIncrementor bean = directory.lookupStateful(PassivatingIncrementorBean.class, PassivatingIncrementor.class);
            try {
                assertThat(bean.getPassivations()).isEmpty();
                assertThat(bean.getActivations()).isEmpty();

                for (int i = 0; i < 5; ++i) {
                    // Wait until idle
                    Thread.sleep(IDLE_THRESHOLD.toMillis());

                    Instant preInvoke = Instant.now();
                    Deque<Instant> passivations = bean.getPassivations();
                    Instant postInvoke = Instant.now();
                    Deque<Instant> activations = bean.getActivations();

                    assertThat(passivations).hasSize(i + 1);
                    assertThat(activations).hasSize(i + 1);

                    Instant lastPassivation = passivations.pollLast();
                    Instant lastActivation = activations.pollLast();
                    if (!activations.isEmpty()) {
                        assertThat(lastPassivation).isAfterOrEqualTo(activations.getLast().plus(IDLE_THRESHOLD));
                    }
                    assertThat(lastPassivation).isBeforeOrEqualTo(lastActivation);
                    assertThat(lastActivation).isAfter(preInvoke).isBefore(postInvoke);
                }
            } finally {
                bean.remove();
            }
        }
    }
}
