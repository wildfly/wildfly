/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.testsuite.integration.as858;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleSpec;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.osgi.framework.ModuleLoaderProvider;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.testing.ManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [AS7-858] Cannot load module when applying resolver results
 *
 * https://issues.jboss.org/browse/AS7-858
 *
 * @author Thomas.Diesler@jboss.com
 */
@RunWith(Arquillian.class)
public class AS858TestCase {

    @Inject
    public ServiceContainer serviceContainer;

    @Deployment
    public static Archive<?> deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "as858");
        archive.add(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.jboss.as.osgi,org.jboss.osgi.framework");
                return builder.openStream();
            }}, JarFile.MANIFEST_NAME);
        return archive;
    }

    @Test
    public void testModuleLoader() throws NamingException, Exception {
        ServiceController<?> controller = serviceContainer.getService(Services.MODULE_LOADER_PROVIDER);
        ModuleLoaderProvider loaderProvider = (ModuleLoaderProvider) controller.getValue();

        ModuleIdentifier identifier = ModuleIdentifier.create("deployment.as858.test");
        ModuleSpec moduleSpec = ModuleSpec.build(identifier).create();
        loaderProvider.addModule(moduleSpec);
        Module module = loaderProvider.getModuleLoader().loadModule(identifier);
        assertEquals(identifier, module.getIdentifier());
    }
}
