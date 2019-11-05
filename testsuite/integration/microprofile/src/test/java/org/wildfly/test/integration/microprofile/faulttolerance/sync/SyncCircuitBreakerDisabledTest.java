/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.microprofile.faulttolerance.sync;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.ReflectPermission;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import io.smallrye.faulttolerance.SimpleCommand;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Adapted from Thorntail.
 *
 * @author Martin Kouba
 * @author Radoslav Husar
 */
@RunWith(Arquillian.class)
public class SyncCircuitBreakerDisabledTest {

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, SyncCircuitBreakerDisabledTest.class.getSimpleName() + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(new StringAsset("Dependencies: com.netflix.archaius.core, com.netflix.hystrix.core, io.smallrye.fault-tolerance\n"), "MANIFEST.MF")
                .addAsManifestResource(new FileAsset(new File("src/test/resources/faulttolerance/microprofile-config.properties")), "microprofile-config.properties")
                .addPackage(SyncCircuitBreakerDisabledTest.class.getPackage())
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission("*", "read,write"),
                        new ReflectPermission("suppressAccessChecks"),
                        new RuntimePermission("getenv.*"),
                        new RuntimePermission("modifyThread")
                ), "permissions.xml")
                ;
    }

    @Inject
    ShakyServiceClient client;

    @Test
    public void testDefaultHystrixCircuitBreakerUsed() throws InterruptedException {
        // Verify Hystrix config first
        DynamicLongProperty intervalInMilliseconds = DynamicPropertyFactory.getInstance().getLongProperty("hystrix.command.default.metrics.healthSnapshot.intervalInMilliseconds", 500);
        assertEquals(10, intervalInMilliseconds.get());
        ShakyServiceClient.COUNTER.set(0);

        // CLOSED
        for (int i = 0; i < ShakyServiceClient.REQUEST_THRESHOLD; i++) {
            assertInvocation(false);
        }
        assertEquals(ShakyServiceClient.REQUEST_THRESHOLD, ShakyServiceClient.COUNTER.get());
        // Should be OPEN now
        HystrixCircuitBreaker breaker = HystrixCircuitBreaker.Factory.getInstance(HystrixCommandKey.Factory.asKey(getCommandKey()));
        assertNotNull(breaker);
        assertFalse(breaker.getClass().getName().contains("org.wildfly.microprofile.faulttolerance"));
        assertTrue(breaker.isOpen());
        assertInvocation(true);
        assertEquals(ShakyServiceClient.REQUEST_THRESHOLD, ShakyServiceClient.COUNTER.get());

        // Wait a little so that hystrix allows us to close
        TimeUnit.MILLISECONDS.sleep(ShakyServiceClient.DELAY);

        // Should be HALF-OPEN
        assertInvocation(false, true);
        assertEquals(ShakyServiceClient.REQUEST_THRESHOLD + 1, ShakyServiceClient.COUNTER.get());

        // Should be CLOSED
        assertInvocation(false);
        assertInvocation(false);
        assertEquals(ShakyServiceClient.REQUEST_THRESHOLD + 3, ShakyServiceClient.COUNTER.get());
    }

    private String getCommandKey() {
        try {
            return SimpleCommand.getCommandKey(ShakyServiceClient.class.getDeclaredMethod("ping", Boolean.TYPE));
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private void assertInvocation(boolean open) throws InterruptedException {
        assertInvocation(open, false);
    }

    private void assertInvocation(boolean open, boolean success) throws InterruptedException {
        try {
            client.ping(success);
            if (!success) {
                fail("Invocation should have failed!");
            }
        } catch (Exception e) {
            if (open) {
                assertTrue("Circuit breaker must be open: " + e, e instanceof CircuitBreakerOpenException);
            } else {
                assertTrue("IllegalStateException expected: " + e, e instanceof IllegalStateException);
            }
        }
        TimeUnit.MILLISECONDS.sleep(100);
    }
}