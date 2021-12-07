package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.PathElement;

import java.util.function.UnaryOperator;

/**
 * Definition of the /subsystem=distributable-ejb/client-mappings-registry=local resource.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class LocalClientMappingsRegistryProviderResourceDefinition extends ClientMappingsRegistryProviderResourceDefinition {

    static final PathElement PATH = pathElement("local");

    LocalClientMappingsRegistryProviderResourceDefinition() {
        super(PATH, UnaryOperator.identity(), LocalClientMappingsRegistryProviderServiceConfigurator::new);
    }
}
