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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.jboss.as.clustering.jgroups.JGroupsMessages;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.annotations.ManagedAttribute;
import org.jgroups.annotations.Property;
import org.jgroups.stack.Protocol;

/**
 * Resource representing a run-time only channel instance.
 * <p/>
 * Some points to note:
 * - user cannot add/remove channels, so no need for add/remove handlers
 * - requires a backing custom resource ChannelInstanceResource to define the children
 * - need a read-only handler to allow read access to attributes, which are obtained by the handler
 * from the underlying services as and when required; they are never made persistent
 * - creates attribute definitions and description resolver dynamically
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ChannelInstanceResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement CHANNEL_PATH = PathElement.pathElement(MetricKeys.CHANNEL);

    private final boolean runtimeRegistration;

    // metrics
    static final SimpleAttributeDefinition ADDRESS =
            new SimpleAttributeDefinitionBuilder(MetricKeys.ADDRESS, ModelType.STRING, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition ADDRESS_AS_UUID =
            new SimpleAttributeDefinitionBuilder(MetricKeys.ADDRESS_AS_UUID, ModelType.STRING, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition DISCARD_OWN_MESSAGES =
            new SimpleAttributeDefinitionBuilder(MetricKeys.DISCARD_OWN_MESSAGES, ModelType.BOOLEAN, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition NUM_TASKS_IN_TIMER =
            new SimpleAttributeDefinitionBuilder(MetricKeys.NUM_TASKS_IN_TIMER, ModelType.INT, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition NUM_TIMER_THREADS =
            new SimpleAttributeDefinitionBuilder(MetricKeys.NUM_TIMER_THREADS, ModelType.INT, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition RECEIVED_BYTES =
            new SimpleAttributeDefinitionBuilder(MetricKeys.RECEIVED_BYTES, ModelType.LONG, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition RECEIVED_MESSAGES =
            new SimpleAttributeDefinitionBuilder(MetricKeys.RECEIVED_MESSAGES, ModelType.LONG, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition SENT_BYTES =
            new SimpleAttributeDefinitionBuilder(MetricKeys.SENT_BYTES, ModelType.LONG, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition SENT_MESSAGES =
            new SimpleAttributeDefinitionBuilder(MetricKeys.SENT_MESSAGES, ModelType.LONG, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition STATE =
            new SimpleAttributeDefinitionBuilder(MetricKeys.STATE, ModelType.STRING, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition STATS_ENABLED =
            new SimpleAttributeDefinitionBuilder(MetricKeys.STATS_ENABLED, ModelType.BOOLEAN, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition VERSION =
            new SimpleAttributeDefinitionBuilder(MetricKeys.VERSION, ModelType.STRING, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition VIEW =
            new SimpleAttributeDefinitionBuilder(MetricKeys.VIEW, ModelType.STRING, true)
                    .setStorageRuntime()
                    .build();

    static final AttributeDefinition[] CHANNEL_METRICS = {ADDRESS, ADDRESS_AS_UUID, DISCARD_OWN_MESSAGES, NUM_TASKS_IN_TIMER,
            NUM_TIMER_THREADS, RECEIVED_BYTES, RECEIVED_MESSAGES, SENT_BYTES, SENT_MESSAGES, STATE, STATS_ENABLED, VERSION, VIEW};

    public ChannelInstanceResourceDefinition(String channelName, boolean runtimeRegistration) {

        super(PathElement.pathElement(MetricKeys.CHANNEL, channelName),
                JGroupsExtension.getResourceDescriptionResolver(MetricKeys.CHANNEL),
                null,
                null);
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // register any metrics and the read-only handler
        if (runtimeRegistration) {
            for (AttributeDefinition attr : CHANNEL_METRICS) {
                resourceRegistration.registerMetric(attr, ChannelMetricsHandler.INSTANCE);
            }
        }
    }

    /*
     *  Add an operation step to register a channel and the protocols for the channel, in the context of the resource
     *  /subsystem=jgroups/channel=<channel name>
     *
     *  This step is required as channels are registered from /subsystem=infinispan/cache-container=* which does
     *  not provide direct access to the ManagementResourceRegistration for JGroups; so we execute the step to
     *  provide access to /subsystem=jgroups, the root subsystem resource.
     */
    public static void addChannelProtocolMetricsRegistrationStep(OperationContext context, String channelName, String stackName) {

        // set up the operation for the step
        PathAddress rootSubsystemAddress = PathAddress.pathAddress(JGroupsExtension.SUBSYSTEM_PATH);
        ModelNode registerProtocolsOp = Util.createOperation("some_operation_name", rootSubsystemAddress);
        if (stackName != null)
            registerProtocolsOp.get(ModelKeys.STACK).set(new ModelNode(stackName));
        registerProtocolsOp.get(MetricKeys.CHANNEL).set(new ModelNode(channelName));

        // add the step
        context.addStep(registerProtocolsOp, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                String channelName = operation.get(MetricKeys.CHANNEL).asString();
                String stackName = null;
                if (operation.get(ModelKeys.STACK).isDefined()) {
                    stackName = operation.get(ModelKeys.STACK).asString();
                }
                registerChannelProtocolMetrics(context, channelName, stackName);
                context.stepCompleted();
            }
        }, OperationContext.Stage.RUNTIME);
    }

    public static void registerChannelProtocolMetrics(OperationContext context, String channelName, String stackName)
            throws OperationFailedException {
        if (stackName == null) {
            stackName = getDefaultStack(context);
        }
        // get the stack model
        PathElement stackElement = PathElement.pathElement(ModelKeys.STACK, stackName);
        PathAddress stackPath = PathAddress.pathAddress(PathAddress.pathAddress(JGroupsExtension.SUBSYSTEM_PATH), stackElement);
        ModelNode stack = Resource.Tools.readModel(context.readResourceFromRoot(stackPath, true));

        // get the transport
        ModelNode transport = stack.get(ModelKeys.TRANSPORT, ModelKeys.TRANSPORT_NAME).clone();
        String transportName = (String) transport.get(ModelKeys.TYPE).asString();
        ResourceDefinition transportDefinition = getProtocolMetricResourceDefinition(context, channelName, transportName);

        List<org.jboss.dmr.Property> protocolOrdering = stack.get(ModelKeys.PROTOCOL).asPropertyList();
        // create the protocol definitions for this channel
        final List<ResourceDefinition> protocolDefinitions = new ArrayList<ResourceDefinition>();
        for (org.jboss.dmr.Property protocol : protocolOrdering) {
            String protocolName = protocol.getName();
            ResourceDefinition protocolDefinition = getProtocolMetricResourceDefinition(context, channelName, protocolName);
            protocolDefinitions.add(protocolDefinition);
        }

        // create the relay resource definition if element is defined
        ResourceDefinition relayDefinition = null ;
        if (stack.hasDefined(ModelKeys.RELAY)) {
            relayDefinition = getProtocolMetricResourceDefinition(context, channelName, "relay.RELAY2");
        }

        // register the channel resource and its protocol resources
        ManagementResourceRegistration subsystemRootRegistration = context.getResourceRegistrationForUpdate();
        ManagementResourceRegistration channelRegistration = subsystemRootRegistration.registerSubModel(new ChannelInstanceResourceDefinition(channelName, true));
        channelRegistration.registerSubModel(transportDefinition);
        for (ResourceDefinition protocolDefinition : protocolDefinitions) {
            channelRegistration.registerSubModel(protocolDefinition);
        }
        if (stack.hasDefined(ModelKeys.RELAY)) {
            channelRegistration.registerSubModel(relayDefinition);
        }
    }

    public static void addChannelProtocolMetricsDeregistrationStep(OperationContext context, String channelName) {
        // do what is required to de-register
    }

    /*
     * Method to pick up the default stack from the JGroups subsystem.
     */
    private static String getDefaultStack(OperationContext context) throws OperationFailedException {
        ModelNode resolvedValue = null;
        PathAddress subsystemPath = PathAddress.pathAddress(JGroupsExtension.SUBSYSTEM_PATH);
        ModelNode subsystem = Resource.Tools.readModel(context.readResourceFromRoot(subsystemPath));
        return (resolvedValue = JGroupsSubsystemRootResourceDefinition.DEFAULT_STACK.resolveModelAttribute(context, subsystem)).isDefined() ? resolvedValue.asString() : null;
    }

    /*
     * This dynamically creates a protocol=* resource definition by doing the following:
     * - load the class for the protocol
     * - use reflection to get the fields (their name and type) annotated as @ManagedAttribute or @Property
     * - create a ResourceDefinition and add in the attributes
     */
    private static ResourceDefinition getProtocolMetricResourceDefinition(OperationContext context, String channelName, String protocolName) throws OperationFailedException {

        String className = JGROUPS_PROTOCOL_PKG + "." + protocolName;

        // load the protocol class and get the attributes
        Class<? extends Protocol> protocolClass = null;
        try {
            protocolClass = Protocol.class.getClassLoader().loadClass(className).asSubclass(Protocol.class);
        } catch (Exception e) {
            throw JGroupsMessages.MESSAGES.unableToLoadProtocolClass(className);
        }

        // create the attributes
        Field[] fields = getProtocolFields(protocolClass);

        List<AttributeDefinition> attributes = new ArrayList<AttributeDefinition>();
        Map<String, String> attributeDescriptionMap = new HashMap<String,String>();
        // add the description of the resource
        attributeDescriptionMap.put(protocolName, "The " + protocolName + " protocol");

        for (Field field : fields) {
            boolean equivalentTypeAvailable = isEquivalentModelTypeAvailable(field.getType());

            // add any managed attributes
            ManagedAttribute managed = field.getAnnotation(ManagedAttribute.class);
            if (managed != null && equivalentTypeAvailable) {
                addAttributeDefinition(attributes, attributeDescriptionMap, protocolName, field.getName(), getEquivalentModelType(field.getType()), managed.description());
            }

            // add any properties
            Property property = field.getAnnotation(Property.class);
            if (property != null) {
                if (equivalentTypeAvailable) {
                    // add an attribute definition using an equivalent ModelType
                    addAttributeDefinition(attributes, attributeDescriptionMap, protocolName, field.getName(), getEquivalentModelType(field.getType()), property.description());
                } else {
                    // add an attribute definition using a String type
                    addAttributeDefinition(attributes, attributeDescriptionMap, protocolName, field.getName(), ModelType.STRING, property.description());
                }
            }
        }

        // create the resource using a custom ResourceDescriptionResolver
        // NOTE: do we need to be careful about creating the ResourceBundle vs supplying it?
        ResourceBuilder protocolBuilder = ResourceBuilder.Factory.create(PathElement.pathElement(ModelKeys.PROTOCOL, protocolName),
                new StandardResourceDescriptionResolver(protocolName, "org.jboss.as.clustering.jgroups.subsystem.ChannelInstanceResourceDefinition$ProtocolResources", ChannelInstanceResourceDefinition.class.getClassLoader()));

        // register the resource's attributes
        for (AttributeDefinition def : attributes) {
            protocolBuilder.addMetric(def, new ProtocolMetricsHandler());
        }

        // add the attribute descriptions to the map
        ProtocolResources.addProtocolMapEntries(attributeDescriptionMap);

        // return the resource
        return protocolBuilder.build();
    }

    private static void addAttributeDefinition(List<AttributeDefinition> attributes, Map<String, String> map, String protocolName, String name, ModelType type, String description) {

        // create and add attribute
        SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, type, true);
        builder.setStorageRuntime();
        attributes.add(builder.build());

        // add description to map
        map.put(protocolName+"."+name, description);
    }

    public static boolean isEquivalentModelTypeAvailable(Class<?> type) {
        // short, int, long ranges of integers and basic strings
        String[] equivalentTypes = {"int", "short", "long", "float", "double", "boolean", "class java.lang.String"};
        boolean available = false;
        for (String equivalentType : equivalentTypes) {
            if (type.toString().equals(equivalentType)) {
                available = true;
                break;
            }
        }
        return available;
    }

    /*
     * Returns the equivalent ModelType for a Java type if it exists;
     * Otherwise returns ModelType.UNDEFINED
     */
    private static ModelType getEquivalentModelType(Class<?> typeClass) {
        ProtocolMetricsHandler.FieldTypes type = ProtocolMetricsHandler.FieldTypes.getStat(typeClass.toString());
        switch (type) {
            case BOOLEAN:
                return ModelType.BOOLEAN;
            case BYTE:
                return ModelType.UNDEFINED;
            case CHAR:
                return ModelType.UNDEFINED;
            case SHORT:
                return ModelType.INT;
            case INT:
                return ModelType.INT;
            case LONG:
                return ModelType.LONG;
            case FLOAT:
                return ModelType.DOUBLE;
            case DOUBLE:
                return ModelType.DOUBLE;
            case STRING:
                return ModelType.STRING;
            default:
                return ModelType.UNDEFINED;
        }
    }

    private static Field[] getProtocolFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        // stop recursing when we reach Protocol
        if (clazz.getSuperclass() != null) {
            fields.addAll(Arrays.asList(getProtocolFields(clazz.getSuperclass())));
        }
        return fields.toArray(new Field[]{});
    }

    /*
     * A ResourceBundle which holds all protocol attribute descriptions, prefixed by
     * the protocol name itself:
     *
     * MPING.name=...
     * MPING.timeout=...
     * ...
     * PING.name=...
     * ...
     * This allows us to create a ResourceBundle dynamically, based on attribute descriptions
     * obtained from the protocol @Property and @ManagedAttribute annotations.
     */
    public static class ProtocolResources extends ResourceBundle {

       private static Map<String,String> resources = new HashMap<String,String>() ;

       public static void addProtocolMapEntries(Map<String,String> map) {
           resources.putAll(map);
       }

       public Object handleGetObject(String key) {
           return resources.get(key);
       }

       public Enumeration<String> getKeys() {
           return Collections.enumeration(keySet());
       }

       protected Set<String> handleKeySet() {
           return resources.keySet();
       }
    }
}
