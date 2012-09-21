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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;

import java.io.InputStream;

import javax.inject.Inject;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.osgi.api.Echo;
import org.jboss.as.test.integration.osgi.ejb3.bundle.SimpleStatelessSessionBean;
import org.jboss.as.test.integration.osgi.ejb3.bundle.TargetBundleActivator;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Testcase for basic EJB3 / OSGi integration
 *
 * @author thomas.diesler@jboss.com
 * @since 13-May-2011
 */
@RunWith(Arquillian.class)
public class StatelessBeanTestCase {

    static final String EJB3_DEPLOYMENT_NAME = "ejb3-osgi.jar";

    @Inject
    public Bundle bundle;

    @Deployment(order = 1)
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "ejb3-osgi-target");
        archive.addClasses(Echo.class, TargetBundleActivator.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addBundleActivator(TargetBundleActivator.class);
                builder.addImportPackages(BundleActivator.class, Logger.class, Module.class, InitialContext.class);
                builder.addExportPackages(Echo.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Deployment(name = EJB3_DEPLOYMENT_NAME, order = 2, testable = false)
    public static JavaArchive getTestArchive() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, EJB3_DEPLOYMENT_NAME);
        archive.addClass(SimpleStatelessSessionBean.class);
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Bundle.class, Echo.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testTargetBundle() throws Exception {
        bundle.start();
        BundleContext context = bundle.getBundleContext();
        ServiceReference sref = context.getServiceReference(Echo.class.getName());
        Echo service = (Echo) context.getService(sref);
        assertEquals("foo", service.echo("foo"));
    }

    @Test
    public void testStatelessBean() throws Exception {
        String jndiname = "java:global/ejb3-osgi/SimpleStatelessSessionBean!org.jboss.as.test.integration.osgi.ejb3.bundle.SimpleStatelessSessionBean";
        Echo service = (Echo) new InitialContext().lookup(jndiname);
        assertNotNull("StatelessBean not null", service);
        assertEquals("ejb3-osgi-target", service.echo(BUNDLE_SYMBOLICNAME));
        assertEquals("foo", service.echo("foo"));
    }
}
