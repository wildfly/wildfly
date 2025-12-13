/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful.passivation;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.PassivationEventTracker;
import org.jboss.as.test.clustering.PassivationEventTrackerBean;
import org.jboss.as.test.clustering.PassivationEventTrackerUtil;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.Incrementor;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.IncrementorBean;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.Result;
import org.jboss.as.test.clustering.single.ejb.stateful.passivation.bean.PassivatingIncrementor;
import org.jboss.as.test.clustering.single.ejb.stateful.passivation.bean.PassivatingIncrementorBean;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests idle time-based (idle-threshold) passivation for stateful session beans.
 * Validates that beans are passivated after the configured idle threshold and properly activated when accessed again.
 *
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
@ServerSetup({
        SnapshotRestoreSetupTask.class, // MUST be first!
        IdleThresholdStatefulSessionBeanPassivationTestCase.ServerSetupTask.class
})
public class IdleThresholdStatefulSessionBeanPassivationTestCase {

    static class ServerSetupTask extends ManagementServerSetupTask {
        ServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .startBatch()
                            .add("/subsystem=ejb3:write-attribute(name=default-sfsb-cache, value=distributable)")
                            .add("/subsystem=distributable-ejb/infinispan-bean-management=default:undefine-attribute(name=max-active-beans)")
                            .add("/subsystem=distributable-ejb/infinispan-bean-management=default:write-attribute(name=idle-threshold, value=PT1S)")
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
                .addClasses(Result.class, Incrementor.class, PassivatingIncrementor.class, PassivatingIncrementorBean.class, IncrementorBean.class, PassivationEventTrackerUtil.class)
                .addClasses(PassivationEventTracker.class, PassivationEventTrackerBean.class, PassivationEventTrackerUtil.class)
                ;
    }

    private EJBDirectory directory;

    @Before
    public void before() throws Exception {
        this.directory = new RemoteEJBDirectory(MODULE_NAME);
    }

    @After
    public void after() throws Exception {
        this.directory.close();
    }

    // Maximum passivation polling duration after which the test will abort waiting for the passivation event
    // This needs to be longer than idle-threshold (1s) to allow time for passivation to occur
    private static final Duration PASSIVATION_POLLING_DURATION = Duration.ofSeconds(TimeoutUtil.adjust(10));
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    @Test
    public void test(@ArquillianResource ManagementClient managementClient) throws Exception {
        PassivatingIncrementor bean = this.directory.lookupStateful(PassivatingIncrementorBean.class, PassivatingIncrementor.class);
        PassivationEventTracker eventTracker = this.directory.lookupStateless(PassivationEventTrackerBean.class, PassivationEventTracker.class);

        // Clear any previous events
        eventTracker.clearPassivationEvents();

        // Step 1: Set initial state on the bean
        String beanIdentifier = bean.getIdentifier();
        assertEquals("Initial value should be 1", 1, bean.increment().getValue().intValue());

        // Ensure no passivation/activation event has occurred yet
        Map.Entry<Object, PassivationEventTrackerUtil.EventType> event = eventTracker.pollPassivationEvent();
        assertNull(event);

        // Step 2: Poll for PASSIVATION event from the server (without accessing the bean directly as to not activate it)
        event = Awaitility.await("stateful bean to passivate")
                .atMost(PASSIVATION_POLLING_DURATION)
                .pollInterval(POLL_INTERVAL)
                .until(eventTracker::pollPassivationEvent, Objects::nonNull);

        // Step 3: Poll to verify bean was passivated due to idle timeout
        assertEquals("Bean should have been passivated due to idle timeout", PassivationEventTrackerUtil.EventType.PASSIVATION, event.getValue());

        // Step 4: Access the bean - this should trigger activation
        // The bean should have been passivated while idle
        assertEquals("Value should be preserved and incremented after passivation", 2, bean.increment().getValue().intValue());

        // Verify activation event
        event = eventTracker.pollPassivationEvent();
        assertNotNull("Activation event should be present", event);
        assertEquals("Event should be for the correct bean", beanIdentifier, event.getKey());
        assertEquals("Event should be ACTIVATION", PassivationEventTrackerUtil.EventType.ACTIVATION, event.getValue());

        // Step 5: Test a second idle cycle
        assertEquals("Value should be incremented to 3", 3, bean.increment().getValue().intValue());

        // Step 2: Poll for PASSIVATION event from the server (without accessing the bean directly as to not activate it)
        event = Awaitility.await("stateful bean to passivate")
                .atMost(PASSIVATION_POLLING_DURATION)
                .pollInterval(POLL_INTERVAL)
                .until(eventTracker::pollPassivationEvent, Objects::nonNull);

        assertNotNull("Bean should have been passivated again after second idle timeout", event);

        // Access the bean again - should trigger second activation
        assertEquals("Value should be preserved and incremented after second passivation", 4, bean.increment().getValue().intValue());

        // Verify second activation event
        event = eventTracker.pollPassivationEvent();
        assertNotNull("Second activation event should be present", event);
        assertEquals("Event should be for the correct bean", beanIdentifier, event.getKey());
        assertEquals("Event should be ACTIVATION", PassivationEventTrackerUtil.EventType.ACTIVATION, event.getValue());

        // Cleanup
        bean.remove();
        eventTracker.clearPassivationEvents();
    }

}
