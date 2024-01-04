/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.wildfly.clustering.marshalling.protostream.FunctionalMarshaller;
import org.wildfly.common.function.ExceptionFunction;

/**
 * Marshaller for a {@link URL}.
 * @author Paul Ferraro
 */
public class URLMarshaller extends FunctionalMarshaller<URL, URI> {

    private static final ExceptionFunction<URL, URI, IOException> TO_URI = new ExceptionFunction<>() {
        @Override
        public URI apply(URL url) throws IOException {
            try {
                return url.toURI();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
    };

    public URLMarshaller() {
        super(URL.class, URI.class, TO_URI, URI::toURL);
    }
}
