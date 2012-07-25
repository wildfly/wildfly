package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {

    SimpleAttributeDefinition DEFAULT_STACK =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_STACK, ModelType.STRING, false)
                    .setXmlName(Attribute.DEFAULT_STACK.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING, false)
                    .setXmlName(Attribute.NAME.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition TYPE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.TYPE, ModelType.STRING, false)
                    .setXmlName(Attribute.TYPE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition SHARED =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SHARED, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.SHARED.getLocalName())
                    .setAllowExpression(false)
                    .setDefaultValue(new ModelNode().set(true))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition SOCKET_BINDING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SOCKET_BINDING, ModelType.STRING, true)
                    .setXmlName(Attribute.SOCKET_BINDING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition DIAGNOSTICS_SOCKET_BINDING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DIAGNOSTICS_SOCKET_BINDING, ModelType.STRING, true)
                    .setXmlName(Attribute.DIAGNOSTICS_SOCKET_BINDING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition DEFAULT_EXECUTOR =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_EXECUTOR, ModelType.STRING, true)
                    .setXmlName(Attribute.DEFAULT_EXECUTOR.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition OOB_EXECUTOR =
            new SimpleAttributeDefinitionBuilder(ModelKeys.OOB_EXECUTOR, ModelType.STRING, true)
                    .setXmlName(Attribute.OOB_EXECUTOR.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition TIMER_EXECUTOR =
            new SimpleAttributeDefinitionBuilder(ModelKeys.TIMER_EXECUTOR, ModelType.STRING, true)
                    .setXmlName(Attribute.TIMER_EXECUTOR.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition THREAD_FACTORY =
            new SimpleAttributeDefinitionBuilder(ModelKeys.THREAD_FACTORY, ModelType.STRING, true)
                    .setXmlName(Attribute.THREAD_FACTORY.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition SITE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SITE, ModelType.STRING, true)
                    .setXmlName(Attribute.SITE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition RACK =
            new SimpleAttributeDefinitionBuilder(ModelKeys.RACK, ModelType.STRING, true)
                    .setXmlName(Attribute.RACK.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition MACHINE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MACHINE, ModelType.STRING, true)
                    .setXmlName(Attribute.MACHINE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    SimpleAttributeDefinition PROPERTY = new SimpleAttributeDefinition(ModelKeys.PROPERTY, ModelType.PROPERTY, true);

    SimpleListAttributeDefinition PROPERTIES = SimpleListAttributeDefinition.Builder.of(ModelKeys.PROPERTIES, PROPERTY).
            setAllowNull(true).
            build();

    SimpleAttributeDefinition VALUE =
            new SimpleAttributeDefinitionBuilder("value", ModelType.STRING, false)
                    .setXmlName("value")
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    AttributeDefinition[] TRANSPORT_ATTRIBUTES = {TYPE, SHARED, SOCKET_BINDING, DIAGNOSTICS_SOCKET_BINDING, DEFAULT_EXECUTOR,
            OOB_EXECUTOR, TIMER_EXECUTOR, THREAD_FACTORY, SITE, RACK, MACHINE};

    AttributeDefinition[] TRANSPORT_PARAMETERS = {TYPE, SHARED, SOCKET_BINDING, DIAGNOSTICS_SOCKET_BINDING, DEFAULT_EXECUTOR,
            OOB_EXECUTOR, TIMER_EXECUTOR, THREAD_FACTORY, SITE, RACK, MACHINE, PROPERTIES};

    AttributeDefinition[] PROTOCOL_ATTRIBUTES = {TYPE, SOCKET_BINDING};

    AttributeDefinition[] PROTOCOL_PARAMETERS = {TYPE, SOCKET_BINDING, PROPERTIES};

    ObjectTypeAttributeDefinition PROTOCOL = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.PROTOCOL, PROTOCOL_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("protocol").
            build();

    ObjectTypeAttributeDefinition TRANSPORT = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.TRANSPORT, TRANSPORT_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("transport").
            build();

    ObjectListAttributeDefinition PROTOCOLS = ObjectListAttributeDefinition.
            Builder.of(ModelKeys.PROTOCOLS, PROTOCOL).
            setAllowNull(true).
            build();

    AttributeDefinition[] STACK_ATTRIBUTES = { TRANSPORT, PROTOCOLS};

    ObjectTypeAttributeDefinition STACK = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.STACK, STACK_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("stack").
            build();

}
