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

import org.jboss.as.server.deployment.scanner.api.DeploymentScanner;
import org.jboss.dmr.ModelNode;

/**
 * Toggle the 'auto-deploy-exploded' attribute on a {@code DeploymentScanner}.
 *
 * @author Brian Stansberry
 */
class WriteAutoDeployExplodedAttributeHandler extends AbstractWriteAttributeHandler {

    static final WriteAutoDeployExplodedAttributeHandler INSTANCE = new WriteAutoDeployExplodedAttributeHandler();

    private WriteAutoDeployExplodedAttributeHandler() {
        super(DeploymentScannerDefinition.AUTO_DEPLOY_EXPLODED);
    }

    @Override
    protected void updateScanner(final DeploymentScanner scanner, final ModelNode newValue) {

        boolean enable = newValue.resolve().asBoolean();

        scanner.setAutoDeployExplodedContent(enable);
    }
}
