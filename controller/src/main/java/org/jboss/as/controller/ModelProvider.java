/**
 *
 */
package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;

/**
 * An object that provides access to a model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ModelProvider {

    /**
     * Gets the model.
     *
     * @return the model. Will not return {@code null}
     */
    ModelNode getModel();
}
