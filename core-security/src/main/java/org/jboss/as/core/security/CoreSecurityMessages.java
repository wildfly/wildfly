/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.core.security;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * This module is using message IDs in the range 20900-20999.
 * <p/>
 * This file is using the subsets 20900-20949. If ever a logger is needed that can have 20950-20999.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved JBAS message id blocks.
 * <p/>
 *
 * Reserved logging id ranges from: http://community.jboss.org/wiki/LoggingIds: 20900 - 20999
 *
 * @author <a href="mailto:jperkins@redhat.com">Kabir Khan</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface CoreSecurityMessages {

    /**
     * The messages
     */
    CoreSecurityMessages MESSAGES = Messages.getBundle(CoreSecurityMessages.class);

    @Message(id = 20900, value = "'%s' can not be null.")
    IllegalArgumentException canNotBeNull(String name);

}
