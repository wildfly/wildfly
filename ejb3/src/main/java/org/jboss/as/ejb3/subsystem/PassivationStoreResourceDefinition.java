/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
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
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
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

}
