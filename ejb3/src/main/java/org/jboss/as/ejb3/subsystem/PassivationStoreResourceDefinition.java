/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;

/**
 * @author Paul Ferraro
 */
public class PassivationStoreResourceDefinition extends SimpleResourceDefinition {

    static final SimpleAttributeDefinition MAX_SIZE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.MAX_SIZE, ModelType.INT, true)
            .setXmlName(EJB3SubsystemXMLAttribute.MAX_SIZE.getLocalName())
            .setDefaultValue(new ModelNode(10000))
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(0, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build()
    ;
    static final SimpleAttributeDefinition CACHE_CONTAINER = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CACHE_CONTAINER, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.CACHE_CONTAINER.getLocalName())
            .setDefaultValue(new ModelNode(BeanManagerFactoryServiceConfiguratorConfiguration.DEFAULT_CONTAINER_NAME))
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

    static final PassivationStoreAdd ADD_HANDLER = new PassivationStoreAdd(ATTRIBUTES);

    static final PassivationStoreResourceDefinition INSTANCE = new PassivationStoreResourceDefinition(EJB3SubsystemModel.PASSIVATION_STORE);

    private PassivationStoreResourceDefinition(String element, AttributeDefinition... attributes) {
        super(new Parameters(PathElement.pathElement(element), EJB3Extension.getResourceDescriptionResolver(element))
                .setAddHandler(ADD_HANDLER)
                .setRemoveHandler(new PassivationStoreRemove(ADD_HANDLER))
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition definition: ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(definition, null, writeHandler);
        }
    }
}
