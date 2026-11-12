/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.locking.reentrant;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;

/**
 * Regression test for WFLY-22385.
 *
 * Verifies that reentrant invocations on the same SFSB instance via
 * {@link jakarta.ejb.SessionContext#getBusinessObject(Class)} complete
 * correctly and do not produce spurious WFLYEJB0487 log errors.
 *
 * Before the fix, the invocation sync state was prematurely reset to
 * NO_INVOCATION (0) by the innermost reentrant frame. Each outer frame
 * then saw an unexpected state on unwind and logged WFLYEJB0487.
 * With the fix, the interceptor captures the previous sync state on entry;
 * only the outermost frame (which entered with NO_INVOCATION) resets the
 * state back on unwind.
 */
@RunWith(Arquillian.class)
public class ReentrantSFSBInvocationTestCase {

    private static final String ARCHIVE_NAME = "ReentrantSFSBInvocationTestCase";
    private static final String EJB3_LOGGER_NAME = "org.jboss.as.ejb3";
    private static final int EXPECTED_COUNT = 10;
    private static final String WFLYEJB0487 = "WFLYEJB0487";

    @Inject
    private ReentrantSFSB reentrantSFSB;

    private CapturingLogHandler logHandler;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(ReentrantSFSBInvocationTestCase.class, ReentrantSFSB.class);
        jar.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        return jar;
    }

    @Before
    public void installLogHandler() {
        logHandler = new CapturingLogHandler();
        Logger ejb3Logger = Logger.getLogger(EJB3_LOGGER_NAME);
        ejb3Logger.addHandler(logHandler);
    }

    @After
    public void removeLogHandler() {
        Logger.getLogger(EJB3_LOGGER_NAME).removeHandler(logHandler);
    }

    /**
     * Triggers 9 reentrant self-calls via the EJB proxy - the exact reproducer
     * pattern from WFLY-22385 (stop when count % 10 == 0).
     *
     * Asserts:
     * 1. The count reaches 10 - all reentrant frames completed successfully.
     * 2. WFLYEJB0487 was NOT logged - the spurious "Unexpected invocation state 0"
     *    error emitted by each outer frame on unwind (before the fix) is gone.
     */
    @Test
    public void testReentrantSelfInvocationViaBusinessObject() {
        reentrantSFSB.increment();

        Assert.assertEquals("All reentrant invocations must complete: count must reach " + EXPECTED_COUNT,
                EXPECTED_COUNT, reentrantSFSB.getCount());

        Assert.assertFalse(
                "WFLYEJB0487 (Unexpected invocation state) must not be logged during reentrant SFSB invocation (regression check for WFLY-22385)",
                logHandler.containsMessage(WFLYEJB0487));
    }

    /**
     * A simple JUL Handler that captures all log messages so they can be
     * asserted in tests without requiring access to the server log file.
     */
    private static class CapturingLogHandler extends Handler {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record.getMessage() != null) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
            messages.clear();
        }

        public boolean containsMessage(String fragment) {
            return messages.stream().anyMatch(m -> m.contains(fragment));
        }
    }
}
