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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.osgi.bundleA.Echo;
import org.jboss.as.test.integration.osgi.bundleA.RemoteEcho;
import org.jboss.as.test.integration.osgi.bundleA.SampleSFSB;
import org.jboss.as.test.integration.osgi.bundleA.SampleSLSB;
import org.jboss.osgi.spi.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests EJB deployments with OSGi metadata
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Jul-2012
 */
@RunWith(Arquillian.class)
public class SimpleEJBTestCase {

    private static final String MODULE_NAME = "ejb3-test";
    private static final String JAVA_GLOBAL_NAMESPACE_PREFIX = "java:global/";
    private static final String JAVA_APP_NAMESPACE_PREFIX = "java:app/";
    private static final String JAVA_MODULE_NAMESPACE_PREFIX = "java:module/";

    @Deployment
    public static JavaArchive createStandaloneJar() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(RemoteEcho.class, SampleSFSB.class, SampleSLSB.class, Echo.class);
        jar.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleSymbolicName(jar.getName());
                builder.addBundleManifestVersion(2);
                builder.addImportPackages(Context.class);
                return builder.openStream();
            }
        });
        return jar;
    }

    @Test
    public void testBindingsOnSLSB() throws Exception {
        String ejbName = SampleSLSB.class.getSimpleName();
        verifyBindings(ejbName, RemoteEcho.class.getName());
        verifyBindings(ejbName, Echo.class.getName());
        verifyBindings(ejbName, SampleSLSB.class.getName());
    }

    @Test
    public void testBindingsOnSFSB() throws Exception {
        String ejbName = SampleSFSB.class.getSimpleName();
        verifyBindings(ejbName, RemoteEcho.class.getName());
        verifyBindings(ejbName, Echo.class.getName());
        verifyBindings(ejbName, SampleSFSB.class.getName());
    }

    private void verifyBindings(String ejbName, String typeName) throws NamingException {
        Context ctx = new InitialContext();

        String lookup = JAVA_GLOBAL_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + typeName;
        Echo localBusinessInterface = (Echo) ctx.lookup(lookup);
        Assert.assertNotNull("Not null: " + lookup, localBusinessInterface);

        lookup = JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + typeName;
        Echo localBusinessInterfaceInAppNamespace = (Echo) ctx.lookup(lookup);
        Assert.assertNotNull("Not null: " + lookup, localBusinessInterfaceInAppNamespace);

        lookup = JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + typeName;
        Echo localBusinessInterfaceInModuleNamespace = (Echo) ctx.lookup(lookup);
        Assert.assertNotNull("Not null: " + lookup, localBusinessInterfaceInModuleNamespace);
    }
}
