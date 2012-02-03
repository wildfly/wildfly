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

package org.jboss.as.clustering.jgroups;

import java.net.URL;

import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * Date: 29.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface JGroupsMessages {
    /**
     * The messages.
     */
    JGroupsMessages MESSAGES = Messages.getBundle(JGroupsMessages.class);

    /**
     * A message indicating a file could not be parsed.
     *
     * @param url the path to the file.
     *
     * @return the message.
     */
    @Message(id = 10270, value = "Failed to parse %s")
    String parserFailure(URL url);

    /**
     * A message indicating a resource could not be located.
     *
     * @param resource the resource that could not be located.
     *
     * @return the message.
     */
    @Message(id = 10271, value = "Failed to locate %s")
    String notFound(String resource);

    @Message(id = 10272, value = "A node named %s already exists in this cluster.  Perhaps there is already a server running on this host?  If so, restart this server with a unique node name, via -Djboss.node.name=<node-name>")
    IllegalStateException duplicateNodeName(String name);
}
