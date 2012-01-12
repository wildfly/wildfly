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
package org.jboss.as.test.integration.osgi.jndi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A test that deployes a bundle that exercises the {@link InitialContext}
 *
 * @author thomas.diesler@jboss.com
 * @author David Bosschaert
 * @since 05-May-2009
 */
@RunWith(Arquillian.class)
public class JNDITestCase {

    @Inject
    public Bundle bundle;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "example-jndi");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(InitialContext.class);
                builder.addImportPackages(InitialContextFactoryBuilder.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testJNDIAccess() throws Exception {
        bundle.start();
        InitialContext iniCtx = getInitialContext(bundle.getBundleContext());
        Object lookup = iniCtx.lookup("java:jboss");
        assertNotNull("Lookup not null", lookup);

        final String value = "Bar";
        iniCtx.createSubcontext("test").bind("Foo", value);
        assertEquals(value, iniCtx.lookup("test/Foo"));
    }

    @Test
    public void testInitialContextFactoryBuilderService() throws Exception {
        bundle.start();
        BundleContext context = bundle.getBundleContext();
        ServiceReference ref = context.getServiceReference(InitialContextFactoryBuilder.class.getName());
        InitialContextFactoryBuilder builder = (InitialContextFactoryBuilder) context.getService(ref);

        InitialContextFactory factory = builder.createInitialContextFactory(null);
        Context iniCtx = factory.getInitialContext(null);

        Object lookup = iniCtx.lookup("java:jboss");
        assertNotNull("Lookup not null", lookup);
    }

    private InitialContext getInitialContext(BundleContext context) {
        ServiceReference sref = context.getServiceReference(InitialContext.class.getName());
        return (InitialContext) context.getService(sref);
    }
}
