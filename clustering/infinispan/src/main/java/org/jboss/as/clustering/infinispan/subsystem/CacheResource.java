package org.jboss.as.clustering.infinispan.subsystem;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Base class for cache resources which require common cache attributes only.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheResource extends SimpleResourceDefinition {

    // attributes
    static final SimpleAttributeDefinition BATCHING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.BATCHING, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.BATCHING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();

    static final SimpleAttributeDefinition CACHE_MODULE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MODULE, ModelType.STRING, true)
                    .setXmlName(Attribute.MODULE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new ModuleIdentifierValidator(true))
                    .build();

    static final SimpleAttributeDefinition INDEXING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.INDEXING, ModelType.STRING, true)
                    .setXmlName(Attribute.INDEX.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<Indexing>(Indexing.class, true, false))
                    .setDefaultValue(new ModelNode().set(Indexing.NONE.name()))
                    .build();

    static final SimpleMapAttributeDefinition INDEXING_PROPERTIES = new SimpleMapAttributeDefinition.Builder(ModelKeys.INDEXING_PROPERTIES, true)
            .setAllowExpression(true)
            .setAttributeMarshaller(new AttributeMarshaller() {
                @Override
                public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
                    resourceModel = resourceModel.get(attribute.getName());
                    if (!resourceModel.isDefined()) {
                        return;
                    }
                    for (Property property : resourceModel.asPropertyList()) {
                        writer.writeStartElement(org.jboss.as.controller.parsing.Element.PROPERTY.getLocalName());
                        writer.writeAttribute(org.jboss.as.controller.parsing.Element.NAME.getLocalName(), property.getName());
                        writer.writeCharacters(property.getValue().asString());
                        writer.writeEndElement();
                    }
                }
            })
            .build();

    static final SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.JNDI_NAME, ModelType.STRING, true)
                    .setXmlName(Attribute.JNDI_NAME.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final SimpleAttributeDefinition START =
            new SimpleAttributeDefinitionBuilder(ModelKeys.START, ModelType.STRING, true)
                    .setXmlName(Attribute.START.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<StartMode>(StartMode.class, true, false))
                    .setDefaultValue(new ModelNode().set(StartMode.LAZY.name()))
                    .build();

    static final AttributeDefinition[] CACHE_ATTRIBUTES = {BATCHING, CACHE_MODULE, INDEXING, INDEXING_PROPERTIES, JNDI_NAME, START};

    // here for legacy purposes only
    static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING, true)
                    .setXmlName(Attribute.NAME.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    public CacheResource(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, AbstractAddStepHandler addHandler, OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // do we really need a special handler here?
        final OperationStepHandler writeHandler = new CacheWriteAttributeHandler(CACHE_ATTRIBUTES);
        for (AttributeDefinition attr : CACHE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, CacheReadAttributeHandler.INSTANCE, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);

        resourceRegistration.registerSubModel(LockingResource.INSTANCE);
        resourceRegistration.registerSubModel(TransactionResource.INSTANCE);
        resourceRegistration.registerSubModel(EvictionResource.INSTANCE);
        resourceRegistration.registerSubModel(ExpirationResource.INSTANCE);
        resourceRegistration.registerSubModel(StoreResource.INSTANCE);
        resourceRegistration.registerSubModel(FileStoreResource.INSTANCE);
        resourceRegistration.registerSubModel(StringKeyedJDBCStoreResource.INSTANCE);
        resourceRegistration.registerSubModel(BinaryKeyedJDBCStoreResource.INSTANCE);
        resourceRegistration.registerSubModel(MixedKeyedJDBCStoreResource.INSTANCE);
        resourceRegistration.registerSubModel(RemoteStoreResource.INSTANCE);
    }
}
