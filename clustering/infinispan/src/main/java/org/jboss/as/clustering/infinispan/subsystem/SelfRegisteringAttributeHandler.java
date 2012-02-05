package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/*
 * @author Richard Achmatowicz (c) 2011 Red Hat. Inc
 */
interface SelfRegisteringAttributeHandler extends OperationStepHandler {
    void registerAttributes(final ManagementResourceRegistration registry);
}
