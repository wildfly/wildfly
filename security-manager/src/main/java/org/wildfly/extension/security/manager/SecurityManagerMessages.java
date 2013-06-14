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
/ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.wildfly.extension.security.manager;

import javax.xml.stream.XMLStreamException;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * The security manager subsystem is using message IDs in the range 17700-17799. This file is using the subset
 * 17750-17799 for non-logger messages.
 * </p>
 * See http://http://community.jboss.org/wiki/LoggingIds for the full list of currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface SecurityManagerMessages {

    SecurityManagerMessages MESSAGES = Messages.getBundle(SecurityManagerMessages.class);

    /**
     * Create {@link javax.xml.stream.XMLStreamException} to indicate an invalid version was found in the permissions element.
     *
     * @param found the version that was found in the element.
     * @param expected the expected version.
     * @return the constructed {@link javax.xml.stream.XMLStreamException}
     */
    @Message(id = 17750, value = "Invalid version found in the permissions element. Found %s, expected %s")
    XMLStreamException invalidPermissionsXMLVersion(String found, String expected);

}
