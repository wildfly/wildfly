/**
 *
 */
package org.jboss.as.domain.controller;

import org.jboss.as.controller.NewModelController;
import org.jboss.dmr.ModelNode;

/**
 * A {@link org.jboss.as.domain.controller.DomainController} that isn't acting as the master for the domain.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface NewDomainControllerSlave extends NewModelController {

    /**
     * Sets the domain model the slave domain controller should use.
     *
     * @param initialModel the model. Will not be {@code null}
     */
    void setInitialDomainModel(ModelNode initialModel);
}
