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

import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.ParsedArguments;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentWithValue extends ArgumentWithoutValue {

    private final boolean required;
    private final CommandLineCompleter valueCompleter;

    public ArgumentWithValue(String fullName) {
        this(false, -1, fullName, null);
    }

    public ArgumentWithValue(String fullName, String shortName) {
        this(false, -1, fullName, shortName);
    }

    public ArgumentWithValue(CommandLineCompleter valueCompleter, String fullName) {
        this(false, valueCompleter, -1, fullName, null);
    }

    public ArgumentWithValue(CommandLineCompleter valueCompleter, String fullName, String shortName) {
        this(false, valueCompleter, -1, fullName, shortName);
    }

    public ArgumentWithValue(boolean required, CommandLineCompleter valueCompleter, String fullName) {
        this(required, valueCompleter, -1, fullName, null);
    }

    public ArgumentWithValue(boolean required, CommandLineCompleter valueCompleter, String fullName, String shortName) {
        this(required, valueCompleter, -1, fullName, shortName);
    }

    public ArgumentWithValue(boolean required, String fullName) {
        this(required, -1, fullName, null);
    }

    public ArgumentWithValue(boolean required, String fullName, String shortName) {
        this(required, -1, fullName, shortName);
    }

    public ArgumentWithValue(int index, String fullName) {
        this(false, index, fullName, null);
    }

    public ArgumentWithValue(boolean required, int index, String fullName, String shortName) {
        this(required, null, index, fullName, shortName);
    }

    public ArgumentWithValue(boolean required, CommandLineCompleter valueCompleter, int index, String fullName) {
        this(required, valueCompleter, index, fullName, null);
    }

    public ArgumentWithValue(boolean required, CommandLineCompleter valueCompleter, int index, String fullName, String shortName) {
        super(index, fullName, shortName);
        this.required = required;
        this.valueCompleter = valueCompleter;
    }

    @Override
    public CommandLineCompleter getValueCompleter() {
        return valueCompleter;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandArgument#getValue(org.jboss.as.cli.CommandContext)
     */
    @Override
    public String getValue(ParsedArguments args) {

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
            throw new IllegalArgumentException(buf.toString());
        }
        return value;
    }

    @Override
    public boolean isValueRequired() {
        return true;
    }
}
