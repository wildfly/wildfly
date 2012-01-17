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
package org.jboss.as.arquillian.protocol.jmx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.shrinkwrap.api.asset.Asset;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
class ManifestAsset implements Asset {
    private Manifest manifest = new Manifest();

    ManifestAsset() {
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    }

    public Attributes getMainAttributes() {
        return manifest.getMainAttributes();
    }

    @Override
    public InputStream openStream() {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            manifest.write(baos);
            final ByteArrayInputStream stream = new ByteArrayInputStream(baos.toByteArray());
            return stream;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
