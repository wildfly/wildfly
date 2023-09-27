/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream.net;

import java.net.URI;

import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;

/**
 * Marshaller for a {@link URI}.
 * @author Paul Ferraro
 */
public class URIMarshaller extends FunctionalScalarMarshaller<URI, String> {

    public URIMarshaller() {
        super(URI.class, Scalar.STRING.cast(String.class), URI::toString, URI::create);
    }
}
