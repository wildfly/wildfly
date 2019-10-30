/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.mod_cluster;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.modcluster.load.metric.LoadMetric;

/**
 * Mechanism to plug in {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} on boot time, e.g. Undertow
 * {@link io.undertow.server.HttpHandler}. Passes on a reference to sets with enabled {@link LoadMetric}s and event handler
 * adapters.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public interface BoottimeHandlerProvider {
    void performBoottime(OperationContext context, Set<String> adapterNames, Set<LoadMetric> enabledMetrics) throws OperationFailedException;
}
