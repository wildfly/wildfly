package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Resource description for the addressable resource
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/string-keyed-jdbc-store=STRING_KEYED_JDBC_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class StringKeyedJDBCStoreResource extends BaseJDBCStoreResource {

    private static final PathElement STRING_KEYED_JDBC_STORE_PATH = PathElement.pathElement(ModelKeys.STRING_KEYED_JDBC_STORE, ModelKeys.STRING_KEYED_JDBC_STORE_NAME);
    public static final StringKeyedJDBCStoreResource INSTANCE = new StringKeyedJDBCStoreResource();

    // attributes
    static final AttributeDefinition[] STRING_KEYED_JDBC_STORE_ATTRIBUTES = {STRING_KEYED_TABLE};

    public StringKeyedJDBCStoreResource() {
        super(STRING_KEYED_JDBC_STORE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.STRING_KEYED_JDBC_STORE),
                CacheConfigOperationHandlers.STRING_KEYED_JDBC_STORE_ADD,
                CacheConfigOperationHandlers.REMOVE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(STRING_KEYED_JDBC_STORE_ATTRIBUTES);
        for (AttributeDefinition attr : STRING_KEYED_JDBC_STORE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
