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

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceMetrics;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.extension.datasources.agroal.logging.AgroalLogger;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.credential.source.CredentialSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import static io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ConnectionValidator.defaultValidator;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;

/**
 * Operations common to XA and non-XA DataSources
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class AbstractDataSourceOperations {

    static final OperationStepHandler STATISTICS_ENABLED_WRITE_OPERATION = new StatisticsEnabledAttributeWriter(AbstractDataSourceDefinition.STATISTICS_ENABLED_ATTRIBUTE);

    static final OperationStepHandler CONNECTION_FACTORY_WRITE_OPERATION = new ConnectionFactoryAttributeWriter(AbstractDataSourceDefinition.CONNECTION_FACTORY_ATTRIBUTE.getValueTypes());

    static final OperationStepHandler CONNECTION_POOL_WRITE_OPERATION = new ConnectionPoolAttributeWriter(AbstractDataSourceDefinition.CONNECTION_POOL_ATTRIBUTE.getValueTypes());

    // --- //

    static final OperationStepHandler FLUSH_ALL_OPERATION = new FlushOperation(AgroalDataSource.FlushMode.ALL);

    static final OperationStepHandler FLUSH_GRACEFUL_OPERATION = new FlushOperation(AgroalDataSource.FlushMode.GRACEFUL);

    static final OperationStepHandler FLUSH_INVALID_OPERATION = new FlushOperation(AgroalDataSource.FlushMode.INVALID);

    static final OperationStepHandler FLUSH_IDLE_OPERATION = new FlushOperation(AgroalDataSource.FlushMode.IDLE);

    static final OperationStepHandler RESET_STATISTICS_OPERATION = new ResetStatisticsOperation();

    static final OperationStepHandler STATISTICS_GET_OPERATION = new StatisticsGetOperation();

    static final OperationStepHandler TEST_CONNECTION_OPERATION = new TestConnectionOperation();

    // --- //

    protected static AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        AgroalConnectionFactoryConfigurationSupplier configuration = new AgroalConnectionFactoryConfigurationSupplier();

        if (AbstractDataSourceDefinition.URL_ATTRIBUTE.resolveModelAttribute(context, model).isDefined()) {
            configuration.jdbcUrl(AbstractDataSourceDefinition.URL_ATTRIBUTE.resolveModelAttribute(context, model).asString());
        }

        if (AbstractDataSourceDefinition.NEW_CONNECTION_SQL_ATTRIBUTE.resolveModelAttribute(context, model).isDefined()) {
            configuration.initialSql(AbstractDataSourceDefinition.NEW_CONNECTION_SQL_ATTRIBUTE.resolveModelAttribute(context, model).asString());
        }

        if (AbstractDataSourceDefinition.TRANSACTION_ISOLATION_ATTRIBUTE.resolveModelAttribute(context, model).isDefined()) {
            configuration.jdbcTransactionIsolation(TransactionIsolation.valueOf(AbstractDataSourceDefinition.TRANSACTION_ISOLATION_ATTRIBUTE.resolveModelAttribute(context, model).asString()));
        }

        if (AbstractDataSourceDefinition.CONNECTION_PROPERTIES_ATTRIBUTE.resolveModelAttribute(context, model).isDefined()) {
            for (Property jdbcProperty : AbstractDataSourceDefinition.CONNECTION_PROPERTIES_ATTRIBUTE.resolveModelAttribute(context, model).asPropertyList()) {
                configuration.jdbcProperty(jdbcProperty.getName(), jdbcProperty.getValue().asString());
            }
        }

        if (AbstractDataSourceDefinition.USERNAME_ATTRIBUTE.resolveModelAttribute(context, model).isDefined()) {
            configuration.principal(new NamePrincipal(AbstractDataSourceDefinition.USERNAME_ATTRIBUTE.resolveModelAttribute(context, model).asString()));
        }
        if (AbstractDataSourceDefinition.PASSWORD_ATTRIBUTE.resolveModelAttribute(context, model).isDefined()) {
            configuration.credential(new SimplePassword(AbstractDataSourceDefinition.PASSWORD_ATTRIBUTE.resolveModelAttribute(context, model).asString()));
        }
        return configuration;
    }

    protected static void setupElytronSecurity(OperationContext context, ModelNode model, DataSourceService dataSourceService, ServiceBuilder<?> serviceBuilder) throws OperationFailedException {
        if (AbstractDataSourceDefinition.AUTHENTICATION_CONTEXT.resolveModelAttribute(context, model).isDefined()) {
            String authenticationContextName = AbstractDataSourceDefinition.AUTHENTICATION_CONTEXT.resolveModelAttribute(context, model).asString();
            ServiceName authenticationContextCapability = context.getCapabilityServiceName(AbstractDataSourceDefinition.AUTHENTICATION_CONTEXT_CAPABILITY, authenticationContextName, AuthenticationContext.class);
            serviceBuilder.addDependency(authenticationContextCapability, AuthenticationContext.class, dataSourceService.getAuthenticationContextInjector());
        }
        if (AbstractDataSourceDefinition.CREDENTIAL_REFERENCE.resolveModelAttribute(context, model).isDefined()) {
            ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier = CredentialReference.getCredentialSourceSupplier(context, AbstractDataSourceDefinition.CREDENTIAL_REFERENCE, model, serviceBuilder);
            dataSourceService.getCredentialSourceSupplierInjector().inject(credentialSourceSupplier);
        }
    }

    protected static AgroalConnectionPoolConfigurationSupplier connectionPoolConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        AgroalConnectionPoolConfigurationSupplier configuration = new AgroalConnectionPoolConfigurationSupplier();

        configuration.maxSize(AbstractDataSourceDefinition.MAX_SIZE_ATTRIBUTE.resolveModelAttribute(context, model).asInt());
        configuration.minSize(AbstractDataSourceDefinition.MIN_SIZE_ATTRIBUTE.resolveModelAttribute(context, model).asInt());
        configuration.initialSize(AbstractDataSourceDefinition.INITIAL_SIZE_ATTRIBUTE.resolveModelAttribute(context, model).asInt());

        configuration.acquisitionTimeout(ofMillis(AbstractDataSourceDefinition.BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE.resolveModelAttribute(context, model).asInt()));
        configuration.leakTimeout(ofMillis(AbstractDataSourceDefinition.LEAK_DETECTION_ATTRIBUTE.resolveModelAttribute(context, model).asInt()));
        configuration.validationTimeout(ofMillis(AbstractDataSourceDefinition.BACKGROUND_VALIDATION_ATTRIBUTE.resolveModelAttribute(context, model).asInt()));
        configuration.reapTimeout(ofMinutes(AbstractDataSourceDefinition.IDLE_REMOVAL_ATTRIBUTE.resolveModelAttribute(context, model).asInt()));
        configuration.connectionValidator(defaultValidator());

        return configuration;
    }

    // --- //

    private static AgroalDataSource getDataSource(OperationContext context) throws OperationFailedException {
        ServiceRegistry registry = context.getServiceRegistry(false);
        String dataSourceName = context.getCurrentAddressValue();

        switch (context.getCurrentAddress().getLastElement().getKey()) {
            case DataSourceOperations.DATASOURCE_SERVICE_NAME:
                ServiceController<?> controller = registry.getRequiredService(AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.getCapabilityServiceName(dataSourceName));
                return ((AgroalDataSource) controller.getValue());
            case XADataSourceOperations.XADATASOURCE_SERVICE_NAME:
                ServiceController<?> xaController = registry.getRequiredService(AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.getCapabilityServiceName(dataSourceName));
                return ((AgroalDataSource) xaController.getValue());
            default:
                throw AgroalLogger.SERVICE_LOGGER.unknownDatasourceServiceType(context.getCurrentAddress().getLastElement().getKey());
        }
    }

    // --- //

    private static class StatisticsEnabledAttributeWriter extends AbstractWriteAttributeHandler<Boolean> {

        private StatisticsEnabledAttributeWriter(AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Boolean> handbackHolder) throws OperationFailedException {
            getDataSource(context).getConfiguration().setMetricsEnabled(resolvedValue.asBoolean());
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Boolean handback) throws OperationFailedException {
            getDataSource(context).getConfiguration().setMetricsEnabled(valueToRevert.asBoolean());
        }
    }


    private static class ConnectionFactoryAttributeWriter extends AbstractWriteAttributeHandler<AgroalConnectionFactoryConfiguration> {

        private ConnectionFactoryAttributeWriter(AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<AgroalConnectionFactoryConfiguration> handbackHolder) throws OperationFailedException {
            // At the moment no attributes support hot. Required
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, AgroalConnectionFactoryConfiguration handback) throws OperationFailedException {
        }
    }

    private static class ConnectionPoolAttributeWriter extends AbstractWriteAttributeHandler<AgroalConnectionPoolConfiguration> {

        private ConnectionPoolAttributeWriter(AttributeDefinition... definitions) {
            super(definitions);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<AgroalConnectionPoolConfiguration> handbackHolder) throws OperationFailedException {
            ModelNode newBlockingTimeout = resolvedValue.remove(AbstractDataSourceDefinition.BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE.getName());
            ModelNode newMaxSize = resolvedValue.remove(AbstractDataSourceDefinition.MAX_SIZE_ATTRIBUTE.getName());
            ModelNode newMinSize = resolvedValue.remove(AbstractDataSourceDefinition.MIN_SIZE_ATTRIBUTE.getName());

            for (String attribute : resolvedValue.keys()) {
                if (!currentValue.hasDefined(attribute) || !resolvedValue.get(attribute).equals(currentValue.get(attribute))) {
                    // Other attributes changed. Restart required
                    return true;
                }
            }

            if (newBlockingTimeout != null) {
                getDataSource(context).getConfiguration().connectionPoolConfiguration().setAcquisitionTimeout(Duration.ofMillis(newBlockingTimeout.asInt()));
            }
            if (newMaxSize != null) {
                getDataSource(context).getConfiguration().connectionPoolConfiguration().setMaxSize(newMaxSize.asInt());
                // if max-size decreases Agroal will gracefully destroy connections when they are returned to the pool, so there is nothing to do here
            }
            if (newMinSize != null) {
                getDataSource(context).getConfiguration().connectionPoolConfiguration().setMinSize(newMinSize.asInt());
                // if min-size increases Agroal will create new connections when looking into the (shared) pool. FlushMode.FILL could be used here to enforce the new min-size
            }
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, AgroalConnectionPoolConfiguration handback) throws OperationFailedException {
            ModelNode newBlockingTimeout = valueToRevert.remove(AbstractDataSourceDefinition.BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE.getName());
            ModelNode newMaxSize = valueToRevert.remove(AbstractDataSourceDefinition.MAX_SIZE_ATTRIBUTE.getName());
            ModelNode newMinSize = valueToRevert.remove(AbstractDataSourceDefinition.MIN_SIZE_ATTRIBUTE.getName());

            if (newBlockingTimeout != null) {
                getDataSource(context).getConfiguration().connectionPoolConfiguration().setAcquisitionTimeout(Duration.ofMillis(newBlockingTimeout.asInt()));
            }
            if (newMinSize != null) {
                getDataSource(context).getConfiguration().connectionPoolConfiguration().setMinSize(newMinSize.asInt());
            }
            if (newMaxSize != null) {
                getDataSource(context).getConfiguration().connectionPoolConfiguration().setMaxSize(newMaxSize.asInt());
            }
        }
    }

    // --- //

    private static class FlushOperation implements OperationStepHandler {

        private AgroalDataSource.FlushMode mode;

        private FlushOperation(AgroalDataSource.FlushMode mode) {
            this.mode = mode;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (context.isNormalServer()) {
                AgroalLogger.SERVICE_LOGGER.flushOperation(mode);
                getDataSource(context).flush(mode);
            }
        }
    }

    // --- //

    private static class StatisticsGetOperation implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (context.isNormalServer()) {
                AgroalDataSourceMetrics metrics = getDataSource(context).getMetrics();

                ModelNode result = new ModelNode();

                result.get(AbstractDataSourceDefinition.STATISTICS_ACQUIRE_COUNT_ATTRIBUTE.getName()).set(metrics.acquireCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_ACTIVE_COUNT_ATTRIBUTE.getName()).set(metrics.activeCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_AVAILABLE_COUNT_ATTRIBUTE.getName()).set(metrics.availableCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_AWAITING_COUNT_ATTRIBUTE.getName()).set(metrics.awaitingCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_CREATION_COUNT_ATTRIBUTE.getName()).set(metrics.creationCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_DESTOY_COUNT_ATTRIBUTE.getName()).set(metrics.destroyCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_FLUSH_COUNT_ATTRIBUTE.getName()).set(metrics.flushCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_INVALID_COUNT_ATTRIBUTE.getName()).set(metrics.invalidCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_LEAK_DETECTION_COUNT_ATTRIBUTE.getName()).set(metrics.leakDetectionCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_MAX_USED_COUNT_ATTRIBUTE.getName()).set(metrics.maxUsedCount());
                result.get(AbstractDataSourceDefinition.STATISTICS_REAP_COUNT_ATTRIBUTE.getName()).set(metrics.reapCount());

                result.get(AbstractDataSourceDefinition.STATISTICS_BLOCKING_TIME_AVERAGE_ATTRIBUTE.getName()).set(metrics.blockingTimeAverage().toMillis());
                result.get(AbstractDataSourceDefinition.STATISTICS_BLOCKING_TIME_MAX_ATTRIBUTE.getName()).set(metrics.blockingTimeMax().toMillis());
                result.get(AbstractDataSourceDefinition.STATISTICS_BLOCKING_TIME_TOTAL_ATTRIBUTE.getName()).set(metrics.blockingTimeTotal().toMillis());

                result.get(AbstractDataSourceDefinition.STATISTICS_CREATION_TIME_AVERAGE_ATTRIBUTE.getName()).set(metrics.creationTimeAverage().toMillis());
                result.get(AbstractDataSourceDefinition.STATISTICS_CREATION_TIME_MAX_ATTRIBUTE.getName()).set(metrics.creationTimeMax().toMillis());
                result.get(AbstractDataSourceDefinition.STATISTICS_CREATION_TIME_TOTAL_ATTRIBUTE.getName()).set(metrics.creationTimeTotal().toMillis());

                context.getResult().set(result);
            }
        }
    }

    private static class ResetStatisticsOperation implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (context.isNormalServer()) {
                getDataSource(context).getMetrics().reset();
            }
        }
    }

    // --- //


    private static class TestConnectionOperation implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (context.isNormalServer()) {
                try (Connection connection = getDataSource(context).getConnection()) {
                    context.getResult().set(new ModelNode().add(connection.isValid(0)));
                } catch (SQLException e) {
                    throw AgroalLogger.SERVICE_LOGGER.invalidConnection(e, context.getCurrentAddressValue());
                }
            }
        }
    }
}

