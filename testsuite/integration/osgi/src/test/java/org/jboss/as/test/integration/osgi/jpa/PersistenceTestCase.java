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
package org.jboss.as.test.integration.osgi.jpa;

import java.io.InputStream;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.osgi.jpa.bundle.Employee;
import org.jboss.as.test.integration.osgi.jpa.bundle.PersistenceActivatorA;
import org.jboss.as.test.integration.osgi.jpa.bundle.PersistenceActivatorB;
import org.jboss.as.test.integration.osgi.jpa.bundle.PersistenceService;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test simple OSGi Persistence deployment
 *
 * @author thomas.diesler@jboss.com
 * @since 31-Aug-2012
 */
@RunWith(Arquillian.class)
public class PersistenceTestCase {

    private static final String PERSISTENCE_BUNDLE_A = "persistence-bundle-a.jar";
    private static final String PERSISTENCE_BUNDLE_B = "persistence-bundle-b.jar";

    @ArquillianResource
    Deployer deployer;

    @Inject
    public PackageAdmin packageAdmin;

    @Inject
    public BundleContext context;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "osgi-jpa-test");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Bundle.class, ServiceTracker.class, EntityManager.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEntityManagerFactoryService() throws Exception {
        deployer.deploy(PERSISTENCE_BUNDLE_A);
        try {
            Bundle bundle = packageAdmin.getBundles(PERSISTENCE_BUNDLE_A, null)[0];
            Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

            // This service is registered by the {@link PersistenceActivatorA}
            ServiceReference sref = context.getServiceReference(Callable.class.getName());
            Callable<Boolean> service = (Callable<Boolean>) context.getService(sref);
            Assert.assertTrue(service.call());

        } finally {
            deployer.undeploy(PERSISTENCE_BUNDLE_A);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeferredEntityManagerFactoryService() throws Exception {
        InputStream input = deployer.getDeployment(PERSISTENCE_BUNDLE_A);
        Bundle bundle = context.installBundle(PERSISTENCE_BUNDLE_A, input);
        try {
            Assert.assertEquals("INSTALLED", Bundle.INSTALLED, bundle.getState());

            bundle.start();
            Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

            // This service is registered by the {@link PersistenceActivatorB}
            ServiceReference sref = context.getServiceReference(Callable.class.getName());
            Callable<Boolean> service = (Callable<Boolean>) context.getService(sref);
            Assert.assertTrue(service.call());

        } finally {
            bundle.uninstall();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    @Ignore("[AS7-5654] Cannot restart jpa bundle after activation failure")
    public void testDeferredBundleWithFailure() throws Exception {
        InputStream input = deployer.getDeployment(PERSISTENCE_BUNDLE_B);
        Bundle bundle = context.installBundle(PERSISTENCE_BUNDLE_B, input);
        try {
            Assert.assertEquals("INSTALLED", Bundle.INSTALLED, bundle.getState());

            try {
                bundle.start();
                Assert.fail("BundleException expected");
            } catch (BundleException e) {
                // expected
            }
            Assert.assertEquals("RESOLVED", Bundle.RESOLVED, bundle.getState());

            bundle.start();
            Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

            // This service is registered by the {@link PersistenceActivator}
            ServiceReference sref = context.getServiceReference(Callable.class.getName());
            Callable<Boolean> service = (Callable<Boolean>) context.getService(sref);
            Assert.assertTrue(service.call());

        } finally {
            bundle.uninstall();
        }
    }

    @Deployment(name = PERSISTENCE_BUNDLE_A, managed = false, testable = false)
    public static JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, PERSISTENCE_BUNDLE_A);
        archive.addClasses(Employee.class, PersistenceActivatorA.class, PersistenceService.class);
        archive.addAsResource(Employee.class.getPackage(), "simple-persistence.xml", "META-INF/persistence.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                // The Meta-Persistence header may include zero or more comma-separated jar-paths.
                // Each a path to a Persistence Descriptor resource in the bundle.
                builder.addManifestHeader("Meta-Persistence", "");
                builder.addBundleActivator(PersistenceActivatorA.class);
                builder.addImportPackages(EntityManagerFactory.class);
                builder.addImportPackages(BundleActivator.class, Assert.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = PERSISTENCE_BUNDLE_B, managed = false, testable = false)
    public static JavaArchive getBundleB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, PERSISTENCE_BUNDLE_B);
        archive.addClasses(Employee.class, PersistenceActivatorB.class, PersistenceService.class);
        archive.addAsResource(Employee.class.getPackage(), "simple-persistence.xml", "META-INF/persistence.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                // The Meta-Persistence header may include zero or more comma-separated jar-paths.
                // Each a path to a Persistence Descriptor resource in the bundle.
                builder.addManifestHeader("Meta-Persistence", "");
                builder.addBundleActivator(PersistenceActivatorB.class);
                builder.addImportPackages(EntityManagerFactory.class);
                builder.addImportPackages(BundleActivator.class, Assert.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}
