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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.osgi.api.ConfiguredService;
import org.jboss.as.test.osgi.FrameworkUtils;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;

/**
 * A test that shows how an OSGi {@link ManagedService} can be configured through the {@link ConfigurationAdmin}.
 *
 * This test needs to run against an AS instance that contains the following config
 *
   <subsystem xmlns="urn:jboss:domain:configadmin:1.0">
     <configuration pid="a.test.pid">
       <property name="testkey" value="test value"/>
       <property name="test.key.2" value="nothing"/>
     </configuration>
   </subsystem>
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 11-Dec-2010
 */
@RunWith(Arquillian.class)
public class ConfigurationAdminTestCase {

    static final String PID_A = ConfigurationAdminTestCase.class.getSimpleName() + "-pid-a";
    static final String PID_B = "a.test.pid";

    @ArquillianResource
    Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "osgi-configadmin");
        archive.addClasses(FrameworkUtils.class, ConfiguredService.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(ConfigurationAdmin.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testManagedService() throws Exception {

        // Get the {@link Configuration} for the given PID
        BundleContext context = bundle.getBundleContext();
        ConfigurationAdmin configAdmin = getConfigurationAdmin(context);
        Configuration config = configAdmin.getConfiguration(PID_A);
        Assert.assertNotNull("Config not null", config);
        Assert.assertNull("Config is empty, but was: " + config.getProperties(), config.getProperties());

        try {
            Dictionary<String, String> configProps = new Hashtable<String, String>();
            configProps.put("foo", "bar");
            config.update(configProps);

            // Register a {@link ManagedService}
            ConfiguredService service = new ConfiguredService();
            Dictionary<String, String> serviceProps = new Hashtable<String, String>();
            serviceProps.put(Constants.SERVICE_PID, PID_A);
            context.registerService(new String[] { ConfiguredService.class.getName(), ManagedService.class.getName() }, service, serviceProps);

            // Wait a little for the update event
            Assert.assertTrue(service.awaitUpdate(3, TimeUnit.SECONDS));

            // Verify service property
            Assert.assertEquals("bar", service.getProperties().get("foo"));
        } finally {
            config.delete();
        }
    }

    @Test
    public void testManagedServiceConfiguredFromXML() throws Exception {

        BundleContext context = bundle.getBundleContext();

        // This configuration is present in the standalone.xml used for this test
        ConfiguredService service = new ConfiguredService();
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, PID_B);
        context.registerService(new String[] { ConfiguredService.class.getName(), ManagedService.class.getName() }, service, props);

        // Wait a little for the update to happen
        Assert.assertTrue(service.awaitUpdate(3, TimeUnit.SECONDS));
        Assert.assertEquals("test value", service.getProperties().get("testkey"));
    }

    private ConfigurationAdmin getConfigurationAdmin(BundleContext context) {
        ServiceReference sref = context.getServiceReference(ConfigurationAdmin.class.getName());
        return (ConfigurationAdmin) context.getService(sref);
    }
}
