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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.resolver.v2.MavenCoordinates;
import org.jboss.osgi.resolver.v2.XIdentityCapability;
import org.jboss.osgi.resolver.v2.XRequirementBuilder;
import org.jboss.osgi.resolver.v2.XResource;
import org.jboss.osgi.spi.OSGiManifestBuilder;
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
import org.osgi.framework.resource.Capability;
import org.osgi.framework.resource.Requirement;
import org.osgi.framework.resource.Resource;
import org.osgi.service.repository.Repository;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test that the EventAdmin can be installed through the Repository bundle.
 *
 * @author thomas.diesler@jboss.com
 * @since 19-Jan-2012
 */
@RunWith(Arquillian.class)
public class SimpleRepositoryTestCase {

    @Inject
    public BundleContext context;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-bundle");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(BundleActivator.class, Repository.class, Resource.class);
                builder.addImportPackages(XRequirementBuilder.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testRepositoryService() throws Exception {

        Repository repo = getRepository();
        MavenCoordinates coordinates = MavenCoordinates.parse("org.apache.felix:org.apache.felix.eventadmin:1.2.6");
        Requirement req = XRequirementBuilder.createArtifactRequirement(coordinates);
        assertNotNull("Requirement not null", req);

        Collection<Capability> caps = repo.findProviders(req);
        assertEquals("Capability not null", 1, caps.size());

        XIdentityCapability xcap = (XIdentityCapability) caps.iterator().next();
        assertEquals("org.apache.felix.eventadmin", xcap.getSymbolicName());
        InputStream content = ((XResource)xcap.getResource()).getContent();
        try {
            Bundle bundle = context.installBundle(xcap.getSymbolicName(), content);
            try {
                bundle.start();
                Assert.assertEquals(Bundle.ACTIVE, bundle.getState());
                ServiceReference sref = context.getServiceReference("org.osgi.service.event.EventAdmin");
                assertNotNull("EventAdmin service not null", sref);
            } finally {
                bundle.uninstall();
            }
        } finally {
            content.close();
        }
    }

    private Repository getRepository() {
        ServiceReference sref = context.getServiceReference(Repository.class.getName());
        return (Repository) context.getService(sref);
    }
}
