/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.configadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.configadmin.service.ConfigAdminListener;
import org.jboss.as.configadmin.service.ConfigAdminService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test that shows how an MSC service can be configured through the {@link ConfigAdminService}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Dec-2010
 */
@RunWith(Arquillian.class)
public class ConfigAdminServiceTestCase {

    @Inject
    public ServiceContainer serviceContainer;

    @Inject
    public ServiceTarget serviceTarget;

    @Deployment
    public static Archive<?> deployment() {

        return ShrinkWrap.create(JavaArchive.class, "configadmin.jar")
                .addPackage(ConfiguredService.class.getPackage())
                .addAsManifestResource(new StringAsset(
                        "Manifest-Version: 1.0\n" +
                        "Dependencies: org.jboss.as.configadmin,javax.inject.api\n"
                ), "MANIFEST.MF");
    }

    @Test
    public void testConfigAdminService() throws Exception {

        // Verify that there is no config with this PID already
        ConfigAdminService configAdmin = getConfigAdminService();
        boolean hasconfig = configAdmin.hasConfiguration(ConfiguredService.SERVICE_PID);
        assertFalse("Config null", hasconfig);

        Dictionary<String, String> config = new Hashtable<String, String>();
        config.put("foo", "bar");

        // Register a new config for the given PID
        Dictionary<String, String> oldConfig = configAdmin.putConfiguration(ConfiguredService.SERVICE_PID, config);
        try {
            assertNull("Config null", oldConfig);

            // Verify the registered config
            Dictionary<String, String> regConfig = configAdmin.getConfiguration(ConfiguredService.SERVICE_PID);
            assertNotNull("Config not null", regConfig);
            assertEquals("Config not null", 1, regConfig.size());
            assertEquals("bar", regConfig.get("foo"));

            // Verify unmodifiable dictionary
            try {
                regConfig.remove("foo");
                fail("UnsupportedOperationException expected");
            } catch (UnsupportedOperationException ex) {
                // expected
            }

            // Verify unmodifiable dictionary
            config.put("foo", "baz");
            regConfig = configAdmin.getConfiguration(ConfiguredService.SERVICE_PID);
            assertEquals("bar", regConfig.get("foo"));

        } finally {
            oldConfig = configAdmin.removeConfiguration(ConfiguredService.SERVICE_PID);
            assertNotNull("Config not null", oldConfig);

            hasconfig = configAdmin.hasConfiguration(ConfiguredService.SERVICE_PID);
            assertFalse("Config null", hasconfig);
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testConfigurationListener() throws Exception {

        final AtomicInteger invocationCount = new AtomicInteger();
        final CountDownLatch[] latches = new CountDownLatch[] { new CountDownLatch(1), new CountDownLatch(1) };
        final Dictionary[] dictionaries = new Dictionary[2];

        // Add a configuration listener
        ConfigAdminListener listener = new ConfigAdminListener() {

            @Override
            public void configurationModified(String pid, Dictionary<String, String> props) {
                int index = invocationCount.getAndIncrement();
                if (index < 2) {
                    dictionaries[index] = props;
                    latches[index].countDown();
                }
            }

            @Override
            public Set<String> getPIDs() {
                return Collections.singleton(ConfiguredService.SERVICE_PID);
            }
        };
        ConfigAdminService configAdmin = getConfigAdminService();
        configAdmin.addListener(listener);

        latches[0].await();
        assertNull("First invocation with null", dictionaries[0]);

        Dictionary<String, String> config = new Hashtable<String, String>();
        config.put("foo", "bar");

        // Register a new config for the given PID
        configAdmin.putConfiguration(ConfiguredService.SERVICE_PID, config);
        try {
            latches[1].await();
            assertNotNull("Second invocation not null", dictionaries[1]);
        } finally {
            configAdmin.removeConfiguration(ConfiguredService.SERVICE_PID);
            configAdmin.removeListener(listener);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConfiguredService() throws Exception {

        Dictionary<String, String> config = new Hashtable<String, String>();
        config.put("foo", "bar");

        // Register a new config for the given PID
        ConfigAdminService configAdmin = getConfigAdminService();
        configAdmin.putConfiguration(ConfiguredService.SERVICE_PID, config);
        try {
            ConfiguredService.addService(serviceTarget);
            final CountDownLatch latch = new CountDownLatch(1);
            final ServiceController<ConfiguredService> controller = (ServiceController<ConfiguredService>) serviceContainer.getService(ConfiguredService.SERVICE_NAME);
            controller.addListener(new AbstractServiceListener<ConfiguredService>(){
                public void serviceStarted(ServiceController<? extends ConfiguredService> controller) {
                    controller.removeListener(this);
                    latch.countDown();
                }
            });
            latch.await(3, TimeUnit.SECONDS);
            ConfiguredService service = controller.getValue();
            assertEquals("bar", service.getConfigValue("foo"));
        } finally {
            configAdmin.removeConfiguration(ConfiguredService.SERVICE_PID);
            serviceContainer.getService(ConfiguredService.SERVICE_NAME).setMode(ServiceController.Mode.REMOVE);
        }
    }

    // [TODO] Move this to @Before when Arquillian supports injected values there
    private ConfigAdminService getConfigAdminService() {
        ServiceController<?> controller = serviceContainer.getService(ConfigAdminService.SERVICE_NAME);
        assertNotNull("ServiceController available: " + ConfigAdminService.SERVICE_NAME, controller);
        ConfigAdminService configAdmin = (ConfigAdminService) controller.getValue();
        assertNotNull("Service available: " + ConfigAdminService.SERVICE_NAME, configAdmin);
        return configAdmin;
    }
}
