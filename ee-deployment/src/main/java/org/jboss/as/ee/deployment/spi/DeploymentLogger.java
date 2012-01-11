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
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import javax.enterprise.deploy.spi.TargetModuleID;

import java.io.File;
import java.io.IOException;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;
import static org.jboss.logging.Logger.Level.ERROR;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface DeploymentLogger extends BasicLogger {

    /**
     * A logger with the category of the package name.
     */
    DeploymentLogger ROOT_LOGGER = Logger.getMessageLogger(DeploymentLogger.class, DeploymentLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 16100, value = "Begin deploy: %s")
    void beginDeploy(TargetModuleID targetModuleID);

    @LogMessage(level = INFO)
    @Message(id = 16101, value = "End deploy: %s")
    void endDeploy(TargetModuleID targetModuleID);

    @LogMessage(level = WARN)
    @Message(id = 16102, value = "Cannot determine module type of: %s")
    void cannotDetermineModuleType(ModelNode node);

    @LogMessage(level = WARN)
    @Message(id = 16103, value = "Cannot delete deployment file %s, will be deleted on exit")
    void cannotDeleteDeploymentFile(File deployment);

    @LogMessage(level = ERROR)
    @Message(id = 16104, value = "Cannot transform deployment plan to XML")
    void cannotTransformDeploymentPlanToXML(@Cause IOException ex);

    @LogMessage(level = ERROR)
    @Message(id = 16105, value = "Deployment does not exist: %s")
    void deploymentDoesNotExist(File deployment);
}
