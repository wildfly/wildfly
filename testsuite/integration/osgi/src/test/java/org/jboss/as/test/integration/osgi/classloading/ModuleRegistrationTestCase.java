/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.osgi.classloading;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.osgi.core.bundle.SimpleService;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XBundleRevisionBuilderFactory;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;

/**
 * Test the registration of a non-OSGi deployment.
 *
 * @author thomas.diesler@jboss.com
 */
@RunWith(Arquillian.class)
public class ModuleRegistrationTestCase {

    static final String BUNDLE_A = "bundle-a";

    @Inject
    public ServiceContainer container;

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-module-reg");
        archive.addClass(SimpleService.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core,org.jboss.osgi.framework,org.jboss.as.osgi");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testFindPackageCapability() throws Exception {

        // Build a package requirement
        XResourceBuilder builder = XBundleRevisionBuilderFactory.create();
        builder.addCapability(IDENTITY_NAMESPACE, "somename");
        XRequirement req = builder.addRequirement(PACKAGE_NAMESPACE, SimpleService.class.getPackage().getName());
        builder.getResource();

        // Find the providers for the requirement
        List<Capability> caps = getEnvironment().findProviders(req);
        assertEquals(1, caps.size());

        // Verify resource identity
        XResource xres = (XResource) caps.get(0).getResource();
        XIdentityCapability icap = xres.getIdentityCapability();
        assertEquals("deployment.example-module-reg", icap.getSymbolicName());
        assertEquals(Version.emptyVersion, icap.getVersion());
        assertEquals("unknown", icap.getType());
    }

    @Test
    public void testResolveModule() throws Exception {
        // Install a Bundle that has a package requirement on
        // on a capability exported from the modeule deployment
        InputStream inputA = deployer.getDeployment(BUNDLE_A);
        Bundle bundleA = context.installBundle(BUNDLE_A, inputA);
        try {
            FrameworkWiring fwkWiring = context.getBundle().adapt(FrameworkWiring.class);
            assertTrue(fwkWiring.resolveBundles(Collections.singleton(bundleA)));
            BundleWiring wiringA = bundleA.adapt(BundleWiring.class);
            List<BundleWire> wires = wiringA.getRequiredWires(PACKAGE_NAMESPACE);
            assertEquals(1, wires.size());
            BundleWire wire = wires.get(0);
            assertEquals(bundleA, wire.getRequirer().getBundle());
            XBundleRevision provider = (XBundleRevision) wire.getProvider();
            XIdentityCapability icap = provider.getIdentityCapability();
            assertEquals("deployment.example-module-reg", icap.getSymbolicName());
        } finally {
            bundleA.uninstall();
        }
    }

    private XEnvironment getEnvironment() {
        return (XEnvironment) container.getRequiredService(Services.ENVIRONMENT).getValue();
    }

    @Deployment(name = BUNDLE_A, managed = false, testable = false)
    public static JavaArchive getBundleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, BUNDLE_A);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(SimpleService.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}
