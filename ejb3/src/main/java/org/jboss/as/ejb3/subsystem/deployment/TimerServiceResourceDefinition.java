/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem.deployment;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.subsystem.EJB3Extension;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;

/**
 * {@link ResourceDefinition} for the timer-service resource for runtime ejb deployment.
 * As of now this is dummy path impl, since mgmt ops are supported by top level service=timer-service
 * @author baranowb
 */
public class TimerServiceResourceDefinition<T extends EJBComponent> extends SimpleResourceDefinition {

    private final AbstractEJBComponentRuntimeHandler<T> parentHandler;
    TimerServiceResourceDefinition(AbstractEJBComponentRuntimeHandler<T> parentHandler) {
        super(new SimpleResourceDefinition.Parameters(EJB3SubsystemModel.TIMER_SERVICE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.TIMER_SERVICE))
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES));
        this.parentHandler = parentHandler;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new TimerResourceDefinition<T>(this.parentHandler));
    }
}
