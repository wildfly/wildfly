/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly.lang;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.glassfish.expressly.lang.FunctionMapperImpl.Function;
import org.infinispan.protostream.descriptors.WireType;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamReader;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamWriter;
import org.wildfly.clustering.marshalling.protostream.SimpleObjectOutput;

/**
 * @author Paul Ferraro
 */
public class FunctionMarshaller implements ProtoStreamMarshaller<Function> {

    private static final int PREFIX_INDEX = 1;
    private static final int LOCAL_NAME_INDEX = 2;
    private static final int DECLARING_CLASS_INDEX = 3;
    private static final int METHOD_NAME_INDEX = 4;
    private static final int PARAMETER_TYPE_INDEX = 5;

    @Override
    public Class<? extends Function> getJavaClass() {
        return Function.class;
    }

    @Override
    public Function readFrom(ProtoStreamReader reader) throws IOException {
        String prefix = null;
        String localName = null;
        Class<?> declaringClass = null;
        String methodName = null;
        List<Class<?>> parameterTypes = new LinkedList<>();
        while (!reader.isAtEnd()) {
            int tag = reader.readTag();
            switch (WireType.getTagFieldNumber(tag)) {
                case PREFIX_INDEX:
                    prefix = reader.readString();
                    break;
                case LOCAL_NAME_INDEX:
                    localName = reader.readString();
                    break;
                case DECLARING_CLASS_INDEX:
                    declaringClass = reader.readAny(Class.class);
                    break;
                case METHOD_NAME_INDEX:
                    methodName = reader.readString();
                    break;
                case PARAMETER_TYPE_INDEX:
                    parameterTypes.add(reader.readAny(Class.class));
                    break;
                default:
                    reader.skipField(tag);
            }
        }
        try {
            Method method = (declaringClass != null) ? declaringClass.getDeclaredMethod(methodName, parameterTypes.toArray(new Class<?>[0])) : null;
            return new Function(prefix, localName, method);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void writeTo(ProtoStreamWriter writer, Function function) throws IOException {
        String[] strings = new String[4];
        function.writeExternal(new SimpleObjectOutput.Builder().with(strings).build());

        String prefix = strings[0];
        if (!prefix.isEmpty()) {
            writer.writeString(PREFIX_INDEX, prefix);
        }
        String localName = strings[1];
        if (localName != null) {
            writer.writeString(LOCAL_NAME_INDEX, localName);
        }
        Method method = function.getMethod();
        if (method != null) {
            writer.writeAny(DECLARING_CLASS_INDEX, method.getDeclaringClass());
            writer.writeString(METHOD_NAME_INDEX, method.getName());
            for (Class<?> parameterType : method.getParameterTypes()) {
                writer.writeAny(PARAMETER_TYPE_INDEX, parameterType);
            }
        }
    }
}
