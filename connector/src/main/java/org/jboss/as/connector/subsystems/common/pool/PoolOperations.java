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

package org.jboss.as.connector.subsystems.common.pool;

import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCES;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.subsystems.datasources.Util;
import static org.jboss.as.connector.subsystems.datasources.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.PASSWORD;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.ModelNodeUtil;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.adapters.jdbc.WrappedConnectionRequestInfo;
import org.jboss.jca.core.api.connectionmanager.pool.FlushMode;
import org.jboss.jca.core.api.connectionmanager.pool.Pool;
import org.jboss.jca.core.api.management.ConnectionFactory;
import org.jboss.jca.core.api.management.Connector;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.ServiceController;

public abstract class PoolOperations implements OperationStepHandler {


    private final PoolMatcher matcher;
    private final boolean disallowMonitor;

    protected PoolOperations(PoolMatcher matcher, boolean disallowMonitor) {
        super();
        this.matcher = matcher;
        this.disallowMonitor = disallowMonitor;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String jndiName;
        ModelNode model;
        if (!address.getElement(0).getKey().equals(ModelDescriptionConstants.DEPLOYMENT) &&
                (model = context.readResource(PathAddress.EMPTY_ADDRESS, false).getModel()).isDefined()) {
            jndiName = Util.getJndiName(context, model);

            if (isDisabledDatasource(context, address, model)) {
                throw ConnectorLogger.ROOT_LOGGER.datasourceIsDisabled(jndiName);
            }
        } else {
            jndiName = address.getLastElement().getValue();
        }
        final Object[] parameters = getParameters(context, operation);
        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> managementRepoService = context.getServiceRegistry(disallowMonitor).getService(
                            ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE);
                    if (managementRepoService != null) {
                        ModelNode operationResult = null;
                        try {
                            final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                            final List<Pool> pools = matcher.match(jndiName, repository);

                            if (pools.isEmpty()) {
                                throw ConnectorLogger.ROOT_LOGGER.failedToMatchPool(jndiName);
                            }

                            for (Pool pool : pools) {
                                operationResult = invokeCommandOn(pool, parameters);
                            }

                        } catch (Exception e) {
                            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToInvokeOperation(e.getLocalizedMessage()));
                        }
                        if (operationResult != null) {
                            context.getResult().set(operationResult);
                        }
                    }
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

    protected abstract ModelNode invokeCommandOn(Pool pool, Object... parameters) throws Exception;
    protected abstract Object[] getParameters(OperationContext context, ModelNode operation);

    public static class FlushIdleConnectionInPool extends PoolOperations {
        public static final FlushIdleConnectionInPool DS_INSTANCE = new FlushIdleConnectionInPool(new DsPoolMatcher());
        public static final FlushIdleConnectionInPool RA_INSTANCE = new FlushIdleConnectionInPool(new RaPoolMatcher());

        protected FlushIdleConnectionInPool(PoolMatcher matcher) {
            super(matcher, true);
        }

        @Override
        protected ModelNode invokeCommandOn(Pool pool, Object... parameters) {
            pool.flush(FlushMode.IDLE);
            return null;
        }

        @Override
        protected Object[] getParameters(OperationContext context, ModelNode operation) {
            return null;
        }

    }

    public static class DumpQueuedThreadInPool extends PoolOperations {
            public static final DumpQueuedThreadInPool DS_INSTANCE = new DumpQueuedThreadInPool(new DsPoolMatcher());
            public static final DumpQueuedThreadInPool RA_INSTANCE = new DumpQueuedThreadInPool(new RaPoolMatcher());

            protected DumpQueuedThreadInPool(PoolMatcher matcher) {
                super(matcher, false);
            }

            @Override
            protected ModelNode invokeCommandOn(Pool pool, Object... parameters) {
                ModelNode result = new ModelNode();
                for (String line : pool.dumpQueuedThreads()) {
                    result.add(line);
                }
                return result;
            }

            @Override
            protected Object[] getParameters(OperationContext context, ModelNode operation) {
                return null;
            }

        }


    public static class FlushAllConnectionInPool extends PoolOperations {
        public static final FlushAllConnectionInPool DS_INSTANCE = new FlushAllConnectionInPool(new DsPoolMatcher());
        public static final FlushAllConnectionInPool RA_INSTANCE = new FlushAllConnectionInPool(new RaPoolMatcher());

        protected FlushAllConnectionInPool(PoolMatcher matcher) {
            super(matcher, true);
        }

        @Override
        protected ModelNode invokeCommandOn(Pool pool, Object... parameters) {
            pool.flush(FlushMode.ALL);
            return null;
        }

        @Override
        protected Object[] getParameters(OperationContext context, ModelNode operation) {
            return null;
        }

    }

    public static class FlushInvalidConnectionInPool extends PoolOperations {
        public static final FlushInvalidConnectionInPool DS_INSTANCE = new FlushInvalidConnectionInPool(new DsPoolMatcher());
        public static final FlushInvalidConnectionInPool RA_INSTANCE = new FlushInvalidConnectionInPool(new RaPoolMatcher());

        protected FlushInvalidConnectionInPool(PoolMatcher matcher) {
            super(matcher, true);
        }

        @Override
        protected ModelNode invokeCommandOn(Pool pool, Object... parameters) {
            pool.flush(FlushMode.INVALID);
            return null;
        }

        @Override
        protected Object[] getParameters(OperationContext context, ModelNode operation) {
            return null;
        }

    }

    public static class FlushGracefullyConnectionInPool extends PoolOperations {
        public static final FlushGracefullyConnectionInPool DS_INSTANCE = new FlushGracefullyConnectionInPool(new DsPoolMatcher());
        public static final FlushGracefullyConnectionInPool RA_INSTANCE = new FlushGracefullyConnectionInPool(new RaPoolMatcher());

        protected FlushGracefullyConnectionInPool(PoolMatcher matcher) {
            super(matcher, true);
        }

        @Override
        protected ModelNode invokeCommandOn(Pool pool, Object... parameters) {
            pool.flush(FlushMode.GRACEFULLY);
            return null;
        }

        @Override
        protected Object[] getParameters(OperationContext context, ModelNode operation) {
            return null;
        }

    }

    public static class TestConnectionInPool extends PoolOperations {
        public static final TestConnectionInPool DS_INSTANCE = new TestConnectionInPool(new DsPoolMatcher());
        public static final TestConnectionInPool RA_INSTANCE = new TestConnectionInPool(new RaPoolMatcher());

        protected TestConnectionInPool(PoolMatcher matcher) {
            super(matcher, true);
        }

        @Override
        protected ModelNode invokeCommandOn(Pool pool, Object... parameters) throws Exception {
            boolean returnedValue;
            if (parameters != null) {
                WrappedConnectionRequestInfo cri = new WrappedConnectionRequestInfo((String) parameters[0], (String) parameters[1]);
                returnedValue = pool.testConnection(cri, null);
            } else {
                returnedValue = pool.testConnection();
            }
            if (!returnedValue)
                throw ConnectorLogger.ROOT_LOGGER.invalidConnection();
            ModelNode result = new ModelNode();
            result.add(returnedValue);
            return result;
        }

        @Override
        protected Object[] getParameters(OperationContext context, ModelNode operation) {
            Object[] parameters = null;
            try {
                if (operation.hasDefined(USERNAME.getName()) || operation.hasDefined(PASSWORD.getName())) {
                    parameters = new Object[2];
                    parameters[0] = USERNAME.resolveModelAttribute(context, operation).asString();
                    parameters[1] = PASSWORD.resolveModelAttribute(context, operation).asString();
                }
            } catch (OperationFailedException ofe) {
                //just return null
            }
            return parameters;
        }

    }



    private interface PoolMatcher {
        List<Pool> match(String jndiName, ManagementRepository repository);
    }

    private static class DsPoolMatcher implements PoolMatcher {
        public List<Pool> match(String jndiName, ManagementRepository repository) {
            ArrayList<org.jboss.jca.core.api.connectionmanager.pool.Pool> result = new ArrayList<Pool>(repository
                    .getDataSources().size());
            if (repository.getDataSources() != null) {
                for (DataSource ds : repository.getDataSources()) {
                    if (jndiName.equalsIgnoreCase(ds.getJndiName()) && ds.getPool() != null) {
                        result.add(ds.getPool());
                    }

                }
            }
            result.trimToSize();
            return result;
        }
    }

    private static class RaPoolMatcher implements PoolMatcher {
        public List<Pool> match(String jndiName, ManagementRepository repository) {
            ArrayList<Pool> result = new ArrayList<Pool>(repository.getConnectors().size());
            if (repository.getConnectors() != null) {
                for (Connector c : repository.getConnectors()) {
                    if (c.getConnectionFactories() == null || c.getConnectionFactories().size() == 0)
                        continue;
                    for (ConnectionFactory cf : c.getConnectionFactories()) {
                        if (cf != null && cf.getPool() != null &&
                                jndiName.equalsIgnoreCase(cf.getJndiName())) {
                            result.add(cf.getPool());
                        }

                    }
                }
            }
            return result;
        }
    }

    private boolean isDisabledDatasource(OperationContext context, PathAddress address, ModelNode datasourceNode) throws OperationFailedException {
        return address.getElement(0).getValue().equals(DATASOURCES) &&
                !ModelNodeUtil.getBooleanIfSetOrGetDefault(context, datasourceNode, ENABLED);
    }
}
