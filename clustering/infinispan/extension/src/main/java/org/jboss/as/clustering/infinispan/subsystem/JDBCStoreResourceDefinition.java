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
import org.jboss.as.clustering.controller.validation.EnumValidatorBuilder;
import org.jboss.as.clustering.controller.validation.ParameterValidatorBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;

/**
 * Base class for store resources which require common store attributes and JDBC store attributes
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public abstract class JDBCStoreResourceDefinition extends StoreResourceDefinition {

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DATA_SOURCE("datasource", ModelType.STRING, false),
        DIALECT("dialect", ModelType.STRING, true, new EnumValidatorBuilder<>(DatabaseType.class)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, boolean allowNull) {
            this.definition = createBuilder(name, type, allowNull).build();
        }

        Attribute(String name, ModelType type, boolean allowNull, ParameterValidatorBuilder validator) {
            SimpleAttributeDefinitionBuilder builder = createBuilder(name, type, allowNull);
            this.definition = builder.setValidator(validator.configure(builder).build()).build();
        }

        private static SimpleAttributeDefinitionBuilder createBuilder(String name, ModelType type, boolean allowNull) {
            return new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(allowNull)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            ;
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {

        if (InfinispanModel.VERSION_2_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, Attribute.DIALECT.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, Attribute.DIALECT.getDefinition())
                    .end();
        }

        StoreResourceDefinition.buildTransformation(version, builder);
    }

    JDBCStoreResourceDefinition(PathElement path, InfinispanResourceDescriptionResolver resolver, boolean allowRuntimeOnlyRegistration) {
        super(path, resolver, allowRuntimeOnlyRegistration);
    }
}
