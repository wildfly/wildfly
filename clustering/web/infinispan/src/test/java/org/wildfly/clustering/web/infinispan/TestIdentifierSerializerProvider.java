/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan;

import java.nio.ByteBuffer;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.spi.Marshaller;
import org.wildfly.clustering.web.IdentifierMarshaller;
import org.wildfly.clustering.web.IdentifierMarshallerProvider;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(IdentifierMarshallerProvider.class)
public class TestIdentifierSerializerProvider implements IdentifierMarshallerProvider {

    @Override
    public Marshaller<String, ByteBuffer> getMarshaller() {
        return IdentifierMarshaller.ISO_LATIN_1;
    }
}
