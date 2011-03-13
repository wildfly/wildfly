/**
 *
 */
package org.jboss.as.controller;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jboss.dmr.ModelNode;

/**
 * TODO add class javadoc for ControllerTransaction
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class ControllerTransaction implements ControllerTransactionContext {

    private final Set<ControllerResource> resources = new HashSet<ControllerResource>();
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
        for (ControllerResource resource : resources) {
            if (rollbackOnly)
                resource.rollback();
            else
                resource.commit();
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
}
