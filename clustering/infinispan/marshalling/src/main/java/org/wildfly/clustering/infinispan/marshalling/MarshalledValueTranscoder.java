/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
