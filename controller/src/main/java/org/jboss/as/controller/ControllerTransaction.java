/**
 *
 */
package org.jboss.as.controller;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * A transactional context in which operations on a {@link ModelController} execute.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ControllerTransaction implements ControllerTransactionContext {

    private static final Logger log = Logger.getLogger("org.jboss.as.controller");

    private final Set<ControllerResource> resources = new LinkedHashSet<ControllerResource>();
    private final Set<ControllerTransactionSynchronization> synchronizations = new LinkedHashSet<ControllerTransactionSynchronization>();
    private volatile boolean rollbackOnly;
    private final ModelNode id;
    private final long creationTime = System.currentTimeMillis();

    public ControllerTransaction() {
        final UUID uuid = UUID.randomUUID();
        id = new ModelNode();
        id.add(uuid.getMostSignificantBits());
        id.add(uuid.getLeastSignificantBits());
    }

    public ControllerTransaction(ModelNode id) {
        this.id = id;
    }

    public void commit() {

        for (ControllerTransactionSynchronization sync : synchronizations) {
            try {
                sync.beforeCompletion();
            } catch (Exception e) {
                log.errorf(e, "Caught exception applying beforeCompletion notification for transaction %s on synchronization %s", id, sync);
                // TODO further actions?
            }

        }

        for (ControllerResource resource : resources) {
            try {
                if (rollbackOnly)
                    resource.rollback();
                else
                    resource.commit();
            } catch (Exception e) {
                if (rollbackOnly ) {
                    log.errorf(e, "Caught exception rolling back transaction %s on resource %s", id, resource);
                }
                else {
                    log.errorf(e, "Caught exception committing transaction %s on resource %s", id, resource);
                }
                // TODO further actions?
            }
        }

        for (ControllerTransactionSynchronization sync : synchronizations) {
            try {
                sync.afterCompletion(!rollbackOnly);
            } catch (Exception e) {
                log.errorf(e, "Caught exception applying beforeCompletion notification for transaction %s on synchronization %s", id, sync);
                // TODO further actions?
            }

        }
    }

    @Override
    public ModelNode getTransactionId() {
        return id;
    }

    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public void registerResource(ControllerResource resource) {
        resources.add(resource);
    }

    @Override
    public void deregisterResource(ControllerResource resource) {
        resources.remove(resource);
    }

    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public void registerSynchronization(ControllerTransactionSynchronization synchronization) {
        synchronizations.add(synchronization);
    }
}
