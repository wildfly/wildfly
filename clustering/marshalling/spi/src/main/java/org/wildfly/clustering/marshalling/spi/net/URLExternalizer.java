/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.spi.net;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.OptionalInt;

import org.wildfly.clustering.marshalling.Externalizer;

/**
 * Externalizer for a {@link URL}.
 * @author Paul Ferraro
 */
public class URLExternalizer implements Externalizer<URL> {

    @Override
    public void writeObject(ObjectOutput output, URL url) throws IOException {
        try {
            NetExternalizerProvider.URI.cast(URI.class).writeObject(output, url.toURI());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public URL readObject(ObjectInput input) throws IOException, ClassNotFoundException {
        return NetExternalizerProvider.URI.cast(URI.class).readObject(input).toURL();
    }

    @Override
    public Class<URL> getTargetClass() {
        return URL.class;
    }

    @Override
    public OptionalInt size(URL url) {
        try {
            return NetExternalizerProvider.URI.cast(URI.class).size(url.toURI());
        } catch (URISyntaxException e) {
            return OptionalInt.empty();
        }
    }
}
