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

package org.jboss.as.host.controller;

import org.jboss.as.controller.RunningMode;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 10900-10999.
 * This file is using the subset 10925-10949 for host controller non-logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface HostControllerMessages {

    /**
     * The messages.
     */
    HostControllerMessages MESSAGES = Messages.getBundle(HostControllerMessages.class);

    /**
     * Creates an error message indicating this host is a slave and cannot connect to the master host controller.
     *
     * @return a message for the error.
     */
    @Message(id = 10925, value = "Could not connect to master. Aborting. Error was: %s")
    String cannotConnectToMaster(Exception e);

    /**
     * Creates an error message indicating this host had no domain controller configuration and cannot start
     * if not in {@link RunningMode#ADMIN_ONLY} mode.
     *
     * @return a message for the error.
     */
    @Message(id = 10926, value = "No <domain-controller> configuration was provided and the current running mode ('%s') " +
            "requires access to the Domain Controller host. Startup will be aborted. Use the %s command line argument " +
            "to start in %s mode if you need to start without a domain controller connection and then use the management " +
            "tools to configure one.")
    String noDomainControllerConfigurationProvided(RunningMode currentRunningMode, String adminOnlyCmdLineArg, RunningMode validRunningMode);
}
