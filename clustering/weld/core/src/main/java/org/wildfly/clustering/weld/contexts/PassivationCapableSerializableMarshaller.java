/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.weld.contexts;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.PassivationCapable;

import org.infinispan.protostream.descriptors.WireType;
import org.jboss.weld.Container;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.jboss.weld.serialization.spi.helpers.SerializableContextual;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;

/**
 * @author Paul Ferraro
 */
public class PassivationCapableSerializableMarshaller<SC extends SerializableContextual<C, I> & PassivationCapable, C extends Contextual<I> & PassivationCapable, I> implements ProtoStreamMarshaller<SC> {

    private static final int CONTEXT_INDEX = 1;
    private static final int CONTEXTUAL_INDEX = 2;
    private static final int IDENTIFIER_INDEX = 3;

    private final Class<SC> targetClass;
    private final BiFunction<String, C, SC> factory;
    private final Function<SC, String> contextFunction;

    PassivationCapableSerializableMarshaller(Class<SC> targetClass, BiFunction<String, C, SC> factory, Function<SC, String> contextFunction) {
        this.targetClass = targetClass;
        this.factory = factory;
        this.contextFunction = contextFunction;
    }

    @Override
    public Class<SC> getJavaClass() {
        return this.targetClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public SC readFrom(ProtoStreamReader reader) throws IOException {
        String contextId = null;
        C contextual = null;
        String identifier = null;
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case CONTEXT_INDEX:
                    contextId = reader.readAny(String.class);
                    break;
                case CONTEXTUAL_INDEX:
                    contextual = (C) reader.readAny();
                    break;
                case IDENTIFIER_INDEX:
                    identifier = reader.readAny(String.class);
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        if (contextual == null) {
            contextual = Container.instance(contextId).services().get(ContextualStore.class).getContextual(identifier);
        }
        return this.factory.apply(contextId, contextual);
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, SC contextual) throws IOException {
        writer.writeAny(CONTEXT_INDEX, this.contextFunction.apply(contextual));
        if (writer.getSerializationContext().canMarshall(contextual.get())) {
            writer.writeAny(CONTEXTUAL_INDEX, contextual.get());
        } else {
            writer.writeAny(IDENTIFIER_INDEX, contextual.getId());
        }
    }
}
