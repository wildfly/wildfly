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

package org.jboss.as.ee.deployment.spi;

import org.jboss.dmr.ModelNode;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import java.io.File;
import java.net.URI;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface DeploymentMessages {

    DeploymentMessages MESSAGES = Messages.getBundle(DeploymentMessages.class);

    @Message(id = 16150, value = "Cannot find deployment file: %s")
    String cannotFindDeploymentFile(String filename);

    @Message(id = 16151, value = "Deployment validation failed")
    String deploymentValidationFailed();

    @Message(id = 16152, value = "Cannot obtain meta data")
    String cannotObtainMetaData();

    @Message(id = 16153, value = "Operation %s failed on target: %s")
    String operationFailedOnTarget(CommandType cmdType, Target target);

    @Message(id = 16154, value = "Operation %s started")
    String operationStarted(CommandType cmdType);

    @Message(id = 16155, value = "Operation %s completed")
    String operationCompleted(CommandType cmdType);

    @Message(id = 16156, value = "Cannot obtain module type for: %s")
    String cannotObtainModuleType(String moduleName);

    @Message(id = 16157, value ="Cannot delete existing deployment: %s")
    String cannotDeleteExistingDeployment(File deployment);

    @Message(id = 16158, value = "DeploymentManager not connected")
    String deploymentManagerNotConnected();

    @Message(id = 16159, value = "Invalid targetType in URI: %s")
    String invalidTargetType(URI deployURI);

    @Message(id = 16160, value = "Null %s")
    String nullArgument(String param);

    @Message(id = 16161, value = "Module type not supported: %s")
    String moduleTypeNotSupported(ModuleType type);

    @Message(id = 16162, value = "Deployment plan does not contain entry: %s")
    String deployementPlanDoesNotContainEntry(String entryname);

    @Message(id = 16163, value = "Opaque deployment URI not implemented")
    String opaqueDeploymentUriNotImplemented();

    @Message(id = 16164, value = "Cannot connect to management target: %s")
    String cannotConnectToManagementTarget(URI deployURI);

    @Message(id = 16165, value = "Management request failed: %s")
    String managementRequestFailed(ModelNode node);
}
