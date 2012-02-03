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

import java.util.Collection;


/**
 *
 * @author Alexey Loubyansky
 */
public interface CommandHandler {

    /**
     * Checks whether the command is available in the current context
     * (e.g. some commands require connection with the controller,
     * some are available only in the batch mode, etc)
     * @param ctx  current context
     * @return  true if the command can be executed in the current context, false - otherwise.
     */
    boolean isAvailable(CommandContext ctx);

    /**
     * Whether the command supports batch mode or not.
     * The result could depend on the context, e.g. it won't make sense
     * to add 'some_command --help' to a batch.
     *
     * @param ctx  the current context
     * @return  true if the command can be added to the batch, otherwise - false.
     */
    boolean isBatchMode(CommandContext ctx);

    /**
     * Handles the execution of the command.
     * @param ctx  current command context
     */
    void handle(CommandContext ctx) throws CommandFormatException;

    /**
     * Checks whether the command handler recognizes the argument by the name.
     * @param name  argument name to check
     * @return  true if the handler recognizes the argument, otherwise - false.
     */
    boolean hasArgument(String name);

    /**
     * Checks whether the command handler accepts an argument with the specified index.
     * @param index  argument index to check
     * @return  true if the handler accepts an argument with the specified index, otherwise - false.
     */
    boolean hasArgument(int index);

    Collection<CommandArgument> getArguments(CommandContext ctx);
}
