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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponent;

/**
 * Handles operations that provide runtime management of a {@link StatefulSessionComponent}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class StatefulSessionBeanRuntimeHandler extends AbstractEJBComponentRuntimeHandler<StatefulSessionComponent> {

    public static final StatefulSessionBeanRuntimeHandler INSTANCE = new StatefulSessionBeanRuntimeHandler();

    private StatefulSessionBeanRuntimeHandler() {
        super(EJBComponentType.STATEFUL, StatefulSessionComponent.class);
    }

    @Override
    protected void executeReadAttribute(String attributeName, OperationContext context, StatefulSessionComponent component, PathAddress address) {
        super.executeReadAttribute(attributeName, context, component, address);
        //TODO expose the cache
    }
}
