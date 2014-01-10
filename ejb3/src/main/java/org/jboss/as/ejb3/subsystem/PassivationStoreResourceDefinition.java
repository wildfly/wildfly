/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.ejb3.subsystem;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
public class PassivationStoreResourceDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition MAX_SIZE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.MAX_SIZE, ModelType.INT, true)
            .setXmlName(EJB3SubsystemXMLAttribute.MAX_SIZE.getLocalName())
            .setDefaultValue(new ModelNode(10000))
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(0, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .build()
    ;
    static final SimpleAttributeDefinition CACHE_CONTAINER = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CACHE_CONTAINER, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.CACHE_CONTAINER.getLocalName())
            .setDefaultValue(new ModelNode("ejb"))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build()
    ;
    static final SimpleAttributeDefinition BEAN_CACHE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.BEAN_CACHE, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.BEAN_CACHE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build()
    ;

    static final AttributeDefinition[] ATTRIBUTES = { MAX_SIZE, CACHE_CONTAINER, BEAN_CACHE };
    static final AttributeDefinition[] READ_ONLY_ATTRIBUTES = { CACHE_CONTAINER, BEAN_CACHE };
    static final AttributeDefinition[] READ_WRITE_ATTRIBUTES = { MAX_SIZE };

    static final PassivationStoreAdd ADD_HANDLER = new PassivationStoreAdd(ATTRIBUTES);
    static final PassivationStoreRemove REMOVE_HANDLER = new PassivationStoreRemove(ADD_HANDLER);
    private static final PassivationStoreWriteHandler WRITE_HANDLER = new PassivationStoreWriteHandler(READ_WRITE_ATTRIBUTES);

    static final PassivationStoreResourceDefinition INSTANCE = new PassivationStoreResourceDefinition(EJB3SubsystemModel.PASSIVATION_STORE);

    private PassivationStoreResourceDefinition(String element) {
        super(PathElement.pathElement(element), EJB3Extension.getResourceDescriptionResolver(element), ADD_HANDLER, REMOVE_HANDLER, OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition definition: READ_ONLY_ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(definition, null);
        }
        for (AttributeDefinition definition: READ_WRITE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(definition, null, WRITE_HANDLER);
        }
    }

    /*
     * This transformer does the following:
     * - maps <passivation-store/> to <cluster-passivation-store/>
     * - sets appropriate defaults for IDLE_TIMEOUT, IDLE_TIMEOUT_UNIT, PASSIVATE_EVENTS_ON_REPLICATE, and CLIENT_MAPPINGS_CACHE
     */
    @SuppressWarnings("deprecation")
    static void registerTransformers_1_1_0(ResourceTransformationDescriptionBuilder parent) {

        ResourceTransformationDescriptionBuilder child = parent.addChildRedirection(INSTANCE.getPathElement(), PathElement.pathElement(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE));
        child.getAttributeBuilder()
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode(true), true), EJB3SubsystemModel.PASSIVATE_EVENTS_ON_REPLICATE)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode("default"), true), EJB3SubsystemModel.CLIENT_MAPPINGS_CACHE)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode().set(Integer.MAX_VALUE), true), EJB3SubsystemModel.IDLE_TIMEOUT)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode().set(TimeUnit.SECONDS.name()), true), EJB3SubsystemModel.IDLE_TIMEOUT_UNIT)
        ;
    }

    /*
     * This transformer does the following:
     * - maps <passivation-store/> to <cluster-passivation-store/>
     * - sets appropriate defaults for IDLE_TIMEOUT, IDLE_TIMEOUT_UNIT, PASSIVATE_EVENTS_ON_REPLICATE, and CLIENT_MAPPINGS_CACHE
     */
    @SuppressWarnings("deprecation")
    static void registerTransformers_1_2_0(ResourceTransformationDescriptionBuilder parent) {

        ResourceTransformationDescriptionBuilder child = parent.addChildRedirection(INSTANCE.getPathElement(), PathElement.pathElement(EJB3SubsystemModel.CLUSTER_PASSIVATION_STORE));
        child.getAttributeBuilder()
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode(true), true), EJB3SubsystemModel.PASSIVATE_EVENTS_ON_REPLICATE)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode("default"), true), EJB3SubsystemModel.CLIENT_MAPPINGS_CACHE)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode().set(Integer.MAX_VALUE), true), EJB3SubsystemModel.IDLE_TIMEOUT)
                .setValueConverter(AttributeConverter.Factory.createHardCoded(new ModelNode().set(TimeUnit.SECONDS.name()), true), EJB3SubsystemModel.IDLE_TIMEOUT_UNIT)
        ;
    }
}
