/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment.unit;

import org.jboss.as.deployment.SimpleAttachable;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;

/**
 * Default implementation for DeploymentUnitContext.
 *
 * @author John E. Bailey
 */
public class DeploymentUnitContextImpl extends SimpleAttachable implements DeploymentUnitContext {
    private final String name;
    private final BatchBuilder batchBuilder;
    private final BatchServiceBuilder<Void> serviceBuilder;

    /**
     * Construct new instance.
     *
     * @param name The deployment unit name.
     * @param batchBuilder The batch builder
     */
    public DeploymentUnitContextImpl(String name, BatchBuilder batchBuilder, BatchServiceBuilder<Void> serviceBuilder) {
        this.name = name;
        this.batchBuilder = batchBuilder;
        this.serviceBuilder = serviceBuilder;
    }

    /** {@inheritDoc} */
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    public BatchBuilder getBatchBuilder() {
        return batchBuilder;
    }

    /** {@inheritDoc} */
    public BatchServiceBuilder<Void> getBatchServiceBuilder() {
        return serviceBuilder;
    }
}
