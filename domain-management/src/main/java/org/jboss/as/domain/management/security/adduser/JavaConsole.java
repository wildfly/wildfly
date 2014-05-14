/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2011, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.domain.management.security.adduser;

import static org.jboss.as.domain.management.logging.DomainManagementLogger.ROOT_LOGGER;

import java.io.Console;
import java.io.IOError;
import java.util.IllegalFormatException;

/**
 * Describe the purpose
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class JavaConsole implements ConsoleWrapper {

    private Console theConsole = System.console();

    @Override
    public void format(String fmt, Object... args) throws IllegalFormatException {
        if (hasConsole()) {
            theConsole.format(fmt, args);
        } else {
            System.out.format(fmt, args);
        }
    }

    @Override
    public void printf(String format, Object... args) throws IllegalFormatException {
        if (hasConsole()) {
            theConsole.printf(format, args);
        } else {
            System.out.format(format, args);
        }
    }

    @Override
    public String readLine(String fmt, Object... args) throws IOError {
        if (hasConsole()) {
            return theConsole.readLine(fmt, args);
        } else {
            throw ROOT_LOGGER.noConsoleAvailable();
        }
    }

    @Override
    public char[] readPassword(String fmt, Object... args) throws IllegalFormatException, IOError {
        if (hasConsole()) {
            return theConsole.readPassword(fmt, args);
        } else {
            throw ROOT_LOGGER.noConsoleAvailable();
        }
    }

    @Override
    public boolean hasConsole() {
        return theConsole != null;
    }


}
