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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.apache.aries.jmx.framework.BundleState;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.jmx.MBeanProxy;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.jmx.framework.BundleStateMBean;

/**
 * Test {@link BundleState} functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 15-Feb-2010
 */
@RunWith(Arquillian.class)
public class BundleStateTestCase {

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-bundlestate");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(BundleStateMBean.class);
                builder.addImportPackages(MBeanServer.class, MBeanProxy.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBundleStateMBean() throws Exception {

        ServiceReference sref = context.getServiceReference(MBeanServer.class.getName());
        MBeanServer server = (MBeanServer) context.getService(sref);

        ObjectName oname = ObjectName.getInstance(BundleStateMBean.OBJECTNAME);
        BundleStateMBean bundleState = MBeanProxy.get(server, oname, BundleStateMBean.class);
        assertNotNull("BundleStateMBean not null", bundleState);

        TabularData bundleData = bundleState.listBundles();
        assertNotNull("TabularData not null", bundleData);
        assertFalse("TabularData not empty", bundleData.isEmpty());
    }
}