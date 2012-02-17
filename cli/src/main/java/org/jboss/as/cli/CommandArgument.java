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
package org.jboss.as.cli;

import org.jboss.as.cli.operation.ParsedCommandLine;


/**
 *
 * @author Alexey Loubyansky
 */
public interface CommandArgument {

    /**
     * The default name of the argument.
     * An argument can have more than one name, e.g. --force and -f.
     * Full name can't be null.
     * @return  the default name of the argument.
     */
    String getFullName();

    /**
     * Short name of the argument if exists.
     * @return short name of the argument or null if the short name doesn't exist.
     */
    String getShortName();

    /**
     * If the argument doesn't have a name its value can be found by index.
     * Indexes start with 0.
     * A command could have both a name and an index. In that case, the name is optional.
     * If the command doesn't have a fixed index, the method will return -1.
     * @return  the index of the argument.
     */
    int getIndex();

    /**
     * Checks whether the argument is present on the command line.
     * @param args  parsed arguments
     * @return  true if the argument is present, false - otherwise.
     * @throws CommandFormatException
     */
    boolean isPresent(ParsedCommandLine args) throws CommandFormatException;

    /**
     * Checks whether the argument can appear on the command
     * given the already present arguments.
     * (Used for tab-completion. Although, often isValueComplete(ParsedOperationRequest req) would be more appropriate.)
     * @param ctx
     * @return true if the argument can appear on the command line next, false - otherwise.
     */
    boolean canAppearNext(CommandContext ctx) throws CommandFormatException;

    /**
     * Returns the value of the argument specified on the command line.
     * If the argument isn't specified the returned value is null.
     * Although, it might throw IllegalArgumentException in case the argument is a required one.
     * @param args  parsed arguments.
     * @return  the value of the argument or null if the argument isn't present or is missing value.
     */
    String getValue(ParsedCommandLine args) throws CommandFormatException;

    /**
     * Returns the value of the argument specified on the command line.
     * If the argument isn't specified and the value is not required null is returned.
     * Otherwise an exception is thrown.
     *
     * @param args  parsed arguments.
     * @param required  whether the value for this argument is required.
     * @return  the value of the argument or null if the argument isn't present and the value is not required.
     */
    String getValue(ParsedCommandLine args, boolean required) throws CommandFormatException;

    /**
     * Checks whether the value is specified and complete.
     * The value is considered complete only if it is followed by a separator
     * (argument separator, end of argument list but not end-of-content).
     *
     * @param args  the parsed arguments
     * @return  true if the value of the argument is complete, false otherwise.
     * @throws CommandFormatException
     */
    boolean isValueComplete(ParsedCommandLine args) throws CommandFormatException;

    /**
     * Checks whether the argument accepts value.
     * @return  true if this argument accepts a value, otherwise false.
     */
    boolean isValueRequired();

    /**
     * Returns the tab-completer for the value.
     * @return  tab-completer for the value or null of none available.
     */
    CommandLineCompleter getValueCompleter();
}
