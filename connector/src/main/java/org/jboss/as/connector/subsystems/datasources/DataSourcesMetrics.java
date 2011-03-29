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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
class DataSourcesMetrics implements ModelQueryOperationHandler {

    static DataSourcesMetrics INSTANCE = new DataSourcesMetrics();

    static final String[] NO_LOCATION = new String[0];
    private static final String MAX_POOL_SIZE = "max-pool-size";
    private static final String MIN_POOL_SIZE = "min-pool-size";
    private static final String BLOCKING_TIMEOUT = "blocking-timeout-wait-millis";
    private static final String IDLE_TIMEOUT_MINUTES = "idle-timeout-minutes";
    private static final String BACKGROUND_VALIDATION = "background-validation";
    private static final String BACKGROUND_VALIDATION_MINUTES = "background-validation-minutes";
    private static final String POOL_PREFILL = "pool-prefill";
    private static final String POOL_USE_STRICT_MIN = "pool-use-strict-min";
    private static final String USE_FAST_FAIL = "use-fast-fail";

    static final String[] ATTRIBUTES = new String[] { MAX_POOL_SIZE, MIN_POOL_SIZE, BLOCKING_TIMEOUT, IDLE_TIMEOUT_MINUTES,
        BACKGROUND_VALIDATION, BACKGROUND_VALIDATION_MINUTES, POOL_PREFILL, POOL_USE_STRICT_MIN, USE_FAST_FAIL};

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                    final String jndiName = address.getLastElement().getValue();
                    final String attributeName = operation.require(NAME).asString();

                    final ServiceController<?> managementRepoService = context.getServiceRegistry().getService(
                            ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE);
                    if (managementRepoService != null) {
                        try {
                            final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                            final ModelNode result = new ModelNode();
                            if (repository.getDataSources() != null) {
                                for (DataSource ds : repository.getDataSources()) {
                                    if (jndiName.equalsIgnoreCase(ds.getJndiName())) {
                                        if (MAX_POOL_SIZE.equals(attributeName)) {
                                            result.set("" + ds.getPoolConfiguration().getMaxSize());
                                        }
                                        if (MIN_POOL_SIZE.equals(attributeName)) {
                                            result.set("" + ds.getPoolConfiguration().getMinSize());
                                        }
                                        if (BLOCKING_TIMEOUT.equals(attributeName)) {
                                            result.set("" + ds.getPoolConfiguration().getBlockingTimeout());
                                        }
                                        if (IDLE_TIMEOUT_MINUTES.equals(attributeName)) {
                                            result.set("" + ds.getPoolConfiguration().getIdleTimeout());
                                        }
                                        if (BACKGROUND_VALIDATION.equals(attributeName)) {
                                            result.set("" + ds.getPoolConfiguration().isBackgroundValidation());
                                        }
                                        if (BACKGROUND_VALIDATION_MINUTES.equals(attributeName)) {
                                            result.set("" + ds.getPoolConfiguration().getBackgroundValidationMinutes());
                                        }
                                        if (POOL_PREFILL.equals(attributeName)) {
                                            result.set("" + ds.getPoolConfiguration().isPrefill());
                                        }
                                        if (POOL_USE_STRICT_MIN.equals(attributeName)) {
                                            result.set("" + ds.getPoolConfiguration().isStrictMin());
                                        }
                                        if (USE_FAST_FAIL.equals(attributeName)) {
                                            result.set("" + ds.getPoolConfiguration().isUseFastFail());
                                        }
                                    }
                                }
                            }
                            resultHandler.handleResultFragment(new String[0], result);
                            resultHandler.handleResultComplete();
                        } catch (Exception e) {
                            throw new OperationFailedException(new ModelNode().set("failed to get metrics" + e.getMessage()));
                        }
                    }
                }
            });
        } else {
            resultHandler.handleResultFragment(NO_LOCATION, new ModelNode().set("no metrics available"));
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult();
    }
}
