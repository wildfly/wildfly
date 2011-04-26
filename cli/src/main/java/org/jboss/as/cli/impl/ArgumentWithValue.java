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

    public ArgumentWithValue(String... names) {
        this(false, -1, names);
    }

    public ArgumentWithValue(CommandLineCompleter valueCompleter, String... names) {
        this(false, valueCompleter, -1, names);
    }

    public ArgumentWithValue(boolean required, CommandLineCompleter valueCompleter, String... names) {
        this(required, valueCompleter, -1, names);
    }

    public ArgumentWithValue(boolean required, String... names) {
        this(required, -1, names);
    }

    public ArgumentWithValue(int index, String defaultName) {
        this(false, index, defaultName);
    }

    public ArgumentWithValue(boolean required, int index, String... names) {
        this(required, null, index, names);
    }

    public ArgumentWithValue(boolean required, CommandLineCompleter valueCompleter, int index, String... names) {
        super(index, names);
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
                    value = others.get(index);
                }
            }

            if(names != null) {
                if(names.length == 1) {
                    if(value == null) {
                        value = args.getArgument(names[0]);
                    } else {
                        String namedValue = args.getArgument(names[0]);
                        if(namedValue != null && !namedValue.equals(value)) {
                            throw new IllegalArgumentException("Argument " + defaultName + " is specified twice: '" +
                                    value + "' vs '" + namedValue + "'.");
                        }
                    }
                } else {
                    for(String name : names) {
                        if(value == null) {
                            value = args.getArgument(name);
                        } else {
                            String namedValue = args.getArgument(name);
                            if(namedValue != null && !namedValue.equals(value)) {
                                throw new IllegalArgumentException("Argument " + defaultName + " is specified twice: '" +
                                        value + "' vs '" + namedValue + "'.");
                            }
                        }

                    }
                }
            }
        }

        if(required && value == null && !isPresent(args)) {
            StringBuilder buf = new StringBuilder();
            buf.append("Required argument ");
            if(names != null) {
                buf.append('\'').append(names[0]).append('\'');
            } else {
                buf.append("with index ").append(index);
            }
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
