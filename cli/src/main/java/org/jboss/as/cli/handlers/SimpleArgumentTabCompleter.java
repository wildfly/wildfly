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


/**
 *
 * @author Alexey Loubyansky
 */
public class SimpleArgumentTabCompleter implements CommandLineCompleter {

    private final List<CommandArgument> allArgs = new ArrayList<CommandArgument>();

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
            if (Character.isWhitespace(buffer.charAt(buffer.length() - 1))) {

                int lastNonWS = buffer.length() - 1;
                while(lastNonWS > 0 && Character.isWhitespace(buffer.charAt(lastNonWS))) {
                    --lastNonWS;
                }

                if(lastNonWS > 0 && buffer.charAt(lastNonWS) == '=') {
                    // value completion
                    String argName = buffer.substring(firstCharIndex, lastNonWS - 1);
                    for(CommandArgument arg : allArgs) {
                        if(argName.equals(arg.getDefaultName())) {
                            valueCompleter = arg.getValueCompleter();
                            if(valueCompleter == null) {
                                return -1;
                            }
                            break;
                        }
                    }
                }
            } else {
                int i = buffer.length() - 1;
                while (i >= 0) {
                    char ch = buffer.charAt(i);
                    if(Character.isWhitespace(ch)) {
                        break;
                    }
                    if(ch == '=') {
                        result = i + 1;
                        chunk = buffer.substring(result);
                    }
                    --i;
                }

                if(chunk == null) {
                    result = i + 1;
                    chunk = buffer.substring(result);

                    if(buffer.charAt(result) != '-') {
                        // it's an argument with optional name
                        ctx.setArgumentsString(buffer.substring(firstCharIndex, result));

                        for(CommandArgument arg : allArgs) {
                            if(arg.getIndex() >= 0 && arg.canAppearNext(ctx)) {
                                valueCompleter = arg.getValueCompleter();
                                break;
                            }
                        }
                        if(valueCompleter == null) {
                            return -1;
                        }
                    }
                } else {
                    // value completion
                    if(buffer.charAt(i + 1) != '-') {
                        result = i + 1;
                        chunk = buffer.substring(result);
                        // it's an argument with optional name
                        ctx.setArgumentsString(buffer.substring(firstCharIndex, result));

                        for (CommandArgument arg : allArgs) {
                            if (arg.getIndex() >= 0 && arg.canAppearNext(ctx)) {
                                valueCompleter = arg.getValueCompleter();
                                break;
                            }
                        }
                    } else {
                        String argName = buffer.substring(i + 1, result - 1);
                        for (CommandArgument arg : allArgs) {
                            if (argName.equals(arg.getDefaultName())) {
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
}
