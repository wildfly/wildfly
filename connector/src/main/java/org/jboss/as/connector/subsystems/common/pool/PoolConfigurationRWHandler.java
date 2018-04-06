/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.common.pool;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.api.management.ConnectionFactory;
import org.jboss.jca.core.api.management.Connector;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.ServiceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_ATTRIBUTES;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.common.pool.Constants.VALIDATE_ON_MATCH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public class PoolConfigurationRWHandler {

    public static final List<String> ATTRIBUTES = Arrays.asList(MAX_POOL_SIZE.getName(), MIN_POOL_SIZE.getName(), INITIAL_POOL_SIZE.getName(),BLOCKING_TIMEOUT_WAIT_MILLIS.getName(),
            IDLETIMEOUTMINUTES.getName(), BACKGROUNDVALIDATION.getName(), BACKGROUNDVALIDATIONMILLIS.getName(),
            POOL_PREFILL.getName(), POOL_FAIR.getName(), POOL_USE_STRICT_MIN.getName(), POOL_FLUSH_STRATEGY.getName());

    // TODO this seems to just do what the default handler does, so registering it is probably unnecessary
    public static class PoolConfigurationReadHandler implements OperationStepHandler {
        public static final PoolConfigurationReadHandler INSTANCE = new PoolConfigurationReadHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String parameterName = operation.require(NAME).asString();

            final ModelNode submodel = context.readResource(PathAddress.EMPTY_ADDRESS, false).getModel();
            final ModelNode currentValue = submodel.hasDefined(parameterName) ? submodel.get(parameterName).clone() : new ModelNode();

            context.getResult().set(currentValue);
        }
    }

    public abstract static class PoolConfigurationWriteHandler extends AbstractWriteAttributeHandler<List<PoolConfiguration>> {


        protected PoolConfigurationWriteHandler() {
            super(POOL_ATTRIBUTES);
        }

        @Override
        protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                               final String parameterName, final ModelNode newValue,
                                               final ModelNode currentValue, final HandbackHolder<List<PoolConfiguration>> handbackHolder) throws OperationFailedException {

            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String poolName = address.getLastElement().getValue();

            final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                    ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE);
            List<PoolConfiguration> poolConfigs = null;
            if (managementRepoService != null) {
                try {
                    final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                    poolConfigs = getMatchingPoolConfigs(poolName, repository);
                    updatePoolConfigs(poolConfigs, parameterName, newValue);
                    handbackHolder.setHandback(poolConfigs);
                } catch (Exception e) {
                    throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToSetAttribute(e.getLocalizedMessage()));
                }
            }

            return (IDLETIMEOUTMINUTES.getName().equals(parameterName) || BACKGROUNDVALIDATION.getName().equals(parameterName)
                    || BACKGROUNDVALIDATIONMILLIS.getName().equals(parameterName)
                    || POOL_PREFILL.getName().equals(parameterName) || POOL_FLUSH_STRATEGY.getName().equals(parameterName)
                    || MAX_POOL_SIZE.getName().equals(parameterName) || MIN_POOL_SIZE.getName().equals(parameterName));

        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String parameterName,
                                             ModelNode valueToRestore, ModelNode valueToRevert,
                                             List<PoolConfiguration> handback) throws OperationFailedException {
            if (handback != null) {
                updatePoolConfigs(handback, parameterName, valueToRestore.resolve());
            }
        }


        private void updatePoolConfigs(List<PoolConfiguration> poolConfigs, String parameterName, ModelNode newValue) {
            for (PoolConfiguration pc : poolConfigs) {
                if (MAX_POOL_SIZE.getName().equals(parameterName)) {
                    pc.setMaxSize(newValue.asInt());
                }
                if (MIN_POOL_SIZE.getName().equals(parameterName)) {
                    pc.setMinSize(newValue.asInt());
                }
                if (INITIAL_POOL_SIZE.getName().equals(parameterName)) {
                    pc.setInitialSize(newValue.isDefined()? newValue.asInt(): 0);
                }
                if (BLOCKING_TIMEOUT_WAIT_MILLIS.getName().equals(parameterName)) {
                    pc.setBlockingTimeout(newValue.isDefined()? newValue.asLong(): 0);
                }
                if (POOL_USE_STRICT_MIN.getName().equals(parameterName)) {
                    pc.setStrictMin(newValue.asBoolean());
                }
                if (USE_FAST_FAIL.getName().equals(parameterName)) {
                    pc.setUseFastFail(newValue.asBoolean());
                }
                if (VALIDATE_ON_MATCH.getName().equals(parameterName)) {
                    pc.setValidateOnMatch(newValue.asBoolean());
                }
                if (POOL_FAIR.getName().equals(parameterName)) {
                    pc.setFair(newValue.asBoolean());
                }
            }
        }

        protected abstract List<PoolConfiguration> getMatchingPoolConfigs(String jndiName, ManagementRepository repository);
    }

    public static class LocalAndXaDataSourcePoolConfigurationWriteHandler extends PoolConfigurationWriteHandler {
        public static final LocalAndXaDataSourcePoolConfigurationWriteHandler INSTANCE = new LocalAndXaDataSourcePoolConfigurationWriteHandler();

        protected LocalAndXaDataSourcePoolConfigurationWriteHandler() {
            super();
        }

        protected List<PoolConfiguration> getMatchingPoolConfigs(String poolName, ManagementRepository repository) {
            ArrayList<PoolConfiguration> result = new ArrayList<PoolConfiguration>(repository.getDataSources().size());
            if (repository.getDataSources() != null) {
                for (DataSource ds : repository.getDataSources()) {
                    if (poolName.equalsIgnoreCase(ds.getPool().getName())) {
                        result.add(ds.getPoolConfiguration());
                    }
                }
            }
            result.trimToSize();
            return result;
        }


    }

    public static class RaPoolConfigurationWriteHandler extends PoolConfigurationWriteHandler {
        public static final RaPoolConfigurationWriteHandler INSTANCE = new RaPoolConfigurationWriteHandler();

        protected RaPoolConfigurationWriteHandler() {
            super();
        }

        protected List<PoolConfiguration> getMatchingPoolConfigs(String poolName, ManagementRepository repository) {
            ArrayList<PoolConfiguration> result = new ArrayList<PoolConfiguration>(repository.getConnectors().size());
            if (repository.getConnectors() != null) {
                for (Connector conn : repository.getConnectors()) {
                    if (conn.getConnectionFactories() == null || conn.getConnectionFactories().get(0) == null
                            || conn.getConnectionFactories().get(0).getPool() == null)
                        continue;

                    ConnectionFactory connectionFactory = conn.getConnectionFactories().get(0);
                    if (poolName.equals(connectionFactory.getPool().getName())) {
                        PoolConfiguration pc = conn.getConnectionFactories().get(0).getPoolConfiguration();
                        result.add(pc);
                    }
                }
            }
            result.trimToSize();
            return result;
        }

    }
}
