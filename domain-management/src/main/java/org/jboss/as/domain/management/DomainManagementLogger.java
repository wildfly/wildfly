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

package org.jboss.as.domain.management;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface DomainManagementLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    DomainManagementLogger ROOT_LOGGER = Logger.getMessageLogger(DomainManagementLogger.class, DomainManagementLogger.class.getPackage().getName());

    /**
     * Logs a warning message indicating the user and password were found in the properties file.
     */
    @LogMessage(level = WARN)
    @Message(id = 15200, value = "Properties file defined with default user and password, this will be easy to guess.")
    void userAndPasswordWarning();

    /**
     * Logs a warning message indicating that whitespace has been trimmed from the password when it was
     * decoded from Base64.
     */
    @LogMessage(level = WARN)
    @Message(id = 15201, value = "Whitespace has been trimmed from the Base64 representation of the secret identity.")
    void whitespaceTrimmed();

    /**
     * Logs a warning message indicating that the password attribute is deprecated that that keystore-password
     * should be used instead.
     */
    @LogMessage(level = WARN)
    @Message(id = 15202, value = "The attribute 'password' is deprecated, 'keystore-password' should be used instead.")
    void passwordAttributeDeprecated();

    /**
     * Logs a message indicating that the name of the realm does not match the name used in the properties file.
     */
    @LogMessage(level = WARN)
    @Message(id = 15203, value = "The realm name of the defined security realm '%s' does not match the realm name within the properties file '%s'.")
    void realmMisMatch(final String realmRealmName, final String fileRealmName);

    /*
     * Logging IDs 15200 to 15299 are reserved for domain management, the file DomainManagementMessages also contains messages
     * in this range commencing 15220.
     */

}
