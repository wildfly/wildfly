/**
 *
 */
package org.jboss.as.controller;

import org.jboss.as.controller.persistence.ConfigurationPersisterProvider;

/**
 * Provides a context in a which a {@link ModelController} can execute an {@link Operation}
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface OperationControllerContext {

    ModelProvider getModelProvider();

    OperationContextFactory getOperationContextFactory();

    ConfigurationPersisterProvider getConfigurationPersisterProvider();

    ControllerTransactionContext getControllerTransactionContext();
}
