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

import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

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
}
