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

package org.jboss.as.server.deployment.scanner;

import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Update the 'deployment-timeout' attribute on a {@code DeploymentScanner}.
 *
 * @author John Bailey
 */
public class WriteDeploymentTimeoutAttributeHandler extends AbstractWriteAttributeHandler {
    static final WriteDeploymentTimeoutAttributeHandler INSTANCE = new WriteDeploymentTimeoutAttributeHandler();

    public WriteDeploymentTimeoutAttributeHandler() {
        super(new ModelTypeValidator(ModelType.LONG, false, true), new ModelTypeValidator(ModelType.LONG, false, false));
    }

    protected void updateScanner(final DeploymentScanner scanner, final ModelNode newValue) {
        final long timeout = newValue.resolve().asLong();
        scanner.setDeploymentTimeout(timeout);
    }
}
