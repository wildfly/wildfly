/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.marshalling.protostream;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.ProtobufTagMarshaller.OperationContext;
import org.infinispan.protostream.impl.TagWriterImpl;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractProtoStreamOperation implements ProtoStreamOperation, OperationContext {

    private final OperationContext context;

    public AbstractProtoStreamOperation(ImmutableSerializationContext context) {
        this(TagWriterImpl.newInstance(context));
    }

    public AbstractProtoStreamOperation(OperationContext context) {
        this.context = context;
    }

    @Override
    public ImmutableSerializationContext getSerializationContext() {
        return this.context.getSerializationContext();
    }

    @Override
    public Object getParam(Object key) {
        return this.context.getParam(key);
    }

    @Override
    public void setParam(Object key, Object value) {
        this.context.setParam(key, value);
    }
}
