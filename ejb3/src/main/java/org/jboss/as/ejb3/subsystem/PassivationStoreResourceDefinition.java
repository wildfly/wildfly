/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanDefaultCacheRequirement;

/**
 * Definies a CacheFactoryBuilder instance which, during deployment, is used to configure, build and install a CacheFactory for the SFSB being deployed.
 * The CacheFactory produces bean caches which are distributable and have passivation enabled. Used to support CacheFactoryResourceDefinition.
 *
 * @author Paul Ferraro
 */
public class PassivationStoreResourceDefinition extends SimpleResourceDefinition {

    public static final String PASSIVATION_STORE_CAPABILITY_NAME = "org.wildfly.ejb.passivation-store";

    // use these to avoid pulling in ISPN SPI module
    protected static final String INFINISPAN_DEFAULT_CACHE_CONFIGURATION_CAPABILITY_NAME = "org.wildfly.clustering.infinispan.default-cache-configuration";
    protected static final String INFINISPAN_CACHE_CONFIGURATION_CAPABILITY_NAME = "org.wildfly.clustering.infinispan.cache-configuration";

    static final RuntimeCapability<Void> PASSIVATION_STORE_CAPABILITY = RuntimeCapability.Builder.of(PASSIVATION_STORE_CAPABILITY_NAME, true)
            .setServiceType(Void.class)
            .build();

    static final SimpleAttributeDefinition MAX_SIZE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.MAX_SIZE, ModelType.INT, true)
            .setXmlName(EJB3SubsystemXMLAttribute.MAX_SIZE.getLocalName())
            .setDefaultValue(new ModelNode(10000))
            .setAllowExpression(true)
            .setValidator(new LongRangeValidator(0, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    static final SimpleAttributeDefinition CACHE_CONTAINER = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.CACHE_CONTAINER, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.CACHE_CONTAINER.getLocalName())
            .setDefaultValue(new ModelNode(BeanManagerFactoryServiceConfiguratorConfiguration.DEFAULT_CONTAINER_NAME))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            // a CapabilityReference to a UnaryRequirement
            .setCapabilityReference(new CapabilityReference(()->PASSIVATION_STORE_CAPABILITY, InfinispanDefaultCacheRequirement.CONFIGURATION))
            .build();

    static final SimpleAttributeDefinition BEAN_CACHE = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.BEAN_CACHE, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.BEAN_CACHE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            // a CapabilityReference to a BinaryRequirement (including a parent attribute)
            .setCapabilityReference(new CapabilityReference(()->PASSIVATION_STORE_CAPABILITY, InfinispanCacheRequirement.CONFIGURATION, ()->CACHE_CONTAINER))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = { MAX_SIZE, CACHE_CONTAINER, BEAN_CACHE };

    static final PassivationStoreAdd ADD_HANDLER = new PassivationStoreAdd(ATTRIBUTES);

    static final PassivationStoreResourceDefinition INSTANCE = new PassivationStoreResourceDefinition(EJB3SubsystemModel.PASSIVATION_STORE);

    private PassivationStoreResourceDefinition(String element, AttributeDefinition... attributes) {
        super(new Parameters(PathElement.pathElement(element), EJB3Extension.getResourceDescriptionResolver(element))
                .setAddHandler(ADD_HANDLER)
                .setRemoveHandler(new PassivationStoreRemove(ADD_HANDLER, PASSIVATION_STORE_CAPABILITY))
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setCapabilities(PASSIVATION_STORE_CAPABILITY));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition definition: ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(definition, null, writeHandler);
        }
    }
}
