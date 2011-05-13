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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.parsing.CommandLineParser;


/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleArgumentTabCompleter implements CommandLineCompleter {

    private final List<CommandArgument> allArgs = new ArrayList<CommandArgument>();

    private final ParsingResults results = new ParsingResults();

    public void addArgument(CommandArgument arg) {
        allArgs.add(arg);
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.CommandLineCompleter#complete(org.jboss.as.cli.CommandContext, java.lang.String, int, java.util.List)
     */
    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

        int firstCharIndex = 0;
        while(firstCharIndex < buffer.length()) {
            if(!Character.isWhitespace(buffer.charAt(firstCharIndex))) {
                break;
            }
            ++firstCharIndex;
        }

        int result = buffer.length();
        String chunk = null;
        CommandLineCompleter valueCompleter = null;
        if (firstCharIndex != result) {
            results.reset();
            try {
                CommandLineParser.parse(buffer, new CommandLineParser.CallbackHandler() {
                    @Override
                    public void argument(String name, int nameStart, String value, int valueStart, int end) {
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

            if(results.argValue != null) {
                if(results.argValue.isEmpty()) {
                    chunk = null;
                    result = results.valueStart;

                    if(results.argName != null) {
                        ctx.setArgumentsString(buffer.substring(0, results.nameStart));
                        for(CommandArgument arg : allArgs) {
                            if(results.argName.equals(arg.getDefaultName())) {
                                valueCompleter = arg.getValueCompleter();
                                break;
                            }
                        }
                    } else {
                        ctx.setArgumentsString(buffer.substring(0, results.valueStart));
                        for (CommandArgument arg : allArgs) {
                            if (arg.getIndex() >= 0 && arg.canAppearNext(ctx)) {
                                valueCompleter = arg.getValueCompleter();
                                break;
                            }
                        }
                    }

                    if(valueCompleter == null) {
                        return -1;
                    }
                } else {
                    if(results.endIndex < buffer.length()) {
                        chunk = null;
                    } else {
                        chunk = results.argValue;
                        result = results.valueStart;

                        if(results.argName != null) {
                            ctx.setArgumentsString(buffer.substring(0, results.nameStart));
                            for(CommandArgument arg : allArgs) {
                                if(results.argName.equals(arg.getDefaultName())) {
                                    valueCompleter = arg.getValueCompleter();
                                    break;
                                }
                            }
                        } else {
                            ctx.setArgumentsString(buffer.substring(0, results.valueStart));
                            for (CommandArgument arg : allArgs) {
                                if (arg.getIndex() >= 0 && arg.canAppearNext(ctx)) {
                                    valueCompleter = arg.getValueCompleter();
                                    break;
                                }
                            }
                        }

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
            int valueResult = valueCompleter.complete(ctx, chunk == null ? "" : chunk, 0, candidates);
            if(valueResult < 0) {
                return valueResult;
            } else {
                return result + valueResult;
            }
        }

        int charLength = buffer.length() - firstCharIndex;
        if(charLength == 1 && buffer.charAt(firstCharIndex) == '-' ||
                charLength == 2 && '-' == buffer.charAt(firstCharIndex) && '-' == buffer.charAt(firstCharIndex + 1)) {
            //parsedArgs.parse("");
            ctx.setArgumentsString("");
        } else {
            //parsedArgs.parse(buffer.substring(firstCharIndex, result));
            ctx.setArgumentsString(buffer.substring(firstCharIndex, result));
        }

        for(CommandArgument arg : allArgs) {
            if(arg.canAppearNext(ctx)) {
                if(arg.getIndex() >= 0) {
                    CommandLineCompleter valCompl = arg.getValueCompleter();
                    if(valCompl != null) {
                        valCompl.complete(ctx, chunk == null ? "" : chunk, cursor, candidates);
                    }
                } else {
                    String argName = arg.getDefaultName();
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
        }

        Collections.sort(candidates);
        return result;
    }

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
