package org.jboss.as.connector.pool;

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.connectionmanager.pool.Pool;
import org.jboss.jca.core.api.management.Connector;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

public abstract class PoolOperations implements OperationStepHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.datasources");

    private final PoolMatcher matcher;

    protected PoolOperations(PoolMatcher matcher) {
        super();
        this.matcher = matcher;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String jndiName = address.getLastElement().getValue();

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                            ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE);
                    if (managementRepoService != null) {
                        ModelNode operationResult = null;
                        try {
                            final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                            List<Pool> pools = matcher.match(jndiName, repository);

                            for (Pool pool : pools) {
                                operationResult = invokeCommandOn(pool);
                            }

                        } catch (Exception e) {
                            throw new OperationFailedException(new ModelNode().set("failed to set attribute" + e.getMessage()));
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

    protected abstract ModelNode invokeCommandOn(Pool pool);

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
        protected ModelNode invokeCommandOn(Pool pool) {
            boolean returnedValue = pool.testConnection();
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
                    if (jndiName.equalsIgnoreCase(c.getUniqueId())) {
                        if (c.getConnectionFactories() == null || c.getConnectionFactories().get(0) == null
                                || c.getConnectionFactories().get(0).getPool() == null)
                            continue;
                        result.add(c.getConnectionFactories().get(0).getPool());
                    }

                }
            }
            return result;
        }
    }
}
