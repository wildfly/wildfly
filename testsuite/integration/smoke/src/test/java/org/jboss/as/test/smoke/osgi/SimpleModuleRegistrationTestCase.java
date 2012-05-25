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
package org.jboss.as.test.smoke.osgi;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.smoke.osgi.bundle.SimpleService;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XPackageRequirement;
import org.jboss.osgi.resolver.XResolver;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.resolver.XResourceBuilder;
import org.jboss.osgi.resolver.XResourceBuilderFactory;
import org.jboss.osgi.spi.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolveContext;

/**
 * Test the registration of a non-OSGi deployment.
 *
 * @author thomas.diesler@jboss.com
 */
@RunWith(Arquillian.class)
public class SimpleModuleRegistrationTestCase {

    @Inject
    public ServiceContainer container;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-module-reg");
        archive.addClass(SimpleService.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.osgi.core,org.jboss.osgi.framework");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testFindPackageCapability() throws Exception {

        // Build a package requirement
        XResourceBuilder builder = XResourceBuilderFactory.create();
        builder.addIdentityCapability("somename");
        XPackageRequirement req = builder.addPackageRequirement(SimpleService.class.getPackage().getName());

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

        // Build a resource with a package requirement
        XResourceBuilder builder = XResourceBuilderFactory.create();
        builder.addIdentityCapability("somename");
        builder.addPackageRequirement(SimpleService.class.getPackage().getName());
        Resource resource = builder.getResource();

        // Setup the resolve context
        XResolver resolver = getResolver();
        XEnvironment env = getEnvironment();
        ResolveContext context = resolver.createResolverContext(env, Collections.singleton(resource), null);

        // Find the providers
        Map<Resource, List<Wire>> wiremap = resolver.resolve(context);
        assertEquals(2, wiremap.size());

        // Verify the wires
        List<Wire> wires = wiremap.get(resource);
        assertEquals(1, wires.size());
        Wire wire = wires.get(0);
        assertEquals(resource, wire.getRequirer());
        XResource provider = (XResource) wire.getProvider();
        XIdentityCapability icap = provider.getIdentityCapability();
        assertEquals("deployment.example-module-reg", icap.getSymbolicName());
    }

    private XEnvironment getEnvironment() {
        return (XEnvironment) container.getService(Services.ENVIRONMENT).getValue();
    }

    private XResolver getResolver() {
        ServiceName serviceName = ServiceName.JBOSS.append("osgi", "as", "resolver");
        return (XResolver) container.getService(serviceName).getValue();
    }
}
