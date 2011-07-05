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
package org.jboss.as.cli.handlers;

import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.DefaultParsedArguments;
import org.jboss.as.cli.parsing.CommandLineParser;

/**
 *
 * @author Alexey Loubyansky
 */
public abstract class BaseArgumentTabCompleter implements CommandLineCompleter {

    private final ParsingResults results = new ParsingResults();

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandLineCompleter#complete(org.jboss.as.cli.CommandContext, java.lang.String, int, java.util.List)
     */
    @Override
    public int complete(CommandContext ctx, final String buffer, int cursor, List<String> candidates) {

        int firstCharIndex = 0;
        while(firstCharIndex < buffer.length()) {
            if(!Character.isWhitespace(buffer.charAt(firstCharIndex))) {
                break;
            }
            ++firstCharIndex;
        }

        results.reset();
        final DefaultParsedArguments parsedArguments = (DefaultParsedArguments) ctx.getParsedArguments();
        parsedArguments.reset(null, null);
        try {
            CommandLineParser.parse(buffer, new CommandLineParser.CallbackHandler() {
                @Override
                public void argument(String name, int nameStart, String value, int valueStart, int end) throws CommandFormatException {
                    if(end > 0 && end < buffer.length()) {
                        parsedArguments.argument(name, nameStart, value, valueStart, end);
                    }
                    results.argName = name;
                    results.argValue = value;
                    results.nameStart = nameStart;
                    results.valueStart = valueStart;
                    results.endIndex = end;
                }
            });
        } catch (CommandLineException e) {
            return -1;
        }

        int result = buffer.length();
        String chunk = null;
        CommandLineCompleter valueCompleter = null;
        if (firstCharIndex != result) {

            if(results.argValue != null) {
                if(results.argValue.isEmpty()) {
                    chunk = null;
                    result = results.valueStart;
                    valueCompleter = getValueCompleter(ctx, results.argName);
                    if(valueCompleter == null) {
                        return -1;
                    }
                } else {
                    if(results.endIndex < buffer.length()) {
                        chunk = null;
                    } else {
                        chunk = results.argValue;
                        result = results.valueStart;
                        valueCompleter = getValueCompleter(ctx, results.argName);
                        if(valueCompleter == null) {
                            return -1;
                        }
                    }
                }
            } else {
                if(results.endIndex < buffer.length()) {
                    chunk = null;
                } else {
                    chunk = results.argName;
                    if (results.argName != null) {
                        result = results.nameStart;
                    }
                }
            }
        }

        if(valueCompleter != null) {
            int valueResult = valueCompleter.complete(ctx, chunk == null ? "" : chunk, cursor, candidates);
            if(valueResult < 0) {
                return valueResult;
            } else {
                return result + valueResult;
            }
        }

        for(CommandArgument arg : getAllArguments(ctx)) {
            try {
                if(arg.canAppearNext(ctx)) {
                    if(arg.getIndex() >= 0) {
                        CommandLineCompleter valCompl = arg.getValueCompleter();
                        if(valCompl != null) {
                            valCompl.complete(ctx, chunk == null ? "" : chunk, cursor, candidates);
                        }
                    } else {
                        String argName = arg.getFullName();
                        if (chunk == null) {
                            if (arg.isValueRequired()) {
                                argName += '=';
                            }
                            candidates.add(argName);
                        } else if (argName.startsWith(chunk)) {
                            if (arg.isValueRequired()) {
                                argName += '=';
                            }
                            candidates.add(argName);
                        }
                    }
                }
            } catch (CommandFormatException e) {
                return -1;
            }
        }

        Collections.sort(candidates);
        return result;
    }

    protected CommandLineCompleter getValueCompleter(CommandContext ctx, final String argName) {
        if(argName != null) {
            for(CommandArgument arg : getAllArguments(ctx)) {
                if(argName.equals(arg.getFullName())) {
                    return arg.getValueCompleter();
                }
            }
            return null;
        }

        for (CommandArgument arg : getAllArguments(ctx)) {
            try {
                if (arg.getIndex() >= 0 && arg.canAppearNext(ctx)) {
                    return arg.getValueCompleter();
                }
            } catch (CommandFormatException e) {
                return null;
            }
        }
        return null;
    }

    protected abstract Iterable<CommandArgument> getAllArguments(CommandContext ctx);

    private static final class ParsingResults {
        String argName;
        String argValue;
        int nameStart;
        int valueStart;
        int endIndex;

        void reset() {
            argName = null;
            argValue = null;
            nameStart = -1;
            valueStart = -1;
            endIndex = -1;
        }
    }
}
