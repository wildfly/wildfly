/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * Defines a CacheFactoryBuilder instance which, during deployment, is used to configure, build and install a CacheFactory for the SFSB being deployed.
 * The CacheFactory resource instances defined here produce bean caches which are either:
 * - distributed and have passivation-enabled
 * - non distributed and do not have passivation-enabled
 * For passivation enabled CacheFactoryBuilders, the PassivationStoreResourceDefinition must define a supporting passivation store.
 *
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyCacheFactoryResourceDefinition extends SimpleResourceDefinition {

    // capabilities not required as although we install CacheFactoryBuilder services, these do not depend on any defined clustering resources

    public static final StringListAttributeDefinition ALIASES = new StringListAttributeDefinition.Builder(EJB3SubsystemModel.ALIASES)
            .setXmlName(EJB3SubsystemXMLAttribute.ALIASES.getLocalName())
            .setRequired(false)
            .build();

    public static final SimpleAttributeDefinition PASSIVATION_STORE =
            new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.PASSIVATION_STORE, ModelType.STRING, true)
                    .setXmlName(EJB3SubsystemXMLAttribute.PASSIVATION_STORE_REF.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_NONE)
                    .build();

    private static final AttributeDefinition[] ATTRIBUTES = { ALIASES, PASSIVATION_STORE };
    private static final LegacyCacheFactoryAdd ADD_HANDLER = new LegacyCacheFactoryAdd();
    private static final LegacyCacheFactoryRemove REMOVE_HANDLER = new LegacyCacheFactoryRemove(ADD_HANDLER);

    LegacyCacheFactoryResourceDefinition() {
        super(new SimpleResourceDefinition.Parameters(PathElement.pathElement(EJB3SubsystemModel.CACHE), EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.CACHE))
                .setCapabilities(StatefulSessionBeanCacheProviderResourceDefinition.CAPABILITY)
                .setAddHandler(ADD_HANDLER)
                .setRemoveHandler(REMOVE_HANDLER)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES));
        this.setDeprecated(EJB3Model.VERSION_10_0_0.getVersion());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute: ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute,  null, ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }
}
