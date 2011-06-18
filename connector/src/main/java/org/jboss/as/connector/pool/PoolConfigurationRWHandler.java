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

package org.jboss.as.connector.pool;

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.connector.ConnectorServices;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.pool.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.pool.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.pool.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.pool.Constants.POOL_PREFILL;
import static org.jboss.as.connector.pool.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.pool.Constants.USE_FAST_FAIL;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.core.api.connectionmanager.pool.PoolConfiguration;
import org.jboss.jca.core.api.management.Connector;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public class PoolConfigurationRWHandler {

    static final String[] NO_LOCATION = new String[0];

    public static final String[] ATTRIBUTES = new String[]{MAX_POOL_SIZE, MIN_POOL_SIZE, BLOCKING_TIMEOUT_WAIT_MILLIS,
            IDLETIMEOUTMINUTES, BACKGROUNDVALIDATION, BACKGROUNDVALIDATIONMINUTES, POOL_PREFILL, POOL_USE_STRICT_MIN,
            USE_FAST_FAIL};

    public static class PoolConfigurationReadHandler implements NewStepHandler {
        public static PoolConfigurationReadHandler INSTANCE = new PoolConfigurationReadHandler();

        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
            final String parameterName = operation.require(NAME).asString();

            final ModelNode submodel = context.readModel(PathAddress.EMPTY_ADDRESS);
            final ModelNode currentValue = submodel.get(parameterName).clone();

            context.getResult().set(currentValue);

            context.completeStep();
        }
    }

    public abstract static class PoolConfigurationWriteHandler extends ServerWriteAttributeOperationHandler {

        protected PoolConfigurationWriteHandler(ParameterValidator validator) {
            super(validator);
        }

        @Override
        protected boolean applyUpdateToRuntime(final NewOperationContext context, final ModelNode operation,
               final String parameterName, final ModelNode newValue,
               final ModelNode currentValue) throws OperationFailedException {

            if (context.getType() == NewOperationContext.Type.SERVER) {
                context.addStep(new NewStepHandler() {
                    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                        final String jndiName = address.getLastElement().getValue();

                        final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                                ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE);
                        if (managementRepoService != null) {
                            try {
                                final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                                List<PoolConfiguration> poolConfigs = getMatchingPoolConfigs(jndiName, repository);
                                for (PoolConfiguration pc : poolConfigs) {
                                    if (MAX_POOL_SIZE.equals(parameterName)) {
                                        pc.setMaxSize(newValue.asInt());
                                    }
                                    if (MIN_POOL_SIZE.equals(parameterName)) {
                                        pc.setMinSize(newValue.asInt());
                                    }
                                    if (BLOCKING_TIMEOUT_WAIT_MILLIS.equals(parameterName)) {
                                        pc.setBlockingTimeout(newValue.asLong());
                                    }
                                    if (POOL_USE_STRICT_MIN.equals(parameterName)) {
                                        pc.setStrictMin(newValue.asBoolean());
                                    }
                                    if (USE_FAST_FAIL.equals(parameterName)) {
                                        pc.setUseFastFail(newValue.asBoolean());
                                    }

                                }
                            } catch (Exception e) {
                                throw new OperationFailedException(new ModelNode().set("failed to set attribute" + e.getMessage()));
                            }
                        }
                        context.completeStep();
                    }
                }, NewOperationContext.Stage.RUNTIME);
            }

            return (IDLETIMEOUTMINUTES.equals(parameterName) || BACKGROUNDVALIDATION.equals(parameterName)
                    || BACKGROUNDVALIDATIONMINUTES.equals(parameterName) || POOL_PREFILL.equals(parameterName));

        }

        protected abstract List<PoolConfiguration> getMatchingPoolConfigs(String jndiName, ManagementRepository repository);
    }

    public static class LocalAndXaDataSourcePoolConfigurationWriteHandler extends PoolConfigurationWriteHandler {
        public static LocalAndXaDataSourcePoolConfigurationWriteHandler INSTANCE = new LocalAndXaDataSourcePoolConfigurationWriteHandler(
                new PoolConfigurationValidator());

        protected LocalAndXaDataSourcePoolConfigurationWriteHandler(ParameterValidator validator) {
            super(validator);
        }

        protected List<PoolConfiguration> getMatchingPoolConfigs(String jndiName, ManagementRepository repository) {
            ArrayList<PoolConfiguration> result = new ArrayList<PoolConfiguration>(repository.getDataSources().size());
            if (repository.getDataSources() != null) {
                for (DataSource ds : repository.getDataSources()) {
                    if (jndiName.equalsIgnoreCase(ds.getJndiName())) {
                        result.add(ds.getPoolConfiguration());
                    }

                }
            }
            result.trimToSize();
            return result;
        }

    }

    public static class RaPoolConfigurationWriteHandler extends PoolConfigurationWriteHandler {
        public static RaPoolConfigurationWriteHandler INSTANCE = new RaPoolConfigurationWriteHandler(
                new PoolConfigurationValidator());

        protected RaPoolConfigurationWriteHandler(ParameterValidator validator) {
            super(validator);
        }

        protected List<PoolConfiguration> getMatchingPoolConfigs(String jndiName, ManagementRepository repository) {
            ArrayList<PoolConfiguration> result = new ArrayList<PoolConfiguration>(repository.getConnectors().size());
            if (repository.getConnectors() != null) {
                for (Connector conn : repository.getConnectors()) {
                    if (jndiName.equalsIgnoreCase(conn.getUniqueId())) {
                        if (conn.getConnectionFactories() == null || conn.getConnectionFactories().get(0) == null)
                            continue;
                        PoolConfiguration pc = conn.getConnectionFactories().get(0).getPoolConfiguration();
                        result.add(pc);
                    }

                }
            }
            result.trimToSize();
            return result;
        }

    }

    static class PoolConfigurationValidator implements ParameterValidator {

        static final ModelTypeValidator intValidator = new ModelTypeValidator(ModelType.INT);
        static final ModelTypeValidator longValidator = new ModelTypeValidator(ModelType.LONG);
        static final ModelTypeValidator boolValidator = new ModelTypeValidator(ModelType.BOOLEAN);

        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {

            if (MAX_POOL_SIZE.equals(parameterName)) {
                intValidator.validateParameter(parameterName, value);
            } else if (MIN_POOL_SIZE.equals(parameterName)) {
                intValidator.validateParameter(parameterName, value);
            } else if (BLOCKING_TIMEOUT_WAIT_MILLIS.equals(parameterName)) {
                longValidator.validateParameter(parameterName, value);
            } else if (IDLETIMEOUTMINUTES.equals(parameterName)) {
                longValidator.validateParameter(parameterName, value);
            } else if (BACKGROUNDVALIDATION.equals(parameterName)) {
                boolValidator.validateParameter(parameterName, value);
            } else if (BACKGROUNDVALIDATIONMINUTES.equals(parameterName)) {
                intValidator.validateParameter(parameterName, value);
            } else if (POOL_PREFILL.equals(parameterName)) {
                boolValidator.validateParameter(parameterName, value);
            } else if (POOL_USE_STRICT_MIN.equals(parameterName)) {
                boolValidator.validateParameter(parameterName, value);
            } else if (USE_FAST_FAIL.equals(parameterName)) {
                boolValidator.validateParameter(parameterName, value);
            } else {
                throw new OperationFailedException(new ModelNode().set("Wrong parameter name for " + parameterName));
            }

        }

        @Override
        public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
            validateParameter(parameterName, value.resolve());
        }

    }
}
