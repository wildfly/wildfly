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

package org.wildfly.ee.concurrent;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

import java.lang.reflect.InvocationHandler;
import java.util.concurrent.RejectedExecutionException;

/**
 * This module is using message IDs in the range 17800-17899.
 * <p/>
 * This file is using the subset 17850-17899 for non-logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved WFLY message id blocks.
 * <p/>
 *
 * @author Eduardo Martins
 */
@MessageBundle(projectCode = "WFLY")
public interface EeConcurrentMessages {

    /**
     * The messages.
     */
    EeConcurrentMessages MESSAGES = Messages.getBundle(EeConcurrentMessages.class);

    @Message(id = 17850, value = "The lifecycle of a ManagedExecutorService is done by the application server.")
    IllegalStateException lifecycleOfManagedExecutorServiceIsDoneByTheApplicationServer();

    @Message(id = 17851, value = "The ManagedExecutorService is shutdown.")
    RejectedExecutionException managedExecutorServiceShutdown();

    @Message(id = 17852, value = "Unexpected InvocationHandler type: %s")
    IllegalArgumentException unexpectedInvocationHandlerType(InvocationHandler invocationHandler);
}
