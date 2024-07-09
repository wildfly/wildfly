/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;
import org.jgroups.JChannel;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.annotations.Property;
import org.jgroups.stack.Protocol;
import org.jgroups.util.Util;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.common.function.ExceptionFunction;
import org.wildfly.service.capture.FunctionExecutor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * A generic handler for protocol metrics based on reflection.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public class ProtocolMetricsHandler extends AbstractRuntimeOnlyHandler {

    interface Attribute {
        String getName();
        String getDescription();
        Class<?> getType();
        Object read(Object object) throws Exception;
    }

    abstract static class AbstractAttribute<A extends AccessibleObject> implements Attribute {
        final A accessible;

        AbstractAttribute(A accessible) {
            this.accessible = accessible;
        }

        @Override
        public String getName() {
            if (this.accessible.isAnnotationPresent(ManagedAttribute.class)) {
                String name = this.accessible.getAnnotation(ManagedAttribute.class).name();
                if (!name.isEmpty()) return name;
            }
            if (this.accessible.isAnnotationPresent(Property.class)) {
                String name = this.accessible.getAnnotation(Property.class).name();
                if (!name.isEmpty()) return name;
            }
            return null;
        }

        @Override
        public String getDescription() {
            if (this.accessible.isAnnotationPresent(ManagedAttribute.class)) {
                return this.accessible.getAnnotation(ManagedAttribute.class).description();
            }
            if (this.accessible.isAnnotationPresent(Property.class)) {
                return this.accessible.getAnnotation(Property.class).description();
            }
            return this.accessible.toString();
        }

        @Override
        public Object read(final Object object) throws Exception {
            PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<>() {
                @Override
                public Object run() throws Exception {
                    AbstractAttribute.this.accessible.setAccessible(true);
                    try {
                        return AbstractAttribute.this.get(object);
                    } finally {
                        AbstractAttribute.this.accessible.setAccessible(false);
                    }
                }
            };
            try {
                return AccessController.doPrivileged(action);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }

        abstract Object get(Object object) throws Exception;
    }

    static class FieldAttribute extends AbstractAttribute<Field> {
        FieldAttribute(Field field) {
            super(field);
        }

        @Override
        public String getName() {
            String name = super.getName();
            return (name != null) ? name : this.accessible.getName();
        }

        @Override
        public Class<?> getType() {
            return this.accessible.getType();
        }

        @Override
        Object get(Object object) throws IllegalAccessException {
            return this.accessible.get(object);
        }
    }

    static class MethodAttribute extends AbstractAttribute<Method> {
        MethodAttribute(Method method) {
            super(method);
        }

        @Override
        public String getName() {
            String name = super.getName();
            return (name != null) ? name : Util.methodNameToAttributeName(this.accessible.getName());
        }

        @Override
        public Class<?> getType() {
            return this.accessible.getReturnType();
        }

        @Override
        Object get(Object object) throws IllegalAccessException, InvocationTargetException {
            return this.accessible.invoke(object);
        }
    }

    enum FieldType {
        BOOLEAN(ModelType.BOOLEAN, Boolean.TYPE, Boolean.class) {
            @Override
            void setValue(ModelNode node, Object value) {
                node.set((Boolean) value);
            }
        },
        INT(ModelType.INT, Integer.TYPE, Integer.class, Byte.TYPE, Byte.class, Short.TYPE, Short.class) {
            @Override
            void setValue(ModelNode node, Object value) {
                node.set(((Number) value).intValue());
            }
        },
        LONG(ModelType.LONG, Long.TYPE, Long.class) {
            @Override
            void setValue(ModelNode node, Object value) {
                node.set((Long) value);
            }
        },
        DOUBLE(ModelType.DOUBLE, Double.TYPE, Double.class, Float.TYPE, Float.class) {
            @Override
            void setValue(ModelNode node, Object value) {
                node.set(((Number) value).doubleValue());
            }
        },
        STRING(ModelType.STRING) {
            @Override
            void setValue(ModelNode node, Object value) {
                node.set(value.toString());
            }
        },
        ;
        private static final Map<Class<?>, FieldType> TYPES = new HashMap<>();

        static {
            for (FieldType type : FieldType.values()) {
                for (Class<?> classType : type.types) {
                    TYPES.put(classType, type);
                }
            }
        }

        private final Class<?>[] types;
        private final ModelType modelType;

        FieldType(ModelType modelType, Class<?>... types) {
            this.modelType = modelType;
            this.types = types;
        }

        abstract void setValue(ModelNode node, Object value);

        public ModelType getModelType() {
            return this.modelType;
        }

        public static FieldType valueOf(Class<?> typeClass) {
            FieldType type = TYPES.get(typeClass);
            return (type != null) ? type : STRING;
        }
    }

    private final FunctionExecutorRegistry<JChannel> executors;

    public ProtocolMetricsHandler(FunctionExecutorRegistry<JChannel> executors) {
        this.executors = executors;
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        String name = operation.get(ModelDescriptionConstants.NAME).asString();
        String protocolName = context.getCurrentAddressValue();
        ServiceName channelServiceName = JGroupsRequirement.CHANNEL.getServiceName(context, UnaryCapabilityNameResolver.PARENT);
        ExceptionFunction<JChannel, ModelNode, Exception> function = new ExceptionFunction<>() {
            @Override
            public ModelNode apply(JChannel channel) throws Exception {
                int index = protocolName.lastIndexOf('.');
                Protocol protocol = channel.getProtocolStack().findProtocol((index < 0) ? protocolName : protocolName.substring(index + 1));
                if (protocol == null) {
                    throw new IllegalArgumentException(protocolName);
                }
                Attribute attribute = getAttribute(protocol.getClass(), name);
                if (attribute == null) {
                    throw new OperationFailedException(JGroupsLogger.ROOT_LOGGER.unknownMetric(name));
                }
                FieldType type = FieldType.valueOf(attribute.getType());
                ModelNode result = new ModelNode();
                Object value = attribute.read(protocol);
                if (value != null) {
                    type.setValue(result, value);
                }
                return result;
            }
        };
        FunctionExecutor<JChannel> executor = this.executors.getExecutor(ServiceDependency.on(channelServiceName));
        try {
            ModelNode value = (executor != null) ? executor.execute(function) : null;
            if (value != null) {
                context.getResult().set(value);
            }
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        } finally {
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
        }
    }

    static Attribute getAttribute(Class<? extends Protocol> targetClass, String name) {
        Map<String, Attribute> attributes = findProtocolAttributes(targetClass);
        return attributes.get(name);
    }

    static Map<String, Attribute> findProtocolAttributes(Class<? extends Protocol> protocolClass) {
        Map<String, Attribute> attributes = new HashMap<>();
        Class<?> targetClass = protocolClass;
        while (Protocol.class.isAssignableFrom(targetClass)) {
            for (Method method: targetClass.getDeclaredMethods()) {
                if ((method.getParameterCount() == 0) && isManagedAttribute(method)) {
                    putIfAbsent(attributes, new MethodAttribute(method));
                }
            }
            for (Field field: targetClass.getDeclaredFields()) {
                if (isManagedAttribute(field)) {
                    putIfAbsent(attributes, new FieldAttribute(field));
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        return attributes;
    }

    private static void putIfAbsent(Map<String, Attribute> attributes, Attribute attribute) {
        // Some of JGroups @Property-s use '.' in their names (e.g. "timer.queue_max_size") which is disallowed in the domain model,
        // thus we replace all with '-' since JGroups never uses them and this mapping is bijective.
        String name = attribute.getName().replace('.', '-');
        if (!attributes.containsKey(name)) {
            attributes.put(name, attribute);
        }
    }

    private static boolean isManagedAttribute(AccessibleObject object) {
        return object.isAnnotationPresent(ManagedAttribute.class) || (object.isAnnotationPresent(Property.class) && object.getAnnotation(Property.class).exposeAsManagedAttribute());
    }
}
