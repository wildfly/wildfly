/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
        super(EJB3SubsystemModel.TIMER_SERVICE_PATH,
                EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.TIMER_SERVICE), null, null,OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        this.parentHandler = parentHandler;
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
        resourceRegistration.registerSubModel(new TimerResourceDefinition<T>(this.parentHandler));
    }
}
