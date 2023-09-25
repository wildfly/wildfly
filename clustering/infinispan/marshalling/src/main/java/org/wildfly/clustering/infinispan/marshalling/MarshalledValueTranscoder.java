/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.marshalling;

import static org.infinispan.commons.logging.Log.CONTAINER;

import java.io.IOException;

import org.infinispan.commons.CacheException;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.OneToManyTranscoder;
import org.infinispan.commons.util.Util;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;

/**
 * A transcoder that converts between an object and a {@link MarshalledValue}.
 * @author Paul Ferraro
 */
public class MarshalledValueTranscoder<C> extends OneToManyTranscoder {

    private final MarshalledValueFactory<C> factory;
    private final MediaType type;

    public MarshalledValueTranscoder(MediaType type, MarshalledValueFactory<C> factory) {
        super(type, MediaType.APPLICATION_OBJECT);
        this.type = type;
        this.factory = factory;
    }

    @Override
    protected Object doTranscode(Object content, MediaType contentType, MediaType destinationType) {
        if (this.type.match(destinationType)) {
            if (this.type.match(contentType)) {
                return content;
            }
            @SuppressWarnings("unchecked")
            MarshalledValue<Object, C> value = (MarshalledValue<Object, C>) content;
            try {
                return value.get(this.factory.getMarshallingContext());
            } catch (IOException e) {
                throw new CacheException(e);
            }
        }
        if (destinationType.match(MediaType.APPLICATION_OBJECT)) {
            return this.factory.createMarshalledValue(content);
        }
        throw CONTAINER.unsupportedConversion(Util.toStr(content), contentType, destinationType);
    }
}
