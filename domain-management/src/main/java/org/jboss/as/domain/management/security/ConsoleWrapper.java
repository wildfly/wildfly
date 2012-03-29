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

package org.jboss.as.domain.management.security;

import java.io.IOError;
import java.util.IllegalFormatException;

/**
 * Wrap the console commands
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public interface ConsoleWrapper<T> {

    /**
     * Writes a formatted string to this console's output stream using
     * the specified format string and arguments.
     * see <a href="../util/Formatter.html#syntax">Format string syntax</a>
     * @param fmt
     * @param args
     */
    T format(String fmt, Object ...args) throws IllegalFormatException;

    /**
     * A convenience method to write a formatted string to this console's
     * output stream using the specified format string and arguments.
     *
     * @param format
     * @param args
     * @throws IllegalStateException
     */
    void printf(String format, Object ... args) throws IllegalFormatException;

    /**
     * Provides a formatted prompt, then reads a single line of text from the
     * console.
     *
     * @param fmt
     * @param args
     * @return A string containing the line read from the console, not
     *          including any line-termination characters, or <tt>null</tt>
     *          if an end of stream has been reached.
     * @throws IOError
     */
    String readLine(String fmt, Object ... args) throws IOError;

    /**
     * Provides a formatted prompt, then reads a password or passphrase from
     * the console with echoing disabled.
     *
     * @param fmt
     * @param args
     * @return  A character array containing the password or passphrase read
     *          from the console.
     * @throws IOError
     */
    char[] readPassword(String fmt, Object ... args) throws IllegalFormatException, IOError;

    /**
     *  Return the console object
     *
     * @return Return the console object
     */
    T getConsole();
}
