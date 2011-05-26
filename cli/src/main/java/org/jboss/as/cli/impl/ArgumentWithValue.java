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
package org.jboss.as.cli.impl;

import java.util.List;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.ParsedArguments;
import org.jboss.as.cli.handlers.CommandHandlerWithArguments;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentWithValue extends ArgumentWithoutValue {

    private final CommandLineCompleter valueCompleter;

    public ArgumentWithValue(CommandHandlerWithArguments handler, String fullName) {
        this(handler, fullName, null);
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, CommandLineCompleter valueCompleter, String fullName) {
        this(handler, valueCompleter, fullName, null);
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, String fullName, String shortName) {
        this(handler, null, fullName, shortName);
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, int index, String fullName) {
        this(handler, null, index, fullName);
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, CommandLineCompleter valueCompleter, int index, String fullName) {
        super(handler, index, fullName);
        this.valueCompleter = valueCompleter;
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, CommandLineCompleter valueCompleter, String fullName, String shortName) {
        super(handler, fullName, shortName);
        this.valueCompleter = valueCompleter;
    }

    public CommandLineCompleter getValueCompleter() {
        return valueCompleter;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandArgument#getValue(org.jboss.as.cli.CommandContext)
     */
    @Override
    public String getValue(ParsedArguments args, boolean required) throws CommandFormatException {

        String value = null;
        if(args.hasArguments()) {
            if(index >= 0) {
                List<String> others = args.getOtherArguments();
                if(others.size() > index) {
                    return others.get(index);
                }
            }

            value = args.getArgument(fullName);
            if(value == null && shortName != null) {
                value = args.getArgument(shortName);
            }
        }

        if(required && value == null && !isPresent(args)) {
            StringBuilder buf = new StringBuilder();
            buf.append("Required argument ");
            buf.append('\'').append(fullName).append('\'');
            buf.append(" is missing.");
            throw new CommandFormatException(buf.toString());
        }
        return value;
    }

    @Override
    public boolean isValueRequired() {
        return true;
    }
}
