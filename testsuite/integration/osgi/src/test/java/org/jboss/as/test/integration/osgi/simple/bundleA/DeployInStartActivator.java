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
package org.jboss.as.test.integration.osgi.simple.bundleA;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import org.jboss.osgi.repository.XRequirementBuilder;
import org.jboss.osgi.resolver.MavenCoordinates;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.resource.Capability;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;

public class DeployInStartActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws BundleException {

        Repository repo = getRepository(context);
        MavenCoordinates coordinates = MavenCoordinates.parse("org.apache.felix:org.apache.felix.eventadmin:1.2.6");
        XRequirement req = XRequirementBuilder.create(coordinates).getRequirement();
        assertNotNull("Requirement not null", req);

        Collection<Capability> caps = repo.findProviders(Collections.singleton(req)).get(req);
        assertEquals("Capability not null", 1, caps.size());

        XIdentityCapability xcap = (XIdentityCapability) caps.iterator().next();
        assertEquals("org.apache.felix.eventadmin", xcap.getSymbolicName());
        InputStream content = ((RepositoryContent) xcap.getResource()).getContent();
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
            try {
                content.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public void stop(BundleContext context) {
    }

    private Repository getRepository(BundleContext context) {
        ServiceReference sref = context.getServiceReference(Repository.class.getName());
        return (Repository) context.getService(sref);
    }
}