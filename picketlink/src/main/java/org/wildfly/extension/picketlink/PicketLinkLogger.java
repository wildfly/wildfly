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

package org.wildfly.extension.picketlink;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.picketlink.identity.federation.core.audit.PicketLinkAuditEventType;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

/**
 * <p>This module is using message IDs in the range 12500-12599 and 21200-21299.</p>
 *
 * <p>This file is using the subset 21200-21299 for logger messages.</p>
 *
 * <p>See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved JBAS message id blocks.</p>
 *
 * @author Pedro Igor
 */
@MessageLogger(projectCode = "JBAS")
public interface PicketLinkLogger extends BasicLogger {

    PicketLinkLogger ROOT_LOGGER = Logger.getMessageLogger(PicketLinkLogger.class, PicketLinkLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 21200, value = "Activating PicketLink %s Subsystem")
    void activatingSubsystem(String name);

    @LogMessage(level = INFO)
    @Message(id = 21201, value = "Configuring PicketLink Federation for deployment [%s]")
    void federationConfiguringDeployment(String deploymentName);

    @LogMessage(level = INFO)
    @Message(id = 21299, value = "Bound [%s] to [%s]")
    void boundToJndi(String alias, String jndiName);

    @LogMessage(level = INFO)
    @Message(id = 21300, value = "Ignoring unexpected event type [%s]")
    void federationIgnoringAuditEvent(PicketLinkAuditEventType eventType);

    @LogMessage(level = ERROR)
    @Message(id = 21301, value = "Error while configuring the metrics collector. Metrics will not be collected.")
    void federationErrorCollectingMetric(@Cause Throwable t);
}
