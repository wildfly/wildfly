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

package org.jboss.as.modcluster;

import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * Date: 12.09.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
interface ModClusterMessages {

    /**
     * The messages.
     */
    ModClusterMessages MESSAGES = Messages.getBundle(ModClusterMessages.class);

    /**
     * A message indicating a class attribute is needed for the attribute represented by the {@code attributeName}
     * parameter.
     *
     * @param attributeName the name of the required attribute.
     * @return the message.
     */
    @Message(id = 11730, value = "A class attribute is needed for %s")
    String classAttributeRequired(String attributeName);

    /**
     * A message indicating the context and host are needed.
     *
     * @return the message.
     */
    @Message(id = 11731, value = "need context and host")
    String needContextAndHost();

    /**
     * A message indicating a type attribute is needed for the attribute represented by the {@code attributeName}
     * parameter.
     *
     * @param attributeName the name of the required attribute.
     * @return the message.
     */
    @Message(id = 11732, value = "A type attribute is needed for %s")
    String typeAttributeRequired(String attributeName);

    /**
     * A message indicating that the virtualhost or the context can't be found by modcluster.
     * @param Host
     * @param Context
     * @return the message.
     */
    @Message(id = 11733, value = "virtualhost: %s or context %s not found")
    String ContextorHostNotFound(String Host, String Context);
}
