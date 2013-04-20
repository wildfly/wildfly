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

package org.jboss.as.camel;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.StartException;
import org.osgi.framework.Bundle;

/**
 * Logging Id ranges: 20100-20199
 *
 * https://community.jboss.org/wiki/LoggingIds
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Thomas.Diesler@jboss.com
 */
@MessageBundle(projectCode = "JBAS")
public interface CamelMessages {

    /**
     * The messages.
     */
    CamelMessages MESSAGES = Messages.getBundle(CamelMessages.class);

    @Message(id = 20100, value = "%s is null")
    IllegalArgumentException illegalArgumentNull(String name);

    @Message(id = 20101, value = "Cannot obtain camel module: %s")
    StartException cannotObtainCamelModule(@Cause Throwable th, ModuleIdentifier moduleIdentifier);

    @Message(id = 20102, value = "Cannot register camel bundle")
    StartException cannotRegisterCamelBundle(@Cause Throwable th);

    @Message(id = 20103, value = "Cannot start camel bundle: %s")
    StartException cannotStartCamelBundle(@Cause Throwable th, Bundle bundle);
}

