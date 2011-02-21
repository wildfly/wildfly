/**
 *
 */
package org.jboss.as.domain.controller;

import org.jboss.dmr.ModelNode;

/**
 * TODO add class javadoc for DomainControllerSlave
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public interface DomainControllerSlave extends DomainController {

    void setInitialDomainModel(ModelNode initialModel);
}
