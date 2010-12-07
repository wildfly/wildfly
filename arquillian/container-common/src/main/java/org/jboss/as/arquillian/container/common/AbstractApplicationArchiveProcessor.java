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
package org.jboss.as.arquillian.container.common;

import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.jboss.arquillian.spi.ApplicationArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;

/**
 * An abstract {@link ApplicationArchiveProcessor}.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @since 17-Nov-2010
 */
public abstract class AbstractApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    protected Manifest getOrCreateManifest(Archive<?> archive) {
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
