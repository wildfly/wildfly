/**
 *
 */
package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;

/**
 * TODO add class javadoc for TransactionalModelController
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public interface TransactionalModelController extends ModelController {

    OperationResult execute(ModelNode operation, ResultHandler handler, ControllerTransactionContext transaction);
}
