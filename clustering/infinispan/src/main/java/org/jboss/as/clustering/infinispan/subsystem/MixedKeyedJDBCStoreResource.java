package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

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

    // operations
    private static final OperationDefinition MIXED_KEYED_JDBC_STORE_ADD_DEFINITION = new SimpleOperationDefinitionBuilder(ADD, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.MIXED_KEYED_JDBC_STORE))
        .setParameters(COMMON_STORE_PARAMETERS)
        .addParameter(DATA_SOURCE)
        .addParameter(STRING_KEYED_TABLE)
        .addParameter(BINARY_KEYED_TABLE)
        .setAttributeResolver(InfinispanExtension.getResourceDescriptionResolver(ModelKeys.JDBC_STORE))
        .build();


    public MixedKeyedJDBCStoreResource() {
        super(MIXED_KEYED_JDBC_STORE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.MIXED_KEYED_JDBC_STORE),
                CacheConfigOperationHandlers.MIXED_KEYED_JDBC_STORE_ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
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

    // override the add operation to provide a custom definition (for the optional PROPERTIES parameter to add())
    @Override
    protected void registerAddOperation(final ManagementResourceRegistration registration, final OperationStepHandler handler, OperationEntry.Flag... flags) {
        registration.registerOperationHandler(MIXED_KEYED_JDBC_STORE_ADD_DEFINITION, handler);
    }

}
