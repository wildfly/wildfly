/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.arquillian.osgi.extension.protocol;

import java.util.Collection;
import java.util.jar.Manifest;

import org.jboss.as.arquillian.osgi.service.ArquillianService;
import org.jboss.as.arquillian.protocol.jmx.JMXProtocolPackager;
import org.jboss.osgi.spi.util.BundleInfo;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ManifestContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class OSGiJMXProtocolPackager extends JMXProtocolPackager {
    protected OSGiJMXProtocolPackager(OSGiJMXProtocolAS7.ServiceArchiveHolder archiveHolder) {
        super(archiveHolder);
    }

    @Override
    protected void addModulesManifestDependencies(final Archive<?> appArchive) {
        if (appArchive instanceof ManifestContainer<?> == false)
            throw new IllegalArgumentException("ManifestContainer expected " + appArchive);

        final Manifest manifest = getOrCreateManifest(appArchive);

        // Don't enrich with Modules Dependencies if this is a OSGi bundle
        if(BundleInfo.isValidBundleManifest(manifest)) {
            return;
        }
        super.addModulesManifestDependencies(appArchive);
    }

    @Override
    protected JavaArchive generateArquillianServiceArchive(Collection<Archive<?>> auxArchives) {
        final JavaArchive archive = super.generateArquillianServiceArchive(auxArchives);
        archive.addPackage(ArquillianService.class.getPackage());
        return archive;
    }

    @Override
    protected CharSequence getDependencies() {
        StringBuffer dependencies = new StringBuffer();
        dependencies.append("org.jboss.as.osgi,");
        dependencies.append("org.jboss.osgi.framework,");
        dependencies.append("org.osgi.core,");
        dependencies.append(super.getDependencies());
        return dependencies;
    }
}
