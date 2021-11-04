package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.InfinispanDefaultCacheRequirement;

import java.util.function.UnaryOperator;

/**
 * Definition of the /subsystem=distributable-ejb/infinispan-client-mappings-registry resource.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanClientMappingsRegistryProviderResourceDefinition extends ClientMappingsRegistryProviderResourceDefinition {

    static final PathElement PATH = pathElement("infinispan");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CACHE_CONTAINER("cache-container", ModelType.STRING) {

            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setCapabilityReference(new CapabilityReference(Capability.CLIENT_MAPPINGS_REGISTRY_PROVIDER, InfinispanDefaultCacheRequirement.CONFIGURATION))
                        ;
            }
        },
        CACHE("cache", ModelType.STRING) {

            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setCapabilityReference(new CapabilityReference(Capability.CLIENT_MAPPINGS_REGISTRY_PROVIDER, InfinispanCacheRequirement.CONFIGURATION, CACHE_CONTAINER))
                        ;
            }
        }
        ;

        private final AttributeDefinition definition ;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    public InfinispanClientMappingsRegistryProviderResourceDefinition() {
        super(PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class), InfinispanClientMappingsRegistryProviderServiceConfigurator::new);
    }
}
