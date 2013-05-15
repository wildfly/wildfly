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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.osgi.classloading.suba.TypeA;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XBundleRevisionBuilder;
import org.jboss.osgi.resolver.XBundleRevisionBuilderFactory;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
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
    static final String MODULE_A = "module-a";

    @ArquillianResource
    ServiceContainer serviceContainer;

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-module-reg");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core,org.jboss.osgi.framework");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testResourceCapabilities() throws Exception {
        deployer.deploy(MODULE_A);
        try {
            // Build a package requirement
            XBundleRevisionBuilder builder = XBundleRevisionBuilderFactory.create();
            builder.addCapability(IdentityNamespace.IDENTITY_NAMESPACE, "somename");
            XRequirement req = builder.addRequirement(PackageNamespace.PACKAGE_NAMESPACE, "org.jboss.as.test.integration.osgi.classloading.suba");
            builder.getResource();

            // Find the providers for the requirement
            List<Capability> caps = getEnvironment().findProviders(req);
            Assert.assertEquals(1, caps.size());

            // Verify resource identity
            XBundleRevision brev = (XBundleRevision) caps.get(0).getResource();
            XIdentityCapability icap = brev.getIdentityCapability();
            Assert.assertEquals("deployment.module-a", icap.getName());
            Assert.assertEquals(Version.emptyVersion, icap.getVersion());
            Assert.assertEquals(XResource.TYPE_MODULE, icap.getType());

            caps = brev.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
            Assert.assertEquals("One exported package capability", 1, caps.size());
        } finally {
            deployer.undeploy(MODULE_A);
        }
    }

    @Test
    public void testResolveModule() throws Exception {
        deployer.deploy(MODULE_A);
        try {
            // Install a Bundle that has a package requirement on
            // on a capability exported from the module deployment
            InputStream inputA = deployer.getDeployment(BUNDLE_A);
            Bundle bundleA = context.installBundle(BUNDLE_A, inputA);
            try {
                FrameworkWiring fwkWiring = context.getBundle().adapt(FrameworkWiring.class);
                Assert.assertTrue(fwkWiring.resolveBundles(Collections.singleton(bundleA)));
                BundleWiring wiringA = bundleA.adapt(BundleWiring.class);
                List<BundleWire> wires = wiringA.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
                Assert.assertEquals(1, wires.size());
                BundleWire wire = wires.get(0);
                Assert.assertEquals(bundleA, wire.getRequirer().getBundle());
                XBundleRevision provider = (XBundleRevision) wire.getProvider();
                XIdentityCapability icap = provider.getIdentityCapability();
                Assert.assertEquals("deployment.module-a", icap.getName());
            } finally {
                bundleA.uninstall();
            }
        } finally {
            deployer.undeploy(MODULE_A);
        }
    }

    private XEnvironment getEnvironment() {
        return (XEnvironment) serviceContainer.getRequiredService(Services.ENVIRONMENT).getValue();
    }

    @Deployment(name = MODULE_A, managed = false, testable = false)
    public static JavaArchive getModuleA() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, MODULE_A);
        archive.addClass(TypeA.class);
        return archive;
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
                builder.addImportPackages(TypeA.class);
                return builder.openStream();
            }
        });
        return archive;
    }
}
