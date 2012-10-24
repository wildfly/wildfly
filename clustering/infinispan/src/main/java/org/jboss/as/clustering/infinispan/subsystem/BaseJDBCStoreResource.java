package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for store resources which require common store attributes and JDBC store attributes
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class BaseJDBCStoreResource extends BaseStoreResource {

    // attributes
    static final SimpleAttributeDefinition DATA_SOURCE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DATASOURCE, ModelType.STRING, false)
                    .setXmlName(Attribute.DATASOURCE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    static final AttributeDefinition[] COMMON_JDBC_STORE_ATTRIBUTES = {DATA_SOURCE};

    static final SimpleAttributeDefinition BATCH_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.BATCH_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.BATCH_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(100))
                    .build();
    static final SimpleAttributeDefinition FETCH_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.FETCH_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.FETCH_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(100))
                    .build();
    static final SimpleAttributeDefinition PREFIX =
            new SimpleAttributeDefinitionBuilder(ModelKeys.PREFIX, ModelType.STRING, true)
                    .setXmlName(Attribute.PREFIX.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
//                   .setDefaultValue(new ModelNode().set("ispn_bucket"))
//                   .setDefaultValue(new ModelNode().set("ispn_entry"))
                    .build();

    static final SimpleAttributeDefinition COLUMN_NAME = new SimpleAttributeDefinition("column-name", ModelType.STRING, true);

    static final SimpleAttributeDefinition COLUMN_TYPE = new SimpleAttributeDefinition("column-type", ModelType.STRING, true);

    static final ObjectTypeAttributeDefinition ID_COLUMN = ObjectTypeAttributeDefinition.
            Builder.of("id-column", COLUMN_NAME, COLUMN_TYPE).
            setAllowNull(true).
            setSuffix("column").
            build();

    static final ObjectTypeAttributeDefinition DATA_COLUMN = ObjectTypeAttributeDefinition.
            Builder.of("data-column", COLUMN_NAME, COLUMN_TYPE).
            setAllowNull(true).
            setSuffix("column").
            build();

    static final ObjectTypeAttributeDefinition TIMESTAMP_COLUMN = ObjectTypeAttributeDefinition.
            Builder.of("timestamp-column", COLUMN_NAME, COLUMN_TYPE).
            setAllowNull(true).
            setSuffix("column").
            build();

    static final ObjectTypeAttributeDefinition ENTRY_TABLE = ObjectTypeAttributeDefinition.
            Builder.of("entry-table", PREFIX, BATCH_SIZE, FETCH_SIZE, ID_COLUMN, DATA_COLUMN, TIMESTAMP_COLUMN).
            setAllowNull(true).
            setSuffix("table").
            build();

    static final ObjectTypeAttributeDefinition BUCKET_TABLE = ObjectTypeAttributeDefinition.
            Builder.of("bucket-table", PREFIX, BATCH_SIZE, FETCH_SIZE, ID_COLUMN, DATA_COLUMN, TIMESTAMP_COLUMN).
            setAllowNull(true).
            setSuffix("table").
            build();

    static final ObjectTypeAttributeDefinition STRING_KEYED_TABLE = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.STRING_KEYED_TABLE, PREFIX, BATCH_SIZE, FETCH_SIZE, ID_COLUMN, DATA_COLUMN, TIMESTAMP_COLUMN).
            setAllowNull(true).
            setSuffix("table").
            build();

    static final ObjectTypeAttributeDefinition BINARY_KEYED_TABLE = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.BINARY_KEYED_TABLE, PREFIX, BATCH_SIZE, FETCH_SIZE, ID_COLUMN, DATA_COLUMN, TIMESTAMP_COLUMN).
            setAllowNull(true).
            setSuffix("table").
            build();


    public BaseJDBCStoreResource(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(COMMON_JDBC_STORE_ATTRIBUTES);
        for (AttributeDefinition attr : COMMON_JDBC_STORE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

}
