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
import org.jboss.as.cli.operation.ParsedCommandLine;


/**
 *
 * @author Alexey Loubyansky
 */
public abstract class BaseArgumentTabCompleter implements CommandLineCompleter {

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

        final ParsedCommandLine parsedCmd = ctx.getParsedCommandLine();

        Iterable<CommandArgument> allArgs = getAllArguments(ctx);
        if(!allArgs.iterator().hasNext()) {
            return -1;
        }

        try {
            if(!parsedCmd.hasProperties()) {
                for(CommandArgument arg : getAllArguments(ctx)) {
                    if(arg.canAppearNext(ctx)) {
                        if(arg.getIndex() >= 0) {
                            final CommandLineCompleter valCompl = arg.getValueCompleter();
                            if(valCompl != null) {
                                valCompl.complete(ctx, "", cursor, candidates);
                            }
                        } else {
                            String argName = arg.getFullName();
                            if (arg.isValueRequired()) {
                                argName += '=';
                            }
                            candidates.add(argName);
                        }
                    }
                }
                return cursor + buffer.length();
            }
        } catch (CommandFormatException e) {
            return -1;
        }

        int result = cursor + buffer.length();

        String chunk = null;
        CommandLineCompleter valueCompleter = null;
        if (!parsedCmd.endsOnPropertySeparator()) {
            final String argName = parsedCmd.getLastParsedPropertyName();
            final String argValue = parsedCmd.getLastParsedPropertyValue();
            if (argValue != null || parsedCmd.endsOnPropertyValueSeparator()) {
                result = parsedCmd.getLastChunkIndex();//.getLastSeparatorIndex() + 1;
                if(parsedCmd.endsOnPropertyValueSeparator()) {
                    ++result;// it enters on '='
                }
                chunk = argValue;
                if(argName != null) {
                    valueCompleter = getValueCompleter(ctx, argName);
                } else {
                    valueCompleter = getValueCompleter(ctx, parsedCmd.getOtherProperties().size() - 1);
                }

                if(valueCompleter == null) {
                    return -1;
                }
            } else {
                chunk = argName;
                if(firstCharIndex == buffer.length()) {
                    result = cursor + firstCharIndex;
                } else {
                    result = parsedCmd.getLastChunkIndex();
                }
            }
        } else {
            if(parsedCmd.getLastParsedPropertyValue() != null) {
                chunk = parsedCmd.getLastParsedPropertyValue();
            } else {
                chunk = parsedCmd.getLastParsedPropertyName();
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
                        String argFullName = arg.getFullName();
                        if (chunk == null) {
                            if (arg.isValueRequired()) {
                                argFullName += '=';
                            }
                            candidates.add(argFullName);
                        } else if (argFullName.startsWith(chunk)) {
                            if (arg.isValueRequired()) {
                                argFullName += '=';
                            }
                            candidates.add(argFullName);
                        }
                    }
                }
            } catch (CommandFormatException e) {
                e.printStackTrace();
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

    protected CommandLineCompleter getValueCompleter(CommandContext ctx, int index) {
        for (CommandArgument arg : getAllArguments(ctx)) {
            if (arg.getIndex() == index) {
                return arg.getValueCompleter();
            }
        }
        return null;
    }

    protected abstract Iterable<CommandArgument> getAllArguments(CommandContext ctx);
}
