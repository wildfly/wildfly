/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling.spi.net;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;

/**
 * Externalizer for a {@link URL}.
 * @author Paul Ferraro
 */
public class URLExternalizer implements Externalizer<URL> {

    @Override
    public void writeObject(ObjectOutput output, URL url) throws IOException {
        try {
            DefaultExternalizer.URI.cast(URI.class).writeObject(output, url.toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public URL readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return DefaultExternalizer.URI.cast(URI.class).readObject(input).toURL();
    }

    @Override
    public Class<URL> getTargetClass() {
        return URL.class;
    }
}
