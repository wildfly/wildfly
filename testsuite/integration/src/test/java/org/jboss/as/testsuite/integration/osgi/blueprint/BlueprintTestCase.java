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
package org.jboss.as.testsuite.integration.osgi.blueprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.inject.Inject;
import javax.management.MBeanServer;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.testsuite.integration.osgi.blueprint.bundle.BeanA;
import org.jboss.as.testsuite.integration.osgi.blueprint.bundle.BeanB;
import org.jboss.as.testsuite.integration.osgi.blueprint.bundle.ServiceA;
import org.jboss.as.testsuite.integration.osgi.blueprint.bundle.ServiceB;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;

/**
 * A simple Blueprint Container test.
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Jul-2009
 */
@RunWith(Arquillian.class)
public class BlueprintTestCase {

    @Inject
    public BundleContext context;

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-blueprint");
        archive.addClasses(BeanA.class, ServiceA.class, BeanB.class, ServiceB.class);
        archive.addAsResource("osgi/blueprint/blueprint-example.xml", "OSGI-INF/blueprint/blueprint-example.xml");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(BlueprintContainer.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testBlueprintContainerAvailable() throws Exception {
        bundle.start();
        assertEquals("example-blueprint", bundle.getSymbolicName());

        BlueprintContainer bpContainer = getBlueprintContainer();
        assertNotNull("BlueprintContainer available", bpContainer);
    }

    @Test
    public void testServiceA() throws Exception {
        bundle.start();
        ServiceReference sref = context.getServiceReference(ServiceA.class.getName());
        assertNotNull("ServiceA not null", sref);

        ServiceA service = (ServiceA) context.getService(sref);
        MBeanServer mbeanServer = service.getMbeanServer();
        assertNotNull("MBeanServer not null", mbeanServer);
    }

    @Test
    public void testServiceB() throws Exception {
        bundle.start();
        ServiceReference sref = context.getServiceReference(ServiceB.class.getName());
        assertNotNull("ServiceB not null", sref);

        ServiceB service = (ServiceB) context.getService(sref);
        BeanA beanA = service.getBeanA();
        assertNotNull("BeanA not null", beanA);
    }

    private BlueprintContainer getBlueprintContainer() throws Exception {
        // 10sec for processing of STARTING event
        int timeout = 10000;

        ServiceReference sref = null;
        while (sref == null && 0 < (timeout -= 200)) {
            String filter = "(osgi.blueprint.container.symbolicname=example-blueprint)";
            ServiceReference[] srefs = context.getServiceReferences(BlueprintContainer.class.getName(), filter);
            if (srefs != null && srefs.length > 0)
                sref = srefs[0];

            Thread.sleep(200);
        }
        assertNotNull("BlueprintContainer not null", sref);

        BlueprintContainer bpContainer = (BlueprintContainer) context.getService(sref);
        return bpContainer;
    }
}