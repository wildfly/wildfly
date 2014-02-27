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

import org.jboss.as.cli.ArgumentValueConverter;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.handlers.CommandHandlerWithArguments;
import org.jboss.as.cli.operation.ParsedCommandLine;
import org.jboss.as.cli.parsing.ExpressionBaseState;
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.ParsingStateCallbackHandler;
import org.jboss.as.cli.parsing.StateParser;
import org.jboss.as.cli.parsing.WordCharacterHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentWithValue extends ArgumentWithoutValue {

    private final CommandLineCompleter valueCompleter;
    private final ArgumentValueConverter valueConverter;

    public ArgumentWithValue(CommandHandlerWithArguments handler, String fullName) {
        this(handler, null, ArgumentValueConverter.DEFAULT, fullName, null);
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, CommandLineCompleter valueCompleter, String fullName) {
        this(handler, valueCompleter, ArgumentValueConverter.DEFAULT, fullName, null);
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, CommandLineCompleter valueCompleter,
            ArgumentValueConverter valueConverter, String fullName) {
        this(handler, valueCompleter, valueConverter, fullName, null);
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, int index, String fullName) {
        this(handler, null, index, fullName);
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, CommandLineCompleter valueCompleter, int index, String fullName) {
        super(handler, index, fullName);
        this.valueCompleter = valueCompleter;
        valueConverter = ArgumentValueConverter.DEFAULT;
    }

    public ArgumentWithValue(CommandHandlerWithArguments handler, CommandLineCompleter valueCompleter,
            ArgumentValueConverter valueConverter, String fullName, String shortName) {
        super(handler, fullName, shortName);
        this.valueCompleter = valueCompleter;
        this.valueConverter = valueConverter;
    }

    public CommandLineCompleter getValueCompleter() {
        return valueCompleter;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandArgument#getValue(org.jboss.as.cli.CommandContext)
     */
    @Override
    public String getValue(ParsedCommandLine args, boolean required) throws CommandFormatException {
        return getResolvedValue(args, required);
    }

    /**
     * Calls getOriginalValue(ParsedCommandLine parsedLine, boolean required) and correctly
     * handles escape sequences and resolves system properties.
     *
     * @param parsedLine  parsed command line
     * @param required  whether the argument is required
     * @return  resolved argument value
     * @throws CommandFormatException  in case the required argument is missing
     */
    public String getResolvedValue(ParsedCommandLine parsedLine, boolean required) throws CommandFormatException {
        final String value = getOriginalValue(parsedLine, required);
        return resolveValue(value);
    }

    public static String resolveValue(final String value) throws CommandFormatException {
        if(value == null) {
            return null;
        }

        final StringBuilder buf = new StringBuilder();
        final ExpressionBaseState state = new ExpressionBaseState("EXPR", true, false);
        state.setDefaultHandler(WordCharacterHandler.IGNORE_LB_ESCAPE_ON);
        StateParser.parse(value, new ParsingStateCallbackHandler(){
            @Override
            public void enteredState(ParsingContext ctx) throws CommandFormatException {
            }

            @Override
            public void leavingState(ParsingContext ctx) throws CommandFormatException {
            }

            @Override
            public void character(ParsingContext ctx) throws CommandFormatException {
                buf.append(ctx.getCharacter());
            }}, state);
        return buf.toString();
    }

    /**
     * Returns value as it appeared on the command line with escape sequences
     * and system properties not resolved. The variables, though, are resolved
     * during the initial parsing of the command line.
     *
     * @param parsedLine  parsed command line
     * @param required  whether the argument is required
     * @return  argument value as it appears on the command line
     * @throws CommandFormatException  in case the required argument is missing
     */
    public String getOriginalValue(ParsedCommandLine parsedLine, boolean required) throws CommandFormatException {
        String value = null;
        if(parsedLine.hasProperties()) {
            if(index >= 0) {
                List<String> others = parsedLine.getOtherProperties();
                if(others.size() > index) {
                    return others.get(index);
                }
            }

            value = parsedLine.getPropertyValue(fullName);
            if(value == null && shortName != null) {
                value = parsedLine.getPropertyValue(shortName);
            }
        }

        if(required && value == null && !isPresent(parsedLine)) {
            StringBuilder buf = new StringBuilder();
            buf.append("Required argument ");
            buf.append('\'').append(fullName).append('\'');
            buf.append(" is missing.");
            throw new CommandFormatException(buf.toString());
        }
        return value;
    }

    public ModelNode toModelNode(CommandContext ctx) throws CommandFormatException {
        final ParsedCommandLine parsedLine = ctx.getParsedCommandLine();
        final String value = getOriginalValue(parsedLine, false);
        if(value == null) {
            return null;
        }
        return valueConverter.fromString(ctx, value);
    }

    @Override
    public boolean isValueRequired() {
        return true;
    }

    @Override
    public boolean isValueComplete(ParsedCommandLine args) throws CommandFormatException {

        if(!isPresent(args)) {
            return false;
        }

        if (index >= 0) {
            final int size = args.getOtherProperties().size();
            if(index >= size) {
                return false;
            }
            if(index < size -1) {
                return true;
            }
            return !args.getOtherProperties().get(index).equals(args.getLastParsedPropertyValue());
        }

        if(fullName.equals(args.getLastParsedPropertyName())) {
            return false;
        }

        if(shortName != null && shortName.equals(args.getLastParsedPropertyName())) {
            return false;
        }
        return true;
    }

    public ArgumentValueConverter getValueConverter() {
        return valueConverter;
    }
}
