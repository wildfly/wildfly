package org.jboss.as.connector.pool;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.subsystems.datasources.Util;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.connectionmanager.pool.Pool;
import org.jboss.jca.core.api.management.ConnectionFactory;
import org.jboss.jca.core.api.management.Connector;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.ServiceController;

import static org.jboss.as.connector.ConnectorMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

public abstract class PoolOperations implements OperationStepHandler {


    private final PoolMatcher matcher;

    protected PoolOperations(PoolMatcher matcher) {
        super();
        this.matcher = matcher;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String jndiName;
        if (!address.getElement(0).getKey().equals(ModelDescriptionConstants.DEPLOYMENT) &&
                context.readModel(PathAddress.EMPTY_ADDRESS).isDefined()) {
            jndiName = Util.getJndiName(context.readModel(PathAddress.EMPTY_ADDRESS));
        } else {
            jndiName = address.getLastElement().getValue();
        }

        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                            ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE);
                    if (managementRepoService != null) {
                        ModelNode operationResult = null;
                        try {
                            final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                            final List<Pool> pools = matcher.match(jndiName, repository);

                            if (pools.isEmpty()) {
                                throw MESSAGES.failedToMatchPool(jndiName);
                            }

                            for (Pool pool : pools) {
                                operationResult = invokeCommandOn(pool);
                            }

                        } catch (Exception e) {
                            throw new OperationFailedException(new ModelNode().set(MESSAGES.failedToInvokeOperation(e.getLocalizedMessage())));
                        }
                        if (operationResult != null) {
                            context.getResult().set(operationResult);
                        }
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }

    protected abstract ModelNode invokeCommandOn(Pool pool) throws Exception;

    public static class FlushIdleConnectionInPool extends PoolOperations {
        public static FlushIdleConnectionInPool DS_INSTANCE = new FlushIdleConnectionInPool(new DsPoolMatcher());
        public static FlushIdleConnectionInPool RA_INSTANCE = new FlushIdleConnectionInPool(new RaPoolMatcher());

        protected FlushIdleConnectionInPool(PoolMatcher matcher) {
            super(matcher);
        }

        @Override
        protected ModelNode invokeCommandOn(Pool pool) {
            pool.flush();
            return null;
        }

    }

    public static class FlushAllConnectionInPool extends PoolOperations {
        public static FlushAllConnectionInPool DS_INSTANCE = new FlushAllConnectionInPool(new DsPoolMatcher());
        public static FlushAllConnectionInPool RA_INSTANCE = new FlushAllConnectionInPool(new RaPoolMatcher());

        protected FlushAllConnectionInPool(PoolMatcher matcher) {
            super(matcher);
        }

        @Override
        protected ModelNode invokeCommandOn(Pool pool) {
            pool.flush(true);
            return null;
        }

    }

    public static class TestConnectionInPool extends PoolOperations {
        public static TestConnectionInPool DS_INSTANCE = new TestConnectionInPool(new DsPoolMatcher());
        public static TestConnectionInPool RA_INSTANCE = new TestConnectionInPool(new RaPoolMatcher());

        protected TestConnectionInPool(PoolMatcher matcher) {
            super(matcher);
        }

        @Override
        protected ModelNode invokeCommandOn(Pool pool) throws Exception {
            boolean returnedValue = pool.testConnection();
            if (!returnedValue)
                throw MESSAGES.invalidConnection();
            ModelNode result = new ModelNode();
            result.add(returnedValue);
            return result;
        }

    }

    private static interface PoolMatcher {
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
}
