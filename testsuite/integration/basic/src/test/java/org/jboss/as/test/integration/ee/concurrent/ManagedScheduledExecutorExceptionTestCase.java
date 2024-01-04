/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ManagedScheduledExecutorExceptionTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, ManagedScheduledExecutorExceptionTestCase.class.getSimpleName() + ".jar")
                .addClasses(ManagedScheduledExecutorExceptionTestCase.class);
    }

    private static final String loggerMessage = "WFLYEE0136";
    private static final String message = "WFLY-17186";

    private static void badMethod() {
        throw new RuntimeException(message);
    }

    @Resource(lookup = "java:comp/DefaultManagedScheduledExecutorService")
    private ManagedScheduledExecutorService executorService;

    @Test
    public void testScheduledRunnable() {
        assert executorService != null;
        Runnable r = ManagedScheduledExecutorExceptionTestCase::badMethod;
        ScheduledFuture<?> future = executorService.schedule(r, 1, TimeUnit.MILLISECONDS);
        checkFuture(future);
    }

    @Test
    public void testScheduledCallable() {
        assert executorService != null;
        Callable<Void> callable = () -> { badMethod(); return null;};
        ScheduledFuture<?> future = executorService.schedule(callable, 1, TimeUnit.MILLISECONDS);
        checkFuture(future);
    }

    private void checkFuture(ScheduledFuture<?> future) {
        try {
            future.get();
            fail("Exception did not propagate");
        } catch (ExecutionException e) {
            assertTrue(e.toString(), e.getCause().getMessage().contains(loggerMessage));
            assertTrue(e.toString(), e.getCause().getCause().getMessage().contains(message));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Caught " + e);
        }
    }
}
