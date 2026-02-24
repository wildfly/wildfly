/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.timer.passivation;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.clustering.PassivationEventTracker;
import org.jboss.as.test.clustering.PassivationEventTrackerBean;
import org.jboss.as.test.clustering.PassivationEventTrackerUtil;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.clustering.single.ejb.timer.passivation.bean.TimerTracker;
import org.jboss.as.test.clustering.single.ejb.timer.passivation.bean.TimerTrackerBean;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.wildfly.test.stabilitylevel.StabilityServerSetupSnapshotRestoreTasks;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that EJB timers are passivated after the configured idle threshold and that
 * serializable TimerInfo objects are correctly preserved through passivation/activation cycles.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup({
        SnapshotRestoreSetupTask.class, // MUST be first!
        StabilityServerSetupSnapshotRestoreTasks.Community.class,
        IdleThresholdTimerPassivationTestCase.ServerSetupTask.class,
})
public class IdleThresholdTimerPassivationTestCase {

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
                            .add("/subsystem=distributable-ejb/infinispan-timer-management=transient:write-attribute(name=idle-threshold, value=PT1S)")
                            .add("/subsystem=distributable-ejb/infinispan-timer-management=persistent:write-attribute(name=idle-threshold, value=PT1S)")
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
                .addPackage(TimerTracker.class.getPackage())
                .addClasses(PassivationEventTracker.class, PassivationEventTrackerBean.class, PassivationEventTrackerUtil.class)
                ;
    }

    // Maximum passivation polling duration after which the test will abort waiting for the passivation event
    // This needs to be longer than idle-threshold (1s) to allow time for passivation to occur
    private static final Duration PASSIVATION_POLLING_DURATION = Duration.ofSeconds(TimeoutUtil.adjust(5));
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    private EJBDirectory directory;

    @Before
    public void before() throws Exception {
        this.directory = new RemoteEJBDirectory(MODULE_NAME);
    }

    @After
    public void after() throws Exception {
        this.directory.close();
    }

    @Test
    public void test() throws Exception {
        TimerTracker bean = this.directory.lookupSingleton(TimerTrackerBean.class, TimerTracker.class);

        // First, clear any existing events on the server; e.g. from previous failed run
        bean.clearPassivationEvents();

        // Step 1: Create a timer with serializable info
        // n.b. This cannot be a persistent timer because it would be immediately serialized!
        // n.b. TimerInfo is created server-side and never sent to the client as that would trigger serialization logic
        String timerName = "test-timer";
        bean.createTimer(timerName, false, Duration.ofSeconds(5));

        // Step 2: Wait for idle timeout - timer should be passivated
        // but actually do NOT wait – keep polling for the event; this makes the test faster and more resilient as opposed to time-based approach

        // Step 3: Poll for PASSIVATION event from the server (without accessing the timer directly as to not activate it)
        Map.Entry<Object, PassivationEventTrackerUtil.EventType> event = Awaitility.await("timer to passivate")
                .atMost(PASSIVATION_POLLING_DURATION)
                .pollInterval(POLL_INTERVAL)
                .until(bean::pollPassivationEvent, Objects::nonNull);

        assertNotNull("Should have passivation event", event);
        assertEquals("Event should be for correct timer", timerName, event.getKey());
        assertEquals("Event should be PASSIVATION", PassivationEventTrackerUtil.EventType.PASSIVATION, event.getValue());

        // Clear remaining passivation events triggered by size calculation in ByteBufferMarshaller / ByteBufferMarshalledValue
        bean.pollPassivationEvent();
        bean.pollPassivationEvent();
        bean.pollPassivationEvent();

        // Step 4: Wait for timer to actually fire and thus trigger activation
        // again - don't wait, but keep polling

        // Step 5: Poll for ACTIVATION event from the server
        event = Awaitility.await("timer to activate")
                .atMost(PASSIVATION_POLLING_DURATION)
                .pollInterval(POLL_INTERVAL)
                .until(bean::pollPassivationEvent, Objects::nonNull);

        assertNotNull("Should have activation event", event);
        assertEquals("Event should be for correct timer", timerName, event.getKey());
        assertEquals("Event should be ACTIVATION", PassivationEventTrackerUtil.EventType.ACTIVATION, event.getValue());

        // Step 6: Remaining cleanup happens in TimerTrackerBean#preDestroy
    }


}
