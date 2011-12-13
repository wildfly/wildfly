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

package org.jboss.as.domain.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONCURRENT_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;

import org.jboss.as.controller.RunningMode;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * This module is using message IDs in the range 10900-10999.
 * This file is using the subset 10975-10999 for domain controller non-logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface DomainControllerMessages {

    /**
     * The messages.
     */
    DomainControllerMessages MESSAGES = Messages.getBundle(DomainControllerMessages.class);

    /**
     * Creates an exception message indicating this host is a slave and cannot accept registrations from other slaves.
     *
     * @return a message for the error.
     */
    @Message(id = 10975, value = "Registration of remote hosts is not supported on slave host controllers")
    String slaveControllerCannotAcceptOtherSlaves();

    /**
     * Creates an exception message indicating this host is in admin mode and cannot accept registrations from other slaves.
     *
     * @param runningMode the host controller's current running mode
     *
     * @return a message for the error.
     */
    @Message(id = 10976, value = "The master host controller cannot register slave host controllers as it's current running mode is '%s'")
    String adminOnlyModeCannotAcceptSlaves(RunningMode runningMode);

    /**
     * Creates an exception message indicating a host cannot register because another host of the same name is already registered.
     *
     * @param slaveName the name of the slave
     *
     * @return a message for the error.
     */
    @Message(id = 10977, value = "There is already a registered host named '%s'")
    String slaveAlreadyRegistered(String slaveName);

    /**
     * Creates an exception message indicating that a parent is missing a required child.
     *
     * @param parent  the name of the parent element
     * @param child   the name of the missing child element
     * @param parentSpec   the complete string representation of the parent element
     * @return  the error message
     */
    @Message(id = 10978, value = "%s is missing %s: %s")
    String requiredChildIsMissing(String parent, String child, String parentSpec);

    /**
     * Creates an exception message indicating that a parent recognizes only
     * the specified children.
     *
     * @param parent  the name of the parent element
     * @param children  recognized children
     * @param parentSpec  the complete string representation of the parent element
     * @return  the error message
     */
    @Message(id = 10979, value = "%s recognized only %s as children: %s")
    String unrecognizedChildren(String parent, String children, String parentSpec);

    /**
     * Creates an exception message indicating that in-series is missing groups.
     *
     * @param rolloutPlan  string representation of a rollout plan
     * @return  the error message
     */
    @Message(id = 10980, value = IN_SERIES + " is missing groups: %s")
    String inSeriesIsMissingGroups(String rolloutPlan);

    /**
     * Creates an exception message indicating that server-group expects one
     * and only one child.
     *
     * @param rolloutPlan  string representation of a rollout plan
     * @return  the error message
     */
    @Message(id = 10981, value = SERVER_GROUP + " expects one and only one child: %s")
    String serverGroupExpectsSingleChild(String rolloutPlan);

    /**
     * Creates an exception message indicating that one of the groups in
     * rollout plan does not define neither server-group nor concurrent-groups.
     *
     * @param rolloutPlan  string representation of a rollout plan
     * @return  the error message
     */
    @Message(id = 10982, value = "One of the groups does not define neither " + SERVER_GROUP + " nor " + CONCURRENT_GROUPS + ": %s")
    String unexpectedInSeriesGroup(String rolloutPlan);
}
