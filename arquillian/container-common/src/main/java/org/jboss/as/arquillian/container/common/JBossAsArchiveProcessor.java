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
package org.jboss.as.arquillian.container.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.spi.ApplicationArchiveProcessor;
import org.jboss.arquillian.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.container.ManifestContainer;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class JBossAsArchiveProcessor implements ApplicationArchiveProcessor{

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        boolean inContainer = true;
        Run run = testClass.getAnnotation(Run.class);
        if (run != null) {
            inContainer = run.value() == RunModeType.IN_CONTAINER;
        }

        if (inContainer) {
            final Manifest manifest = getOrCreateManifest(applicationArchive);
            Attributes attributes = manifest.getMainAttributes();
            attributes.putValue("Arquillian-deployment", "true");
            ((ManifestContainer<?>)applicationArchive).setManifest(new Asset() {
                public InputStream openStream() {
                    try {
                        manifest.write(System.out);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        manifest.write(baos);
                        return new ByteArrayInputStream(baos.toByteArray());
                    } catch (IOException ex) {
                        throw new IllegalStateException("Cannot write manifest", ex);
                    }
                }
            });
        }
    }

    private Manifest getOrCreateManifest(Archive<?> archive) {
        Manifest manifest;
        try {
            Node node = archive.get ("META-INF/MANIFEST.MF");
            if (node == null) {
                manifest = new Manifest();
                Attributes attributes = manifest.getMainAttributes();
                attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
            } else {
                manifest = new Manifest(node.getAsset().openStream());
            }
            return manifest;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot obtain manifest", ex);
        }
    }
}
