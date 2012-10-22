package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Resource description for the addressable resource
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/mixed-keyed-jdbc-store=MIXED_KEYED_JDBC_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class MixedKeyedJDBCStoreResource extends BaseJDBCStoreResource {

    private static final PathElement MIXED_KEYED_JDBC_STORE_PATH = PathElement.pathElement(ModelKeys.MIXED_KEYED_JDBC_STORE, ModelKeys.MIXED_KEYED_JDBC_STORE_NAME);
    public static final MixedKeyedJDBCStoreResource INSTANCE = new MixedKeyedJDBCStoreResource();

    // attributes
    static final AttributeDefinition[] MIXED_KEYED_JDBC_STORE_ATTRIBUTES = {STRING_KEYED_TABLE, BINARY_KEYED_TABLE};

    public MixedKeyedJDBCStoreResource() {
        super(MIXED_KEYED_JDBC_STORE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.MIXED_KEYED_JDBC_STORE),
                CacheConfigOperationHandlers.MIXED_KEYED_JDBC_STORE_ADD,
                CacheConfigOperationHandlers.REMOVE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(MIXED_KEYED_JDBC_STORE_ATTRIBUTES);
        for (AttributeDefinition attr : MIXED_KEYED_JDBC_STORE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }
}
