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

package org.jboss.as.jdkorb;

import java.io.IOException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;

/**
 * The jacorb subsystem is using message IDs in the range 16300-16499. This file is using the subset 16400-16499 for
 * exception messages.
 * See http://http://community.jboss.org/wiki/LoggingIds for the full list of currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface JDKORBMessages {

    JDKORBMessages MESSAGES = Messages.getBundle(JDKORBMessages.class);

    @Message(id = 16400, value = "SSL support has been enabled but no security domain has been specified")
    OperationFailedException noSecurityDomainSpecified();

    @Message(id = 16443, value = "keyManager[] is null for security domain %s")
    IOException errorObtainingKeyManagers(String securityDomain);

    @Message(id = 16444, value = "Failed to get SSL context")
    IOException failedToGetSSLContext(@Param Throwable cause);

}
