package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.ChannelInstanceCustomResource.JGROUPS_PROTOCOL_PKG;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
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
 * - requires a backing custom resource ChannelInstanceCustomResource to define the children
 * - need a read-only handler to allow read access to attributes, which are obtained by the handler
 * from the underlying services as and when required; they are never made persistent
 * - no write handler required
 * -
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class ChannelInstanceResource extends SimpleResourceDefinition {

    public static final PathElement CHANNEL_PATH = PathElement.pathElement(MetricKeys.CHANNEL);

    private final boolean runtimeRegistration;

    // metrics
    static final SimpleAttributeDefinition STATE =
            new SimpleAttributeDefinitionBuilder(MetricKeys.STATE, ModelType.STRING, true)
                    .setStorageRuntime()
                    .build();
    static final SimpleAttributeDefinition VIEW =
            new SimpleAttributeDefinitionBuilder(MetricKeys.VIEW, ModelType.STRING, true)
                    .setStorageRuntime()
                    .build();

    static final AttributeDefinition[] CHANNEL_METRICS = {STATE, VIEW};

    public ChannelInstanceResource(String channelName, boolean runtimeRegistration) {

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

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);

        // the protocol resources are registered dynamically from ChannelService
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

        List<ModelNode> protocolOrdering = stack.get(ModelKeys.PROTOCOLS).clone().asList();
        // create the protocol definitions for this channel
        final List<ResourceDefinition> protocolDefinitions = new ArrayList<ResourceDefinition>();
        for (ModelNode protocolNameModelNode : protocolOrdering) {
            String protocolName = protocolNameModelNode.asString();
            ResourceDefinition protocolDefinition = getProtocolMetricResourceDefinition(context, channelName, protocolName);
            protocolDefinitions.add(protocolDefinition);
        }

        // register the channel resource and its protocol resources
        ManagementResourceRegistration subsystemRootRegistration = context.getResourceRegistrationForUpdate();
        ManagementResourceRegistration channelRegistration = subsystemRootRegistration.registerSubModel(new ChannelInstanceResource(channelName, true));
        channelRegistration.registerSubModel(transportDefinition);
        for (ResourceDefinition protocolDefinition : protocolDefinitions) {
            channelRegistration.registerSubModel(protocolDefinition);
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
        return (resolvedValue = JGroupsSubsystemRootResource.DEFAULT_STACK.resolveModelAttribute(context, subsystem)).isDefined() ? resolvedValue.asString() : null;
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
            throw new OperationFailedException("can't load protocol class");
        }

        // create the attributes
        Field[] fields = getProtocolFields(protocolClass);

        List<AttributeDefinition> attributes = new ArrayList<AttributeDefinition>();
        for (Field field : fields) {
            boolean equivalentTypeAvailable = isEquivalentModelTypeAvailable(field.getType());

            // add any managed attributes
            ManagedAttribute managed = field.getAnnotation(ManagedAttribute.class);
            if (managed != null && equivalentTypeAvailable) {
                addAttributeDefinition(attributes, field.getName(), getEquivalentModelType(field.getType()), managed.description());
            }

            // add any properties
            Property property = field.getAnnotation(Property.class);
            if (property != null) {
                if (equivalentTypeAvailable) {
                    // add an attribute definition using an equivalent ModelType
                    addAttributeDefinition(attributes, field.getName(), getEquivalentModelType(field.getType()), property.description());
                } else {
                    // add an attribute definition using a String type
                    addAttributeDefinition(attributes, field.getName(), ModelType.STRING, property.description());
                }
            }
        }

        // create the resource
        ResourceBuilder protocolBuilder = ResourceBuilder.Factory.create(PathElement.pathElement(ModelKeys.PROTOCOL, protocolName), new NonResolvingResourceDescriptionResolver());
        // register the resource's attributes
        for (AttributeDefinition def : attributes) {
            protocolBuilder.addMetric(def, new ProtocolMetricsHandler(protocolName));
        }
        // return the resource
        return protocolBuilder.build();
    }

    private static void addAttributeDefinition(List<AttributeDefinition> attributes, String name, ModelType type, String description) {

        SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(name, type, true);
        builder.setStorageRuntime();
        attributes.add(builder.build());
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


    private static Field[] getProtocolFields(Class clazz) {
        List<Field> fields = new ArrayList<Field>();
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        // stop recursing when we reach Protocol
        if (clazz.getSuperclass() != null) {
            fields.addAll(Arrays.asList(getProtocolFields(clazz.getSuperclass())));
        }
        return fields.toArray(new Field[]{});
    }
}
