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

package org.jboss.as.test.integration.osgi.configadmin;


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
import org.jboss.as.configadmin.ConfigAdmin;
import org.jboss.as.configadmin.ConfigAdminListener;
import org.jboss.as.test.integration.osgi.configadmin.bundle.ConfiguredMSCService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Transition;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A test that shows how an MSC service can be configured through the {@link ConfigAdmin}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Dec-2010
 */
@RunWith(Arquillian.class)
public class ConfigAdminTestCase {

    static final String PID_A = ConfigAdminTestCase.class.getSimpleName() + "-pid-a";
    static final String PID_B = ConfigAdminTestCase.class.getSimpleName() + "-pid-b";

    @Inject
    public ServiceContainer serviceContainer;

    @Inject
    public ServiceTarget serviceTarget;

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class, "configadmin.jar")
                .addClasses(ConfiguredMSCService.class)
                .addAsManifestResource(new StringAsset(
                        "Manifest-Version: 1.0\n" +
                        "Dependencies: org.jboss.as.configadmin,javax.inject.api\n"
                ), "MANIFEST.MF");
    }

    @Test
    public void testConfigAdminService() throws Exception {

        // Verify that there is no config with this PID already
        ConfigAdmin configAdmin = getConfigAdmin();
        boolean hasconfig = configAdmin.hasConfiguration(PID_A);
        Assert.assertFalse("Precondition: null config", hasconfig);

        // Register a new config for the given PID
        Dictionary<String, String> config = new Hashtable<String, String>();
        config.put("foo", "bar");
        Dictionary<String, String> oldConfig = configAdmin.putConfiguration(PID_A, config);

        try {
            Assert.assertNull("Old config null", oldConfig);

            // Verify the registered config
            Dictionary<String, String> regConfig = configAdmin.getConfiguration(PID_A);
            Assert.assertNotNull("Config not null", regConfig);
            Assert.assertEquals("bar", regConfig.get("foo"));

            // Verify unmodifiable dictionary
            try {
                regConfig.remove("foo");
                Assert.fail("UnsupportedOperationException expected");
            } catch (UnsupportedOperationException ex) {
                // expected
            }

            // Verify unmodifiable dictionary
            config.put("foo", "baz");
            regConfig = configAdmin.getConfiguration(PID_A);
            Assert.assertEquals("bar", regConfig.get("foo"));

        } finally {
            oldConfig = configAdmin.removeConfiguration(PID_A);
            Assert.assertNotNull("Config not null", oldConfig);
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
                return Collections.singleton(PID_B);
            }
        };
        ConfigAdmin configAdmin = getConfigAdmin();
        configAdmin.addListener(listener);

        Assert.assertTrue(latches[0].await(3, TimeUnit.SECONDS));
        Assert.assertNull("First invocation with null, but was: " + dictionaries[0], dictionaries[0]);

        // Register a new config for the given PID
        Dictionary<String, String> config = new Hashtable<String, String>();
        config.put("foo", "bar");
        configAdmin.putConfiguration(PID_B, config);

        try {
            Assert.assertTrue(latches[1].await(3, TimeUnit.SECONDS));
            Assert.assertNotNull("Second invocation not null", dictionaries[1]);
            Assert.assertEquals("bar", dictionaries[1].get("foo"));
        } finally {
            configAdmin.removeConfiguration(PID_B);
            configAdmin.removeListener(listener);
        }
    }

    @Test
    public void testConfiguredService() throws Exception {

        Dictionary<String, String> config = new Hashtable<String, String>();
        config.put("foo", "bar");

        // Register a new config for the given PID
        ConfigAdmin configAdmin = getConfigAdmin();
        configAdmin.putConfiguration(ConfiguredMSCService.SERVICE_PID, config);

        ServiceController<ConfiguredMSCService> controller = null;
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            ServiceListener<ConfiguredMSCService> tracker = new AbstractServiceListener<ConfiguredMSCService>() {
                @Override
                public void transition(ServiceController<? extends ConfiguredMSCService> controller, Transition transition) {
                    if (transition == Transition.STARTING_to_UP) {
                        latch.countDown();
                    }
                }
            };
            controller = ConfiguredMSCService.addService(serviceTarget, tracker);
            Assert.assertTrue(latch.await(3, TimeUnit.SECONDS));
            ConfiguredMSCService service = controller.getValue();
            Assert.assertEquals("bar", service.getConfig().get("foo"));
        } finally {
            configAdmin.removeConfiguration(ConfiguredMSCService.SERVICE_PID);
            controller.setMode(ServiceController.Mode.REMOVE);
        }
    }

    private ConfigAdmin getConfigAdmin() {
        ServiceController<?> controller = serviceContainer.getRequiredService(ConfigAdmin.SERVICE_NAME);
        return (ConfigAdmin) controller.getValue();
    }
}
