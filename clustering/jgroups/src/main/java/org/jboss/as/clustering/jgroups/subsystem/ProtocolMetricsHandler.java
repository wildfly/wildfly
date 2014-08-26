/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.ChannelInstanceResource.JGROUPS_PROTOCOL_PKG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.lang.reflect.Field;
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
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jgroups.Channel;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.annotations.Property;
import org.jgroups.conf.PropertyConverter;
import org.jgroups.stack.Protocol;

/**
 * A generic handler for protocol metrics based on reflection.
 * <p/>
 * When a request comes in, it has a protocol name and attribute name
 * - use the protocol name to load the protocol class
 * - use the attribute name and the protocol class to get the attribute field/method
 * - based on the attribute field's type, call
 * field.getInt(protocolObject)
 * field.getFloat(protocolObject)
 * field.getDouble(protocolObject)
 * to get the value of the attribute
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 * @author Radoslav Husar
 */
public class ProtocolMetricsHandler extends AbstractRuntimeOnlyHandler {

    private final boolean isField;

    /**
     * @param isField whether the registered attribute is corresponding to a field,
     *                otherwise a method is invoked
     */
    public ProtocolMetricsHandler(boolean isField) {
        this.isField = isField;
    }

    public enum FieldTypes {
        BOOLEAN("boolean"),
        BYTE("byte"),
        CHAR("char"),
        SHORT("short"),
        INT("int"),
        LONG("long"),
        FLOAT("float"),
        DOUBLE("double"),
        STRING("class java.lang.String"),
        NON_PRIMITIVE("non_primitive");
        private static final Map<String, FieldTypes> MAP = new HashMap<String, FieldTypes>();

        static {
            for (FieldTypes type : FieldTypes.values()) {
                MAP.put(type.toString(), type);
            }
        }

        final String name;

        private FieldTypes(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        public static FieldTypes getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation)
            throws OperationFailedException {

        // get the protocol name and attribute
        PathAddress pathAddress = PathAddress.pathAddress(operation.require(OP_ADDR));
        String channelName = pathAddress.getElement(pathAddress.size() - 2).getValue();
        String protocolName = pathAddress.getElement(pathAddress.size() - 1).getValue();
        String attrName = operation.require(NAME).asString();

        // lookup the channel
        ServiceName channelServiceName = ChannelInstanceResource.CHANNEL_PARENT.append(channelName);
        ServiceController<?> controller = context.getServiceRegistry(false).getService(channelServiceName);

        // check that the service has been installed and started
        boolean started = controller != null && controller.getValue() != null;
        ModelNode result = new ModelNode();

        // load the protocol class and get the attributes
        String className = JGROUPS_PROTOCOL_PKG + "." + protocolName;
        Class<? extends Protocol> protocolClass = null;
        boolean loaded = false;
        try {
            protocolClass = Protocol.class.getClassLoader().loadClass(className).asSubclass(Protocol.class);
            loaded = true;
        } catch (ClassNotFoundException e) {
            context.getFailureDescription().set(JGroupsLogger.ROOT_LOGGER.unableToLoadProtocol(className));
        }

        if (loaded) {
            Field field = null;
            Method method = null;
            boolean found = false;
            // check the attribute is valid
            try {
                if (isField) {
                    field = getField(protocolClass, attrName);
                } else {
                    method = getMethod(protocolClass, attrName);
                }
                found = true;
            } catch (NoSuchFieldException | NoSuchMethodException e) {
                context.getFailureDescription().set(JGroupsLogger.ROOT_LOGGER.unknownMetric(attrName));
            }

            // we now have a valid protocol instance and a valid field, get the value
            if (found && started) {
                Channel channel = (Channel) controller.getValue();

                // we need to strip off any package name before trying to find the protocol
                int index = protocolName.lastIndexOf('.');
                if (index > 0) {
                    protocolName = protocolName.substring(index + 1);
                }
                Protocol protocol = channel.getProtocolStack().findProtocol(protocolName);

                if (protocol == null) {
                    context.getFailureDescription().set(JGroupsLogger.ROOT_LOGGER.protocolNotFoundInStack(protocolName));
                }

                // get the type of the attribute and call the appropriate set method
                FieldTypes type;
                Class forType = isField ? field.getType() : method.getReturnType();
                if (ChannelInstanceResourceDefinition.isEquivalentModelTypeAvailable(forType)) {
                    type = FieldTypes.getStat(forType.toString());
                } else {
                    type = FieldTypes.NON_PRIMITIVE;
                }

                try {
                    result = isField ? getProtocolFieldValue(protocol, field, type) : getProtocolMethodValue(protocol, method, type);
                } catch (PrivilegedActionException pae) {
                    context.getFailureDescription().set(JGroupsLogger.ROOT_LOGGER.privilegedAccessExceptionForAttribute(attrName));
                } catch (InstantiationException ie) {
                    context.getFailureDescription().set(JGroupsLogger.ROOT_LOGGER.instantiationExceptionOnConverterForAttribute(attrName));
                }

                context.getResult().set(result);
            }
        }
        context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
    }

    /**
     * Access to fields in the protocol instance may require security privileges
     * as we may need to adjust the access of private and protected fields using using {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}.
     */
    private static ModelNode getProtocolFieldValue(final Protocol p, final Field f, final FieldTypes t)
            throws PrivilegedActionException, InstantiationException {

        /*
        if (p != null)
            System.out.println("protocol = " + p.getName());
        if (f != null)
            System.out.println("field = " + f.getName());
        if (t != null)
            System.out.println("field type = " + t.toString());
        */

        return AccessController.doPrivileged(new PrivilegedExceptionAction<ModelNode>() {
            @Override
            public ModelNode run() throws Exception {
                ModelNode result = new ModelNode();
                String value;

                if (!f.isAccessible())
                    f.setAccessible(true);

                switch (t) {
                    case BOOLEAN:
                        result.set(f.getBoolean(p));
                        break;
                    case BYTE:
                        result.set(f.getByte(p));
                        break;
                    case CHAR:
                        result.set(f.getChar(p));
                        break;
                    case SHORT:
                        result.set((int) f.getShort(p));
                        break;
                    case INT:
                        result.set(f.getInt(p));
                        break;
                    case LONG:
                        result.set(f.getLong(p));
                        break;
                    case FLOAT:
                        result.set((double) f.getFloat(p));
                        break;
                    case DOUBLE:
                        result.set(f.getDouble(p));
                        break;
                    case STRING:
                        value = (String) f.get(p);
                        if (value != null)
                            result.set(value);
                        break;
                    case NON_PRIMITIVE:
                        // here, we handle all other types using JGroups PropertyConverter to convert the field to String
                        // if the value is null, return undefined
                        value = getStringValue(p, f);
                        if (value != null) {
                            result.set(value);
                        }
                        break;
                }
                return result;
            }
        });
    }


    /**
     * Access to methods in the protocol instance may require security privileges
     * as we may need to adjust the access of private and protected methods using {@link java.lang.reflect.AccessibleObject#setAccessible(boolean)}.
     *
     * @return ModelNode result of the invocation
     */
    private static ModelNode getProtocolMethodValue(final Protocol p, final Method m, final FieldTypes t) throws PrivilegedActionException, InstantiationException {

        return AccessController.doPrivileged(new PrivilegedExceptionAction<ModelNode>() {
            @Override
            public ModelNode run() throws Exception {
                ModelNode result = new ModelNode();
                String value;

                if (!m.isAccessible()) {
                    m.setAccessible(true);
                }

                Object invocationResult = m.invoke(p);

                switch (t) {
                    case BOOLEAN:
                        result.set((boolean) invocationResult);
                        break;
                    case BYTE:
                        result.set((byte) invocationResult);
                        break;
                    case CHAR:
                        result.set((char) invocationResult);
                        break;
                    case SHORT:
                        result.set((short) invocationResult);
                        break;
                    case INT:
                        result.set((int) invocationResult);
                        break;
                    case LONG:
                        result.set((long) invocationResult);
                        break;
                    case FLOAT:
                        result.set((double) invocationResult);
                        break;
                    case DOUBLE:
                        result.set((double) invocationResult);
                        break;
                    case STRING:
                        value = (String) invocationResult;
                        if (value != null) {
                            result.set(value);
                        }
                        break;
                    case NON_PRIMITIVE:
                        result.set(invocationResult.toString());
                        break;
                }
                return result;
            }
        });
    }

    /**
     * Gets a String value for a JGroups non-primitive field, using a JGroups converter of necessary.
     * Note that the value of a non-primitive field may be null, in which case we return the value.
     */
    private static String getStringValue(final Protocol protocol, final Field field)
            throws IllegalAccessException, InstantiationException {
        ManagedAttribute managed = field.getAnnotation(ManagedAttribute.class);
        Property property = field.getAnnotation(Property.class);

        // mutually exclusive: managed attribute and Property?
        assert !((managed != null) && (property != null)) : "Attribute " + field.getName() + " is both property and managed attribute";

        if (managed != null) {
            // handle non-primitive managed attribute
            Object value = field.get(protocol);
            if (value != null) {
                return value.toString();
            }
        }

        if (property != null) {
            // handle non-primitive property value using the converter
            Class<?> converter = property.converter();
            if (converter != null) {
                PropertyConverter instance = (PropertyConverter) converter.newInstance();
                Object value = field.get(protocol);
                if (value != null)
                    return instance.toString(value);
            }
        }
        return null;
    }

    private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getField(superClass, fieldName);
            }
        }
    }

    private static Method getMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getMethod(superClass, methodName);
            }
        }
    }
}
