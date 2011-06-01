/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.arquillian.protocol.jmx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.Asset;

/**
 * A simple OSGi manifest builder.
 *
 * @author thomas.diesler@jboss.com
 * @since 08-Mar-2010
 */
public final class ManifestBuilder implements Asset {

    // Provide logging
    private static final Logger log = Logger.getLogger(ManifestBuilder.class);

    private StringWriter sw;
    private PrintWriter pw;
    private Manifest manifest;

    public static ManifestBuilder newInstance() {
        return new ManifestBuilder();
    }

    private ManifestBuilder() {
        sw = new StringWriter();
        pw = new PrintWriter(sw);
        append(Attributes.Name.MANIFEST_VERSION + ": 1.0", true);
    }

    public ManifestBuilder addManifestHeader(String key, String value) {
        append(key + ": " + value, true);
        return this;
    }

    public Manifest getManifest() {
        if (manifest == null) {
            String manifestString = sw.toString();
            if (log.isTraceEnabled())
                log.trace(manifestString);

            try {
                manifest = new Manifest(new ByteArrayInputStream(manifestString.getBytes()));
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot create manifest", ex);
            }
        }
        return manifest;
    }

    @Override
    public InputStream openStream() {
        Manifest manifest = getManifest();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            manifest.write(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot provide manifest InputStream", ex);
        }
    }

    private void append(String line, boolean newline) {
        if (manifest != null)
            throw new IllegalStateException("Cannot append to already existing manifest");

        if (line != null)
            pw.print(line);
        if (newline == true)
            pw.println();
    }
}
