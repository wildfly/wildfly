package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Resource description for the addressable resource
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/binary-keyed-jdbc-store=BINARY_KEYED_JDBC_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class BinaryKeyedJDBCStoreResource extends BaseJDBCStoreResource {

    private static final PathElement BINARY_KEYED_JDBC_STORE_PATH = PathElement.pathElement(ModelKeys.BINARY_KEYED_JDBC_STORE, ModelKeys.BINARY_KEYED_JDBC_STORE_NAME);
    public static final BinaryKeyedJDBCStoreResource INSTANCE = new BinaryKeyedJDBCStoreResource();

    // attributes
    static final AttributeDefinition[] BINARY_KEYED_JDBC_STORE_ATTRIBUTES = {BINARY_KEYED_TABLE};

    public BinaryKeyedJDBCStoreResource() {
        super(BINARY_KEYED_JDBC_STORE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.BINARY_KEYED_JDBC_STORE),
                CacheConfigOperationHandlers.BINARY_KEYED_JDBC_STORE_ADD,
                CacheConfigOperationHandlers.REMOVE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(BINARY_KEYED_JDBC_STORE_ATTRIBUTES);
        for (AttributeDefinition attr : BINARY_KEYED_JDBC_STORE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
