/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.testsuite.integration.osgi.jmx;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import javax.inject.Inject;
import javax.management.MBeanServer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.osgi.jmx.bundle.Foo;
import org.jboss.as.testsuite.integration.osgi.jmx.bundle.FooMBean;
import org.jboss.as.testsuite.integration.osgi.jmx.bundle.MBeanActivator;
import org.jboss.osgi.jmx.MBeanProxy;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A test that deployes a bundle that registeres an MBean
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Feb-2009
 */
@RunWith(Arquillian.class)
public class MBeanServerTestCase {

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-mbean");
        archive.addClasses(Foo.class, FooMBean.class, MBeanActivator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(MBeanActivator.class);
                builder.addImportPackages(BundleActivator.class, ServiceTracker.class);
                builder.addImportPackages(MBeanServer.class, MBeanProxy.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testMBeanAccess() throws Exception {
        bundle.start();
        BundleContext context = bundle.getBundleContext();
        ServiceReference sref = context.getServiceReference(MBeanServer.class.getName());
        MBeanServer server = (MBeanServer) context.getService(sref);
        FooMBean foo = MBeanProxy.get(server, FooMBean.MBEAN_NAME, FooMBean.class);
        assertEquals("hello", foo.echo("hello"));
    }
}