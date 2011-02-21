/**
 *
 */
package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;

/**
 * TODO add class javadoc for ControllerTransactionContext
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public interface ControllerTransactionContext {

    ModelNode getTransactionId();

    void registerResource(ControllerResource resource);

    void deregisterResource(ControllerResource resource);

    void setRollbackOnly();

}
