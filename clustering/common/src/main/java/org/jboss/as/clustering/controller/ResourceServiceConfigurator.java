/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Configures a {@link org.jboss.msc.Service} using the model of a resource.
 * @author Paul Ferraro
 */
public interface ResourceServiceConfigurator extends ServiceConfigurator {

    /**
     * Configures a service using the specified operation context and model.
     * @param context an operation context, used to resolve capabilities and expressions
     * @param model the resource model
     * @return the reference to this configurator
     * @throws OperationFailedException if there was a failure reading the model or resolving expressions/capabilities
     */
    default ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return this;
    }
}
