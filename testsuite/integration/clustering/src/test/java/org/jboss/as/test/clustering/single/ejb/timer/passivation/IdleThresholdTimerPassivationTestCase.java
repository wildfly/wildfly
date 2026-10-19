/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.timer.passivation;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;

import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.PassivationEventCollector;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.clustering.single.ejb.timer.passivation.bean.PassivationTimeoutCollectorBean;
import org.jboss.as.test.clustering.single.ejb.timer.passivation.bean.PassivationTimeoutCollector;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that EJB timers are passivated after the configured idle threshold and that
 * serializable TimerInfo objects are correctly preserved through passivation/activation cycles.
 *
 * @author Radoslav Husar
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(IdleThresholdTimerPassivationTestCase.ServerSetupTask.class)
public class IdleThresholdTimerPassivationTestCase {
    static final Duration IDLE_THRESHOLD = PassivationTimeoutCollector.TIMEOUT_INTERVAL.dividedBy(2L);

    static class ServerSetupTask extends ManagementServerSetupTask {
        ServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            // These must be unset before using default timer management
                            .add("/subsystem=ejb3/service=timer-service:undefine-attribute(name=thread-pool-name)")
                            .add("/subsystem=ejb3/service=timer-service:undefine-attribute(name=default-data-store)")
                            .add("/subsystem=ejb3/service=timer-service:write-attribute(name=default-transient-timer-management, value=transient)")
                            .add("/subsystem=ejb3/service=timer-service:write-attribute(name=default-persistent-timer-management, value=persistent)")
                            .add("/subsystem=distributable-ejb/infinispan-timer-management=transient:write-attribute(name=idle-threshold, value=%s)", IDLE_THRESHOLD)
                            .endBatch()
                            .build())
                    .build());
        }
    }

    private static final String MODULE_NAME = IdleThresholdTimerPassivationTestCase.class.getSimpleName();
    private static final String APPLICATION_NAME = MODULE_NAME + ".jar";

    @Deployment(testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class, APPLICATION_NAME)
                .addPackage(PassivationTimeoutCollector.class.getPackage())
                .addClasses(PassivationEventCollector.class)
                ;
    }

    private static final Duration MAX_POLL_DURATION = PassivationTimeoutCollector.TIMEOUT_INTERVAL.multipliedBy(4L);
    private static final Duration POLL_INTERVAL = PassivationTimeoutCollector.TIMEOUT_INTERVAL.dividedBy(10L);

    @Test
    void test() throws Exception {
        try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {

            PassivationTimeoutCollector bean = directory.lookupSingleton(PassivationTimeoutCollectorBean.class, PassivationTimeoutCollector.class);

            for (int i = 0; i < 5; ++i) {
                int expectedTimeouts = i + 1;
                Deque<Instant> timeouts = Awaitility.await("Await receipt of @Timeout event")
                        .atMost(MAX_POLL_DURATION)
                        .pollInSameThread()
                        .pollDelay(Duration.ZERO)
                        .pollInterval(POLL_INTERVAL)
                        .until(bean::getTimeouts, events -> events.size() == expectedTimeouts);

                Deque<Instant> passivations = bean.getPassivations();
                Deque<Instant> activations = bean.getActivations();

                assertThat(passivations).hasSize(timeouts.size());
                assertThat(activations).hasSize(timeouts.size());

                Instant lastTimeout = timeouts.pollLast();
                Instant previousTimeout = timeouts.pollLast();

                Instant lastPassivation = passivations.pollLast();
                Instant lastActivation = activations.pollLast();
                if (previousTimeout != null) {
                    assertThat(lastPassivation).isAfterOrEqualTo(previousTimeout.plus(IDLE_THRESHOLD)).isAfterOrEqualTo(activations.getLast().plus(IDLE_THRESHOLD));
                }
                assertThat(lastPassivation).isBefore(lastTimeout);
                assertThat(lastActivation).isBeforeOrEqualTo(lastTimeout).isAfter(lastPassivation);
            }
        }
    }
}
