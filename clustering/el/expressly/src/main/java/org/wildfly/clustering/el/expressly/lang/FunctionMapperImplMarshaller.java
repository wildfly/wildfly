/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly.lang;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.glassfish.expressly.lang.FunctionMapperImpl;
import org.glassfish.expressly.lang.FunctionMapperImpl.Function;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.SimpleObjectOutput;

/**
 * @author Paul Ferraro
 */
public class FunctionMapperImplMarshaller implements ProtoStreamMarshaller<FunctionMapperImpl> {

    private static final int FUNCTION_INDEX = 1;

    @Override
    public Class<? extends FunctionMapperImpl> getJavaClass() {
        return FunctionMapperImpl.class;
    }

    @Override
    public FunctionMapperImpl readFrom(ProtoStreamReader reader) throws IOException {
        List<Function> functions = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case FUNCTION_INDEX:
                    functions.add(reader.readObject(Function.class));
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        FunctionMapperImpl mapper = new FunctionMapperImpl();
        for (Function function : functions) {
            String[] strings = new String[4];
            function.writeExternal(new SimpleObjectOutput.Builder().with(strings).build());

            String prefix = strings[0];
            String localName = strings[1];
            Method method = function.getMethod();
            mapper.addFunction(!prefix.isEmpty() ? prefix : null, localName, method);
        }
        return mapper;
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, FunctionMapperImpl value) throws IOException {
        Object[] objects = new Object[1];
        value.writeExternal(new SimpleObjectOutput.Builder().with(objects).build());

        @SuppressWarnings("unchecked")
        Map<String, Function> functions = (Map<String, Function>) objects[0];

        if (functions != null) {
            for (Function function : functions.values()) {
                writer.writeObject(FUNCTION_INDEX, function);
            }
        }
    }
}
