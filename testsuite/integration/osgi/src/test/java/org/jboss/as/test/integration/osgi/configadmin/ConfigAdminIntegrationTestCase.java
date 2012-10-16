/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.io.InputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.configadmin.ConfigAdmin;
import org.jboss.as.configadmin.ConfigAdminListener;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.configadmin.ConfigAdminManagement;
import org.jboss.as.test.integration.osgi.api.ConfiguredService;
import org.jboss.as.test.integration.osgi.configadmin.bundle.ConfigAdminBundleActivatorA;
import org.jboss.as.test.integration.osgi.configadmin.bundle.ConfigAdminBundleActivatorB;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test {@link ConfigurationAdmin}/{@link ConfigAdmin} integration.
 *
 * @author Thomas Diesler
 * @since 30-Sep-2012
 */
@RunWith(Arquillian.class)
public class ConfigAdminIntegrationTestCase {

    static final String CONFIG_ADMIN_BUNDLE_A = "config-admin-bundle-a";
    static final String CONFIG_ADMIN_BUNDLE_B = "config-admin-bundle-b";

    static final String CONFIG_ADMIN_PID_A = "config-admin-pid-a";
    static final String CONFIG_ADMIN_PID_B = "config-admin-pid-b";
    static final String CONFIG_ADMIN_PID_C = "config-admin-pid-c";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @ArquillianResource
    PackageAdmin packageAdmin;

    @ArquillianResource
    BundleContext syscontext;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "osgi-configadmin-tests");
        archive.addClasses(ConfiguredService.class, ConfigAdminManagement.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addExportPackages(ConfiguredService.class);
                builder.addImportPackages(ConfigurationAdmin.class, ModelNode.class);
                builder.addImportPackages(ConfigAdmin.class, ServiceContainer.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testConfigAdminWriteFromBundle() throws Exception {
        InputStream input = deployer.getDeployment(CONFIG_ADMIN_BUNDLE_A);
        Bundle bundle = syscontext.installBundle(CONFIG_ADMIN_PID_A, input);
        try {
            bundle.start();
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

            BundleContext context = bundle.getBundleContext();
            ConfigAdmin configAdmin = getConfigAdmin(context);
            ConfigurationAdmin configurationAdmin = getConfigurationAdmin(context);

            Configuration config = configurationAdmin.getConfiguration(CONFIG_ADMIN_PID_A);
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("foo", "bar");
            config.update(props);
            try {
                ServiceReference sref = context.getServiceReference(ConfiguredService.class.getName());
                ConfiguredService service = (ConfiguredService) context.getService(sref);

                // Wait a little for the update to happen
                Assert.assertTrue(service.awaitUpdate(3, TimeUnit.SECONDS));
                Assert.assertEquals("bar", service.getProperties().get("foo"));

                Dictionary<String, String> modelProps = configAdmin.getConfiguration(CONFIG_ADMIN_PID_A);
                Assert.assertEquals("bar", modelProps.get("foo"));
            } finally {
                config.delete();
            }
        } finally {
            bundle.uninstall();
        }
    }

    @Test
    public void testConfigAdminWriteFromAPI() throws Exception {
        InputStream input = deployer.getDeployment(CONFIG_ADMIN_BUNDLE_A);
        Bundle bundle = syscontext.installBundle(CONFIG_ADMIN_PID_B, input);
        try {
            bundle.start();
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

            BundleContext context = bundle.getBundleContext();
            ConfigAdmin configAdmin = getConfigAdmin(context);
            ConfigurationAdmin configurationAdmin = getConfigurationAdmin(context);

            final CountDownLatch latch = new CountDownLatch(1);
            ConfigAdminListener listener = new ConfigAdminListener() {

                @Override
                public void configurationModified(String pid, Dictionary<String, String> props) {
                    if (props != null)
                        latch.countDown();
                }

                @Override
                public Set<String> getPIDs() {
                    return Collections.singleton(CONFIG_ADMIN_PID_B);
                }
            };
            configAdmin.addListener(listener);

            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("foo", "bar");
            configAdmin.putConfiguration(CONFIG_ADMIN_PID_B, props);

            try {
                // Wait a little for the update to happen
                Assert.assertTrue(latch.await(3, TimeUnit.SECONDS));
                Configuration config = configurationAdmin.getConfiguration(CONFIG_ADMIN_PID_B);
                Assert.assertEquals("bar", config.getProperties().get("foo"));

                ServiceReference sref = context.getServiceReference(ConfiguredService.class.getName());
                ConfiguredService service = (ConfiguredService) context.getService(sref);

                // Wait a little for the update to happen
                Assert.assertTrue(service.awaitUpdate(3, TimeUnit.SECONDS));
                Assert.assertEquals("bar", service.getProperties().get("foo"));
            } finally {
                configAdmin.removeConfiguration(CONFIG_ADMIN_PID_B);
                configAdmin.removeListener(listener);
            }
        } finally {
            bundle.uninstall();
        }
    }

    @Test
    public void testConfigAdminWriteFromModel() throws Exception {
        InputStream input = deployer.getDeployment(CONFIG_ADMIN_BUNDLE_A);
        Bundle bundle = syscontext.installBundle(CONFIG_ADMIN_PID_C, input);
        try {
            bundle.start();
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

            BundleContext context = bundle.getBundleContext();
            ConfigAdmin configAdmin = getConfigAdmin(context);
            ConfigurationAdmin configurationAdmin = getConfigurationAdmin(context);

            final CountDownLatch latch = new CountDownLatch(1);
            ConfigAdminListener listener = new ConfigAdminListener() {

                @Override
                public void configurationModified(String pid, Dictionary<String, String> props) {
                    if (props != null)
                        latch.countDown();
                }

                @Override
                public Set<String> getPIDs() {
                    return Collections.singleton(CONFIG_ADMIN_PID_C);
                }
            };
            configAdmin.addListener(listener);

            Dictionary<String, String> modelProps = new Hashtable<String, String>();
            modelProps.put("foo", "bar");
            ConfigAdminManagement.updateConfiguration(getControllerClient(), CONFIG_ADMIN_PID_C, modelProps);

            try {
                // Wait a little for the update to happen
                Assert.assertTrue(latch.await(3, TimeUnit.SECONDS));
                Configuration config = configurationAdmin.getConfiguration(CONFIG_ADMIN_PID_C);
                Assert.assertEquals("bar", config.getProperties().get("foo"));

                ServiceReference sref = context.getServiceReference(ConfiguredService.class.getName());
                ConfiguredService service = (ConfiguredService) context.getService(sref);

                // Wait a little for the update to happen
                Assert.assertTrue(service.awaitUpdate(3, TimeUnit.SECONDS));
                Assert.assertEquals("bar", service.getProperties().get("foo"));
            } finally {
                configAdmin.removeListener(listener);
                ConfigAdminManagement.removeConfiguration(getControllerClient(), CONFIG_ADMIN_PID_C);
            }
        } finally {
            bundle.uninstall();
        }
    }

    @Test
    public void testConfigAdminWriteFromBundleActivator() throws Exception {
        deployer.deploy(CONFIG_ADMIN_BUNDLE_B);
        try {
            Bundle bundle = packageAdmin.getBundles(CONFIG_ADMIN_BUNDLE_B, null)[0];
            Assert.assertEquals("Bundle ACTIVE", Bundle.ACTIVE, bundle.getState());

            BundleContext context = bundle.getBundleContext();
            ConfigAdmin configAdmin = getConfigAdmin(context);
            ConfigurationAdmin configurationAdmin = getConfigurationAdmin(context);

            Configuration config = configurationAdmin.getConfiguration(CONFIG_ADMIN_BUNDLE_B);
            try {
                Assert.assertEquals("bar", config.getProperties().get("foo"));

                Dictionary<String, String> modelProps = configAdmin.getConfiguration(CONFIG_ADMIN_BUNDLE_B);
                Assert.assertEquals("bar", modelProps.get("foo"));

                ServiceReference sref = context.getServiceReference(ConfiguredService.class.getName());
                ConfiguredService service = (ConfiguredService) context.getService(sref);

                // Wait a little for the update to happen
                Assert.assertTrue(service.awaitUpdate(3, TimeUnit.SECONDS));
                Assert.assertEquals("bar", service.getProperties().get("foo"));
            } finally {
                config.delete();
            }
        } finally {
            deployer.undeploy(CONFIG_ADMIN_BUNDLE_B);
        }
    }

    private ModelControllerClient getControllerClient() {
        return managementClient.getControllerClient();
    }

    private ConfigurationAdmin getConfigurationAdmin(BundleContext context) {
        ServiceReference sref = context.getServiceReference(ConfigurationAdmin.class.getName());
        return (ConfigurationAdmin) context.getService(sref);
    }

    private ConfigAdmin getConfigAdmin(BundleContext context) {
        ServiceReference sref = context.getServiceReference(ServiceContainer.class.getName());
        ServiceContainer serviceContainer = (ServiceContainer) context.getService(sref);
        ServiceController<?> controller = serviceContainer.getRequiredService(ConfigAdmin.SERVICE_NAME);
        return (ConfigAdmin) controller.getValue();
    }

    @Deployment(name = CONFIG_ADMIN_BUNDLE_A, managed = false, testable = false)
    public static JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CONFIG_ADMIN_BUNDLE_A);
        archive.addClasses(ConfigAdminBundleActivatorA.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(ConfigAdminBundleActivatorA.class);
                builder.addImportPackages(BundleActivator.class, ManagedService.class);
                builder.addImportPackages(ConfiguredService.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = CONFIG_ADMIN_BUNDLE_B, managed = false, testable = false)
    public static JavaArchive getBundleD() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CONFIG_ADMIN_BUNDLE_B);
        archive.addClasses(ConfigAdminBundleActivatorB.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(ConfigAdminBundleActivatorB.class);
                builder.addImportPackages(BundleActivator.class, ManagedService.class);
                builder.addImportPackages(ConfiguredService.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}
