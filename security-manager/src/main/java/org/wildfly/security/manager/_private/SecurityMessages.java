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

package org.wildfly.security.manager._private;

import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.Permission;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

import static org.jboss.logging.Logger.Level.DEBUG;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFSECMGR")
public interface SecurityMessages {
    SecurityMessages access = Logger.getMessageLogger(SecurityMessages.class, "org.wildfly.security.access");

    @LogMessage(level = DEBUG)
    @Message(value = "Permission check failed (permission \"%s\" in code source \"%s\" of \"%s\", principals \"%s\")")
    void accessCheckFailed(Permission permission, CodeSource codeSource, ClassLoader classLoader, String principals);

    @LogMessage(level = DEBUG)
    @Message(value = "Permission check failed (permission \"%s\" in code source \"%s\" of \"%s\")")
    void accessCheckFailed(Permission permission, CodeSource codeSource, ClassLoader classLoader);

    @Message(id = 1, value = "Permission check failed for %s")
    AccessControlException accessControlException(@Param Permission permission, Permission permission_);

    @Message(id = 2, value = "Security manager may not be changed")
    SecurityException secMgrChange();

    @Message(id = 3, value = "Unknown security context type")
    SecurityException unknownContext();
}
