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

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.connector.ConnectorServices;
import static org.jboss.as.connector.subsystems.datasources.Constants.*;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers.ReadAttributeHandler;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.server.ServerOperationContext;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 */
class DataSourcePoolConfigurationRWHandler {

    static final String[] NO_LOCATION = new String[0];
    // private static final String MAX_POOL_SIZE = "max-pool-size";
    // private static final String MIN_POOL_SIZE = "min-pool-size";
    // private static final String BLOCKING_TIMEOUT =
    // "blocking-timeout-wait-millis";
    // private static final String IDLE_TIMEOUT_MINUTES =
    // "idle-timeout-minutes";
    // private static final String BACKGROUND_VALIDATION =
    // "background-validation";
    // private static final String BACKGROUND_VALIDATION_MINUTES =
    // "background-validation-minutes";
    // private static final String POOL_PREFILL = "pool-prefill";
    // private static final String POOL_USE_STRICT_MIN = "pool-use-strict-min";
    // private static final String USE_FAST_FAIL = "use-fast-fail";

    static final String[] ATTRIBUTES = new String[] { MAX_POOL_SIZE, MIN_POOL_SIZE, BLOCKING_TIMEOUT_WAIT_MILLIS,
            IDLETIMEOUTMINUTES, BACKGROUNDVALIDATION, BACKGROUNDVALIDATIONMINUTES, POOL_PREFILL, POOL_USE_STRICT_MIN,
            USE_FAST_FAIL };

    static class DataSourcePoolConfigurationReadHandler implements ModelQueryOperationHandler {
        static DataSourcePoolConfigurationReadHandler INSTANCE = new DataSourcePoolConfigurationReadHandler();

        /** {@inheritDoc} */
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation,
                final ResultHandler resultHandler) throws OperationFailedException {

            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String jndiName = address.getLastElement().getValue();
            final String parameterName = operation.require(NAME).asString();

            final ModelNode submodel = context.getSubModel();
            final ModelNode currentValue = submodel.get(parameterName).clone();

            resultHandler.handleResultFragment(new String[0], currentValue);
            resultHandler.handleResultComplete();

            // if (context.getRuntimeContext() != null) {
            // context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
            // public void execute(RuntimeTaskContext context) throws
            // OperationFailedException {
            //
            // final ServiceController<?> managementRepoService =
            // context.getServiceRegistry().getService(
            // ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE);
            // if (managementRepoService != null) {
            // try {
            // final ManagementRepository repository = (ManagementRepository)
            // managementRepoService.getValue();
            // final ModelNode result = new ModelNode();
            // if (repository.getDataSources() != null) {
            // for (DataSource ds : repository.getDataSources()) {
            // if (jndiName.equalsIgnoreCase(ds.getJndiName())) {
            // if (MAX_POOL_SIZE.equals(parameterName)) {
            // result.set("" + ds.getPoolConfiguration().getMaxSize());
            // }
            // if (MIN_POOL_SIZE.equals(parameterName)) {
            // result.set("" + ds.getPoolConfiguration().getMinSize());
            // }
            // if (BLOCKING_TIMEOUT_WAIT_MILLIS.equals(parameterName)) {
            // result.set("" + ds.getPoolConfiguration().getBlockingTimeout());
            // }
            // if (IDLETIMEOUTMINUTES.equals(parameterName)) {
            // result.set("" + ds.getPoolConfiguration().getIdleTimeout());
            // }
            // if (BACKGROUNDVALIDATION.equals(parameterName)) {
            // result.set("" +
            // ds.getPoolConfiguration().isBackgroundValidation());
            // }
            // if (BACKGROUNDVALIDATIONMINUTES.equals(parameterName)) {
            // result.set("" +
            // ds.getPoolConfiguration().getBackgroundValidationMinutes());
            // }
            // if (POOL_PREFILL.equals(parameterName)) {
            // result.set("" + ds.getPoolConfiguration().isPrefill());
            // }
            // if (POOL_USE_STRICT_MIN.equals(parameterName)) {
            // result.set("" + ds.getPoolConfiguration().isStrictMin());
            // }
            // if (USE_FAST_FAIL.equals(parameterName)) {
            // result.set("" + ds.getPoolConfiguration().isUseFastFail());
            // }
            // }
            // }
            // }
            // resultHandler.handleResultFragment(new String[0], result);
            // resultHandler.handleResultComplete();
            // } catch (Exception e) {
            // throw new OperationFailedException(new
            // ModelNode().set("failed to get attribute"
            // + e.getMessage()));
            // }
            // }
            // }
            // });
            // } else {
            // resultHandler.handleResultFragment(NO_LOCATION, new
            // ModelNode().set("no metrics available"));
            // resultHandler.handleResultComplete();
            // }
            return new BasicOperationResult();
        }
    }

    static class DataSourcePoolConfigurationWriteHandler extends ServerWriteAttributeOperationHandler {

        static DataSourcePoolConfigurationWriteHandler INSTANCE = new DataSourcePoolConfigurationWriteHandler(
                new PoolConfigurationValidator());

        protected DataSourcePoolConfigurationWriteHandler(ParameterValidator validator) {
            super(validator);
        }

        @Override
        protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                final ResultHandler resultHandler, final String parameterName, final ModelNode newValue,
                final ModelNode currentValue) throws OperationFailedException {
            if (context.getRuntimeContext() != null) {
                context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                    public void execute(RuntimeTaskContext runtimeCtx) throws OperationFailedException {
                        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                        final String jndiName = address.getLastElement().getValue();

                        final ServiceController<?> managementRepoService = runtimeCtx.getServiceRegistry().getService(
                                ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE);
                        if (managementRepoService != null) {
                            try {
                                final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                                if (repository.getDataSources() != null) {
                                    for (DataSource ds : repository.getDataSources()) {
                                        if (jndiName.equalsIgnoreCase(ds.getJndiName())) {
                                            if (MAX_POOL_SIZE.equals(parameterName)) {
                                                ds.getPoolConfiguration().setMaxSize(newValue.asInt());
                                            }
                                            if (MIN_POOL_SIZE.equals(parameterName)) {
                                                ds.getPoolConfiguration().setMinSize(newValue.asInt());
                                            }
                                            if (BLOCKING_TIMEOUT_WAIT_MILLIS.equals(parameterName)) {
                                                ds.getPoolConfiguration().setBlockingTimeout(newValue.asLong());
                                            }
                                            if (POOL_USE_STRICT_MIN.equals(parameterName)) {
                                                ds.getPoolConfiguration().setStrictMin(newValue.asBoolean());
                                            }
                                            if (USE_FAST_FAIL.equals(parameterName)) {
                                                ds.getPoolConfiguration().setUseFastFail(newValue.asBoolean());
                                            }
                                        }
                                    }
                                }

                                resultHandler.handleResultComplete();

                            } catch (Exception e) {
                                throw new OperationFailedException(new ModelNode().set("failed to set attribute"
                                        + e.getMessage()));
                            }
                        } else {
                            resultHandler.handleResultComplete();
                        }
                    }
                });
            } else {
                resultHandler.handleResultComplete();
            }
            return (IDLETIMEOUTMINUTES.equals(parameterName) || BACKGROUNDVALIDATION.equals(parameterName)
                    || BACKGROUNDVALIDATIONMINUTES.equals(parameterName) || POOL_PREFILL.equals(parameterName));

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
