/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.datasources.agroal.logging.AgroalLogger;

import javax.sql.DataSource;
import java.util.EnumSet;

import static io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation.*;
import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;

/**
 * Common Definition for the datasource resource
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
abstract class AbstractDataSourceDefinition extends PersistentResourceDefinition {

    private static final String DATA_SOURCE_CAPABILITY_NAME = "org.wildfly.data-source";

    public static final RuntimeCapability<Void> DATA_SOURCE_CAPABILITY = RuntimeCapability.Builder.of(DATA_SOURCE_CAPABILITY_NAME, true, DataSource.class).build();

    static final String AUTHENTICATION_CONTEXT_CAPABILITY = "org.wildfly.security.authentication-context";

    static final SimpleAttributeDefinition JNDI_NAME_ATTRIBUTE = create("jndi-name", ModelType.STRING)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setValidator(new ParameterValidator() {
                @Override
                public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
                    if (value.getType() != ModelType.EXPRESSION) {
                        String str = value.asString();
                        if (!str.startsWith("java:/") && !str.startsWith("java:jboss/")) {
                            throw AgroalLogger.SERVICE_LOGGER.jndiNameInvalidFormat();
                        } else if (str.endsWith("/") || str.contains("//")) {
                            throw AgroalLogger.SERVICE_LOGGER.jndiNameShouldValidate();
                        }
                    }
                }
            })
            .build();

    static final SimpleAttributeDefinition STATISTICS_ENABLED_ATTRIBUTE = create("statistics-enabled", ModelType.BOOLEAN)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(false))
            .setRequired(false)
            .build();

    // --- connection-factory attributes //

    static final SimpleAttributeDefinition DRIVER_ATTRIBUTE = create("driver", ModelType.STRING)
            .setRequires()
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .build();

    static final SimpleAttributeDefinition URL_ATTRIBUTE = create("url", ModelType.STRING)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setRequired(false)
            .setValidator(new StringLengthValidator(1))
            .build();

    static final SimpleAttributeDefinition TRANSACTION_ISOLATION_ATTRIBUTE = create("transaction-isolation", ModelType.STRING)
            .setAllowExpression(true)
            .setAllowedValues(NONE.name(), READ_UNCOMMITTED.name(), READ_COMMITTED.name(), REPEATABLE_READ.name(), SERIALIZABLE.name())
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(EnumValidator.create(AgroalConnectionFactoryConfiguration.TransactionIsolation.class, EnumSet.allOf(AgroalConnectionFactoryConfiguration.TransactionIsolation.class)))
            .build();

    static final SimpleAttributeDefinition NEW_CONNECTION_SQL_ATTRIBUTE = create("new-connection-sql", ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition USERNAME_ATTRIBUTE = create("username", ModelType.STRING)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAlternatives("elytron-domain")
            .setAllowExpression(true)
            .setAttributeGroup("security")
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .build();

    static final SimpleAttributeDefinition PASSWORD_ATTRIBUTE = create("password", ModelType.STRING)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
            .addAlternatives(CredentialReference.CREDENTIAL_REFERENCE)
            .setAllowExpression(true)
            .setAttributeGroup("security")
            .setRequired(false)
            .setRequires(USERNAME_ATTRIBUTE.getName())
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .build();

    static SimpleAttributeDefinition AUTHENTICATION_CONTEXT = new SimpleAttributeDefinitionBuilder("authentication-context", ModelType.STRING, true)
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.AUTHENTICATION_CLIENT_REF)
            .addAlternatives(USERNAME_ATTRIBUTE.getName())
            .setCapabilityReference(AUTHENTICATION_CONTEXT_CAPABILITY, DATA_SOURCE_CAPABILITY)
            .setRestartAllServices()
            .build();

    static final ObjectTypeAttributeDefinition CREDENTIAL_REFERENCE = CredentialReference.getAttributeBuilder(true, true)
                                                                                         .addAlternatives(PASSWORD_ATTRIBUTE.getName())
                                                                                         .setAttributeGroup("security")
                                                                                         .build();

    static final PropertiesAttributeDefinition CONNECTION_PROPERTIES_ATTRIBUTE = new PropertiesAttributeDefinition.Builder("connection-properties", true)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final ObjectTypeAttributeDefinition CONNECTION_FACTORY_ATTRIBUTE = ObjectTypeAttributeDefinition.create("connection-factory", DRIVER_ATTRIBUTE, URL_ATTRIBUTE, TRANSACTION_ISOLATION_ATTRIBUTE, NEW_CONNECTION_SQL_ATTRIBUTE, USERNAME_ATTRIBUTE, PASSWORD_ATTRIBUTE, AUTHENTICATION_CONTEXT, CREDENTIAL_REFERENCE, CONNECTION_PROPERTIES_ATTRIBUTE)
            .setRestartAllServices()
            .build();

    // --- connection-pool attributes //

    static final SimpleAttributeDefinition MAX_SIZE_ATTRIBUTE = create("max-size", ModelType.INT)
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(0) )
            .build();

    static final SimpleAttributeDefinition MIN_SIZE_ATTRIBUTE = create("min-size", ModelType.INT)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(0))
            .setRequired(false)
            .setValidator(new IntRangeValidator(0) )
            .build();

    static final SimpleAttributeDefinition INITIAL_SIZE_ATTRIBUTE = create("initial-size", ModelType.INT)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(0))
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0) )
            .build();

    // Agroal will validate that min-size <= initial-size <= max-size

    static final SimpleAttributeDefinition BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE = create("blocking-timeout", ModelType.INT)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(0))
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setRequired(false)
            .build();

    static final SimpleAttributeDefinition BACKGROUND_VALIDATION_ATTRIBUTE = create("background-validation", ModelType.INT)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(0))
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition LEAK_DETECTION_ATTRIBUTE = create("leak-detection", ModelType.INT)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(0))
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final SimpleAttributeDefinition IDLE_REMOVAL_ATTRIBUTE = create("idle-removal", ModelType.INT)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(0))
            .setMeasurementUnit(MeasurementUnit.MINUTES)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    static final ObjectTypeAttributeDefinition CONNECTION_POOL_ATTRIBUTE = ObjectTypeAttributeDefinition.create("connection-pool", MAX_SIZE_ATTRIBUTE, MIN_SIZE_ATTRIBUTE, INITIAL_SIZE_ATTRIBUTE, BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE, BACKGROUND_VALIDATION_ATTRIBUTE, LEAK_DETECTION_ATTRIBUTE, IDLE_REMOVAL_ATTRIBUTE)
            .build();

    // --- Operations //

    private static final OperationDefinition FLUSH_ALL = new SimpleOperationDefinitionBuilder("flush-all", AgroalExtension.getResolver()).build();

    private static final OperationDefinition FLUSH_GRACEFUL = new SimpleOperationDefinitionBuilder("flush-graceful", AgroalExtension.getResolver()).build();

    private static final OperationDefinition FLUSH_INVALID = new SimpleOperationDefinitionBuilder("flush-invalid", AgroalExtension.getResolver()).build();

    private static final OperationDefinition FLUSH_IDLE = new SimpleOperationDefinitionBuilder("flush-idle", AgroalExtension.getResolver()).build();

    private static final OperationDefinition RESET_STATISTICS = new SimpleOperationDefinitionBuilder("reset-statistics", AgroalExtension.getResolver()).build();

    private static final OperationDefinition TEST_CONNECTION = new SimpleOperationDefinitionBuilder("test-connection", AgroalExtension.getResolver()).build();

    // --- Runtime attributes //

    private static final PrimitiveListAttributeDefinition STATISTICS = PrimitiveListAttributeDefinition.Builder.of("statistics", ModelType.LIST)
            .setRequired(false)
            .setStorageRuntime()
            .build();

    // --- //

    protected AbstractDataSourceDefinition(PathElement pathElement, ResourceDescriptionResolver descriptionResolver, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super(pathElement, descriptionResolver, addHandler, removeHandler);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        ReloadRequiredWriteAttributeHandler handler = new ReloadRequiredWriteAttributeHandler(getAttributes());
        for (AttributeDefinition attributeDefinition : getAttributes()) {
            OperationStepHandler writeHandler = handler;

            if (attributeDefinition == STATISTICS_ENABLED_ATTRIBUTE) {
                writeHandler = AbstractDataSourceOperations.STATISTICS_ENABLED_WRITE_OPERATION;
            }

            if (attributeDefinition == CONNECTION_FACTORY_ATTRIBUTE) {
                writeHandler = AbstractDataSourceOperations.CONNECTION_FACTORY_WRITE_OPERATION;
            }

            if (attributeDefinition == CONNECTION_POOL_ATTRIBUTE) {
                writeHandler = AbstractDataSourceOperations.CONNECTION_POOL_WRITE_OPERATION;
            }

            resourceRegistration.registerReadWriteAttribute(attributeDefinition, null, writeHandler);
        }

        // Runtime attributes
        resourceRegistration.registerReadOnlyAttribute(STATISTICS, AbstractDataSourceOperations.STATISTICS_GET_OPERATION);
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(DATA_SOURCE_CAPABILITY);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(FLUSH_ALL, AbstractDataSourceOperations.FLUSH_ALL_OPERATION);
        resourceRegistration.registerOperationHandler(FLUSH_GRACEFUL, AbstractDataSourceOperations.FLUSH_GRACEFUL_OPERATION);
        resourceRegistration.registerOperationHandler(FLUSH_INVALID, AbstractDataSourceOperations.FLUSH_INVALID_OPERATION);
        resourceRegistration.registerOperationHandler(FLUSH_IDLE, AbstractDataSourceOperations.FLUSH_IDLE_OPERATION);
        resourceRegistration.registerOperationHandler(RESET_STATISTICS, AbstractDataSourceOperations.RESET_STATISTICS_OPERATION);
        resourceRegistration.registerOperationHandler(TEST_CONNECTION, AbstractDataSourceOperations.TEST_CONNECTION_OPERATION);
    }
}
