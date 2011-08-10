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
package org.jboss.as.arquillian.protocol.jmx;

import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ManifestUtils {

    public static Manifest getManifest(Archive<?> archive, boolean create) {
        Manifest manifest = null;
        try {
            Node node = archive.get(JarFile.MANIFEST_NAME);
            if (node == null) {
                manifest = new Manifest();
                Attributes attributes = manifest.getMainAttributes();
                attributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
            } else if (create) {
                manifest = new Manifest(node.getAsset().openStream());
            }
            return manifest;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot obtain manifest", ex);
        }
    }

    public static Manifest getOrCreateManifest(Archive<?> archive) {
        Manifest manifest;
        try {
            Node node = archive.get(JarFile.MANIFEST_NAME);
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
