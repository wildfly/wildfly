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

package org.jboss.as.model;

import org.jboss.as.deployment.client.api.server.ServerDeploymentActionResult;
import org.jboss.msc.service.ServiceContainer;


/**
 * Update used when updating a deployment element to be started.
 *
 * @author Brian Stansberry
 */
public class ServerModelDeploymentFullReplaceUpdate extends AbstractServerModelUpdate<ServerDeploymentActionResult> {
    private static final long serialVersionUID = 5773083013951607950L;
//    private static final Logger log = Logger.getLogger("org.jboss.as.model");

    private final ServerModelDeploymentAddUpdate addUpdate;
    private final ServerModelDeploymentRemoveUpdate removeUpdate;

    public ServerModelDeploymentFullReplaceUpdate(final String deploymentUniqueName, final String deploymentFileName, byte[] hash) {
        this(new ServerModelDeploymentAddUpdate(deploymentUniqueName, deploymentFileName, hash, true),
             new ServerModelDeploymentRemoveUpdate(deploymentUniqueName, true));
    }

    private ServerModelDeploymentFullReplaceUpdate(ServerModelDeploymentAddUpdate add,
                                                   ServerModelDeploymentRemoveUpdate remove) {
        addUpdate = add;
        removeUpdate = remove;
    }


    @Override
    public void applyUpdate(ServerModel element) throws UpdateFailedException {
        removeUpdate.applyUpdate(element);
        addUpdate.applyUpdate(element);
    }

    @Override
    public <P> void applyUpdate(ServiceContainer container,
            UpdateResultHandler<? super ServerDeploymentActionResult, P> resultHandler, P param) {
        removeUpdate.applyUpdate(container, resultHandler, param);
        addUpdate.applyUpdate(container, resultHandler, param);
    }

    @Override
    public AbstractServerModelUpdate<?> getCompensatingUpdate(ServerModel original) {
        return new ServerModelDeploymentFullReplaceUpdate(removeUpdate.getCompensatingUpdate(original),
                addUpdate.getCompensatingUpdate(original));
    }
}
