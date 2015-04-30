/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.persistence.jdbc.DatabaseType;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Base class for store resources which require common store attributes and JDBC store attributes
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JDBCStoreResourceDefinition extends StoreResourceDefinition {

    static final SimpleAttributeDefinition DATA_SOURCE = new SimpleAttributeDefinitionBuilder(ModelKeys.DATASOURCE, ModelType.STRING, false)
            .setXmlName(Attribute.DATASOURCE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition DIALECT = new SimpleAttributeDefinitionBuilder(ModelKeys.DIALECT, ModelType.STRING, true)
            .setXmlName(Attribute.DIALECT.getLocalName())
            .setValidator(new EnumValidator<>(DatabaseType.class, true, true))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { DATA_SOURCE, DIALECT };

    static final SimpleAttributeDefinition BATCH_SIZE = new SimpleAttributeDefinitionBuilder(ModelKeys.BATCH_SIZE, ModelType.INT, true)
            .setXmlName(Attribute.BATCH_SIZE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(100))
            .build();

    static final SimpleAttributeDefinition FETCH_SIZE = new SimpleAttributeDefinitionBuilder(ModelKeys.FETCH_SIZE, ModelType.INT, true)
            .setXmlName(Attribute.FETCH_SIZE.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(100))
            .build();

    static final SimpleAttributeDefinition PREFIX = new SimpleAttributeDefinitionBuilder(ModelKeys.PREFIX, ModelType.STRING, true)
            .setXmlName(Attribute.PREFIX.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    static final SimpleAttributeDefinition COLUMN_NAME = new SimpleAttributeDefinitionBuilder("name", ModelType.STRING, true)
            .setXmlName("name")
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set("name"))
            .build();

    static final SimpleAttributeDefinition COLUMN_TYPE = new SimpleAttributeDefinitionBuilder("type", ModelType.STRING, true)
            .setXmlName("type")
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set("type"))
            .build();

    static final AttributeDefinition[] COLUMN_ATTRIBUTES = new AttributeDefinition[] { COLUMN_NAME, COLUMN_TYPE };

    static final ObjectTypeAttributeDefinition ID_COLUMN = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.ID_COLUMN, COLUMN_ATTRIBUTES)
            .setAllowNull(true)
            .setSuffix("column")
            .build();

    static final ObjectTypeAttributeDefinition DATA_COLUMN = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.DATA_COLUMN, COLUMN_ATTRIBUTES)
            .setAllowNull(true)
            .setSuffix("column")
            .build();

    static final ObjectTypeAttributeDefinition TIMESTAMP_COLUMN = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.TIMESTAMP_COLUMN, COLUMN_ATTRIBUTES)
            .setAllowNull(true)
            .setSuffix("column")
            .build();

    static final AttributeDefinition[] TABLE_ATTRIBUTES = new AttributeDefinition[] { PREFIX, BATCH_SIZE, FETCH_SIZE, ID_COLUMN, DATA_COLUMN, TIMESTAMP_COLUMN };

    @Deprecated
    static final ObjectTypeAttributeDefinition ENTRY_TABLE = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.ENTRY_TABLE, TABLE_ATTRIBUTES)
            .setAllowNull(true)
            .setSuffix("table")
            .build();

    @Deprecated
    static final ObjectTypeAttributeDefinition BUCKET_TABLE = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.BUCKET_TABLE, TABLE_ATTRIBUTES)
            .setAllowNull(true)
            .setSuffix("table")
            .build();

    static final ObjectTypeAttributeDefinition STRING_KEYED_TABLE = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.STRING_KEYED_TABLE, TABLE_ATTRIBUTES)
            .setAllowNull(true)
            .setSuffix("table")
            .build();

    static final ObjectTypeAttributeDefinition BINARY_KEYED_TABLE = ObjectTypeAttributeDefinition.Builder.of(ModelKeys.BINARY_KEYED_TABLE, TABLE_ATTRIBUTES)
            .setAllowNull(true)
            .setSuffix("table")
            .build();

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        if (InfinispanModel.VERSION_2_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, DIALECT)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, DIALECT);
        }

        StoreResourceDefinition.buildTransformation(version, builder);
    }

    JDBCStoreResourceDefinition(StoreType store, boolean allowRuntimeOnlyRegistration) {
        super(store, allowRuntimeOnlyRegistration);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        super.registerAttributes(registration);
        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attr : ATTRIBUTES) {
            registration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }
}
