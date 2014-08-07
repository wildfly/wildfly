/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cli;

import org.jboss.as.cli.operation.ParsedCommandLine;


/**
 * Redirection for handling of entered command lines.
 *
 * Normally, entered command lines are parsed and handled immediately
 * by the appropriate handlers.
 *
 * An implementation of this interface (once registered to the CommandContext),
 * will be intervening after the parsing step and will be taking the handling
 * of the command lines from that point. Until it decides to unregister from
 * the CommandContext.
 *
 * @author Alexey Loubyansky
 */
public interface CommandLineRedirection {

    /**
     * This method is called when the instance is
     * registered to the command context.
     *
     * @param registration  the registration object
     */
    void set(Registration registration);

    /**
     * As the user enters a new command line, the command line is parsed
     * and, if there were no parsing errors, this method is invoked.
     * In case parsing failed, the user will be notified immediately
     * and this method will not be called.
     * The implementation of this method can also throw an instance
     * of CommandLineException in case it encountered a problem handling
     * the command line.
     *
     * @param ctx  current instance of CommandContext
     * @throws CommandLineException  in case something went wrong
     */
    void handle(CommandContext ctx) throws CommandLineException;

    /**
     * Represents registration of the command line redirection
     */
    interface Registration {

        /**
         * Checks whether the registration is still active.
         * The method will return false after the unregister
         * method has been called on the instance.
         *
         * @return  true if the registration is still active,
         * false if the unregister method has been called on the instance
         */
        boolean isActive();

        /**
         * Redirection can be stopped by invoking this method.
         *
         * @throws CommandLineException  in case unregistration failed
         */
        void unregister() throws CommandLineException;

        /**
         * Allows to execute the parsed command line as it normally
         * would be executed w/o the redirection in effect.
         *
         * @param parsedLine  the parsed command line
         * @throws CommandLineException  in case the line failed to execute
         */
        void handle(ParsedCommandLine parsedLine) throws CommandLineException;
    }
}
