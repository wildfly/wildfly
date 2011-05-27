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

import java.util.List;
import java.util.Set;

/**
 *
 * @author Alexey Loubyansky
 */
public interface ParsedArguments {

    /**
     * Returns the current command's arguments as a string.
     * @return current command's arguments as a string or null if the command was entered w/o arguments.
     */
    String getArgumentsString();

    /**
     * Checks whether there are arguments on the command line for the current command.
     * @return true if there are arguments, false if there aren't.
     * @throws CommandLineException
     */
    boolean hasArguments() throws CommandFormatException;

    /**
     * Checks whether the named argument is present among the command arguments.
     * @return
     * @throws CommandLineException
     */
    boolean hasArgument(String argName) throws CommandFormatException;

    /**
     * Returns a value for the named argument on the command line or
     * null if the argument with the name isn't present.
     * @param argName  the name of the argument
     * @return  the value of the argument or null if the argument isn't present
     * @throws CommandLineException
     */
    String getArgument(String argName) throws CommandFormatException;

    /**
     * Returns a set of argument names present on the command line
     * of an empty set if there no named arguments on the command line.
     *
     * @return  a set of argument names present on the command line
     * of an empty set if there no named arguments on the command line
     * @throws CommandLineException
     */
    Set<String> getArgumentNames() throws CommandFormatException;

    /**
     * Returns arguments that are not switches as a list of strings
     * in the order they appear on the command line. If there no such arguments
     * an empty list is returned.
     * @return a list of arguments that are not switches or an empty list
     * if there are no such arguments.
     * @throws CommandLineException
     */
    List<String> getOtherArguments() throws CommandFormatException;
}
