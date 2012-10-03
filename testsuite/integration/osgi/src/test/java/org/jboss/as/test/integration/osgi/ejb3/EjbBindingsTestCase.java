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

package org.jboss.as.test.integration.osgi.ejb3;

import java.io.InputStream;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.naming.Context;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.osgi.api.Echo;
import org.jboss.as.test.integration.osgi.ejb3.bundle.BeansActivatorA;
import org.jboss.as.test.integration.osgi.ejb3.bundle.BeansActivatorB;
import org.jboss.as.test.integration.osgi.ejb3.bundle.BeansService;
import org.jboss.as.test.integration.osgi.ejb3.bundle.RemoteEcho;
import org.jboss.as.test.integration.osgi.ejb3.bundle.SampleSFSB;
import org.jboss.as.test.integration.osgi.ejb3.bundle.SampleSLSB;
import org.jboss.osgi.spi.OSGiManifestBuilder;
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

/**
 * Tests EJB deployments with OSGi metadata
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Jul-2012
 */
@RunWith(Arquillian.class)
public class EjbBindingsTestCase {

    private static final String EJB3_BUNDLE_A_JAR = "ejb3-bundle-a.jar";
    private static final String EJB3_BUNDLE_B_JAR = "ejb3-bundle-b.jar";

    @ArquillianResource
    Deployer deployer;

    @Inject
    public PackageAdmin packageAdmin;

    @Inject
    public BundleContext context;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "osgi-ejb3-test");
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                return builder.openStream();
            }
        });
        return jar;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindings() throws Exception {
        deployer.deploy(EJB3_BUNDLE_A_JAR);
        try {
            Bundle bundle = packageAdmin.getBundles(EJB3_BUNDLE_A_JAR, null)[0];
            Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

            // This service is registered by the {@link BeansActivatorA}
            ServiceReference sref = context.getServiceReference(Callable.class.getName());
            Callable<Boolean> service = (Callable<Boolean>) context.getService(sref);
            Assert.assertTrue(service.call());

        } finally {
            deployer.undeploy(EJB3_BUNDLE_A_JAR);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeferredBindings() throws Exception {
        InputStream input = deployer.getDeployment(EJB3_BUNDLE_A_JAR);
        Bundle bundle = context.installBundle(EJB3_BUNDLE_A_JAR, input);
        try {
            Assert.assertEquals("INSTALLED", Bundle.INSTALLED, bundle.getState());

            bundle.start();
            Assert.assertEquals("ACTIVE", Bundle.ACTIVE, bundle.getState());

            // This service is registered by the {@link BeansActivatorA}
            ServiceReference sref = context.getServiceReference(Callable.class.getName());
            Callable<Boolean> service = (Callable<Boolean>) context.getService(sref);
            Assert.assertTrue(service.call());

        } finally {
            bundle.uninstall();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    @Ignore("[AS7-5655] Cannot restart ejb3 bundle after activation failure")
    public void testDeferredBundleWithFailure() throws Exception {
        InputStream input = deployer.getDeployment(EJB3_BUNDLE_B_JAR);
        Bundle bundle = context.installBundle(EJB3_BUNDLE_B_JAR, input);
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

            // This service is registered by the {@link BeansActivatorA}
            ServiceReference sref = context.getServiceReference(Callable.class.getName());
            Callable<Boolean> service = (Callable<Boolean>) context.getService(sref);
            Assert.assertTrue(service.call());

        } finally {
            deployer.undeploy(EJB3_BUNDLE_A_JAR);
        }
    }

    @Deployment(name = EJB3_BUNDLE_A_JAR, managed = false, testable = false)
    public static JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, EJB3_BUNDLE_A_JAR);
        archive.addClasses(RemoteEcho.class, SampleSFSB.class, SampleSLSB.class, Echo.class);
        archive.addClasses(BeansActivatorA.class, BeansService.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(BeansActivatorA.class);
                builder.addImportPackages(Context.class, BundleActivator.class, Assert.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = EJB3_BUNDLE_B_JAR, managed = false, testable = false)
    public static JavaArchive getBundleB() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, EJB3_BUNDLE_B_JAR);
        archive.addClasses(RemoteEcho.class, SampleSFSB.class, SampleSLSB.class, Echo.class);
        archive.addClasses(BeansActivatorB.class, BeansService.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(BeansActivatorB.class);
                builder.addImportPackages(Context.class, BundleActivator.class, Assert.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}
