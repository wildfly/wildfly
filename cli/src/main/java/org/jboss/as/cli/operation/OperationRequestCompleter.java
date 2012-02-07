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
package org.jboss.as.cli.operation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.cli.CommandArgument;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.CommandLineFormat;
import org.jboss.as.cli.EscapeSelector;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;


/**
 *
 * @author Alexey Loubyansky
 */
public class OperationRequestCompleter implements CommandLineCompleter {

    public static final OperationRequestCompleter INSTANCE = new OperationRequestCompleter();

    public static final CommandLineCompleter ARG_VALUE_COMPLETER = new CommandLineCompleter(){
        final DefaultCallbackHandler parsedOp = new DefaultCallbackHandler();
        @Override
        public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {
            try {
                parsedOp.parseOperation(ctx.getPrefix(), buffer);
            } catch (CommandFormatException e) {
                return -1;
            }
            return INSTANCE.complete(ctx, parsedOp, buffer, cursor, candidates);
        }};

    public static final EscapeSelector ESCAPE_SELECTOR = new EscapeSelector() {
        @Override
        public boolean isEscape(char ch) {
            return ch == ':' || ch == '/' || ch == '=' || ch == ' ' || ch == '"' || ch == '\\';
        }
    };

    @Override
    public int complete(CommandContext ctx, final String buffer, int cursor, List<String> candidates) {
        return complete(ctx, ctx.getParsedCommandLine(), buffer, cursor, candidates);
    }

    public int complete(CommandContext ctx, ParsedCommandLine parsedCmd, final String buffer, int cursor, List<String> candidates) {
        return complete(ctx, parsedCmd, ctx.getOperationCandidatesProvider(), buffer, cursor, candidates);
    }

    public int complete(CommandContext ctx, ParsedCommandLine parsedCmd, OperationCandidatesProvider candidatesProvider, final String buffer, int cursor, List<String> candidates) {

        if(parsedCmd.isRequestComplete()) {
            return -1;
        }

        if(parsedCmd.endsOnHeaderListStart() || parsedCmd.hasHeaders()) {
            final Map<String, OperationRequestHeader> headers = candidatesProvider.getHeaders(ctx);
            if(headers.isEmpty()) {
                return -1;
            }
            int result = buffer.length();
            if(parsedCmd.getLastHeaderName() != null) {
                if(buffer.endsWith(parsedCmd.getLastHeaderName())) {
                    result = parsedCmd.getLastChunkIndex();
                    for(String name : headers.keySet()) {
                        if(!parsedCmd.hasHeader(name) && name.startsWith(parsedCmd.getLastHeaderName())) {
                            candidates.add(name);
                        }
                    }
                } else {
                    final OperationRequestHeader header = headers.get(parsedCmd.getLastHeaderName());
                    if(header == null) {
                        return -1;
                    }
                    final CommandLineCompleter headerCompleter = header.getCompleter();
                    if(headerCompleter == null) {
                        return -1;
                    }

                    int valueResult = headerCompleter.complete(ctx, buffer.substring(parsedCmd.getLastChunkIndex()), cursor, candidates);
                    if(valueResult < 0) {
                        return -1;
                    }
                    result = parsedCmd.getLastChunkIndex() + valueResult;
                }
            } else {
                if(!parsedCmd.hasHeaders()) {
                    candidates.addAll(headers.keySet());
                } else if(parsedCmd.endsOnHeaderSeparator()) {
                    candidates.addAll(headers.keySet());
                    for(ParsedOperationRequestHeader parsed : parsedCmd.getHeaders()) {
                        candidates.remove(parsed.getName());
                    }
                } else {
                    final ParsedOperationRequestHeader lastParsedHeader = parsedCmd.getLastHeader();
                    final OperationRequestHeader lastHeader = headers.get(lastParsedHeader.getName());
                    if(lastHeader == null) {
                        return -1;
                    }
                    final CommandLineCompleter headerCompleter = lastHeader.getCompleter();
                    if(headerCompleter == null) {
                        return -1;
                    }
                    result = headerCompleter.complete(ctx, buffer, cursor, candidates);
                }
            }
            Collections.sort(candidates);
            return result;
        }

        if(parsedCmd.endsOnPropertyListEnd()) {
            return buffer.length();
        }

        if (parsedCmd.hasProperties() || parsedCmd.endsOnPropertyListStart()) {

            final Collection<CommandArgument> allArgs = candidatesProvider.getProperties(ctx, parsedCmd.getOperationName(), parsedCmd.getAddress());
            if (allArgs.isEmpty()) {
                final CommandLineFormat format = parsedCmd.getFormat();
                if(format != null && format.getPropertyListEnd() != null) {
                    candidates.add(format.getPropertyListEnd());
                }
                return buffer.length();
            }

            try {
                if (!parsedCmd.hasProperties()) {
                    for (CommandArgument arg : allArgs) {
                        if (arg.canAppearNext(ctx)) {
                            if (arg.getIndex() >= 0) {
                                final CommandLineCompleter valCompl = arg.getValueCompleter();
                                if (valCompl != null) {
                                    valCompl.complete(ctx, "", 0, candidates);
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
                    Collections.sort(candidates);
                    return buffer.length();
                }
            } catch (CommandFormatException e) {
                return -1;
            }

            int result = buffer.length();

            String chunk = null;
            CommandLineCompleter valueCompleter = null;
            if (!parsedCmd.endsOnPropertySeparator()) {
                final String argName = parsedCmd.getLastParsedPropertyName();
                final String argValue = parsedCmd.getLastParsedPropertyValue();
                if (argValue != null || parsedCmd.endsOnPropertyValueSeparator()) {
                    result = parsedCmd.getLastChunkIndex();
                    if (parsedCmd.endsOnPropertyValueSeparator()) {
                        ++result;// it enters on '='
                    }
                    chunk = argValue;
                    if (argName != null) {
                        valueCompleter = getValueCompleter(ctx, allArgs, argName);
                    } else {
                        valueCompleter = getValueCompleter(ctx, allArgs, parsedCmd.getOtherProperties().size() - 1);
                    }
                    if (valueCompleter == null) {
                        if (parsedCmd.endsOnSeparator()) {
                            return -1;
                        }
                        for (CommandArgument arg : allArgs) {
                            try {
                                if (arg.canAppearNext(ctx) && !arg.getFullName().equals(argName)) {
                                    return -1;
                                }
                            } catch (CommandFormatException e) {
                                break;
                            }
                        }
                        final CommandLineFormat format = parsedCmd.getFormat();
                        if(format != null && format.getPropertyListEnd() != null) {
                            candidates.add(format.getPropertyListEnd());
                        }
                        return buffer.length();
                    }
                } else {
                    chunk = argName;
                    result = parsedCmd.getLastChunkIndex();
                }
            } else {
                chunk = null;
            }

            if (valueCompleter != null) {
                int valueResult = valueCompleter.complete(ctx, chunk == null ? "" : chunk, 0, candidates);
                if (valueResult < 0) {
                    return valueResult;
                } else {
                    return result + valueResult;
                }
            }

            for (CommandArgument arg : allArgs) {
                try {
                    if (arg.canAppearNext(ctx)) {
                        if (arg.getIndex() >= 0) {
                            CommandLineCompleter valCompl = arg.getValueCompleter();
                            if (valCompl != null) {
                                final String value = chunk == null ? "" : chunk;
                                valCompl.complete(ctx, value, value.length(), candidates);
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

            if (candidates.isEmpty()) {
                if (chunk == null && !parsedCmd.endsOnSeparator()) {
                    final CommandLineFormat format = parsedCmd.getFormat();
                    if(format != null && format.getPropertyListEnd() != null) {
                        candidates.add(format.getPropertyListEnd());
                    }
                }
            } else {
                Collections.sort(candidates);
            }
            return result;
        }

        if(parsedCmd.hasOperationName() || parsedCmd.endsOnAddressOperationNameSeparator()) {

            if(parsedCmd.getAddress().endsOnType()) {
                return -1;
            }
            final Collection<String> names = candidatesProvider.getOperationNames(ctx, parsedCmd.getAddress());
            if(names.isEmpty()) {
                return -1;
            }

            final String chunk = parsedCmd.getOperationName();
            if(chunk == null) {
                candidates.addAll(names);
            } else {
                for (String name : names) {
                    if (chunk == null || name.startsWith(chunk)) {
                        candidates.add(name);
                    }
                }
            }

            Collections.sort(candidates);
            if(parsedCmd.endsOnSeparator()) {
                return parsedCmd.getLastSeparatorIndex() + 1;
            } else {
                return parsedCmd.getLastChunkIndex();
            }
        }

        final OperationRequestAddress address = parsedCmd.getAddress();

        if(buffer.endsWith("..")) {
            return -1;
        }

        final String chunk;
        if (address.isEmpty() || parsedCmd.endsOnNodeSeparator()
                || parsedCmd.endsOnNodeTypeNameSeparator()
                || address.equals(ctx.getPrefix())) {
            chunk = null;
        } else if (address.endsOnType()) {
            chunk = address.getNodeType();
            address.toParentNode();
        } else {
            chunk = address.toNodeType();
        }

        final Collection<String> names;
        if(address.endsOnType()) {
            names = candidatesProvider.getNodeNames(ctx, address);
        } else {
            names = candidatesProvider.getNodeTypes(ctx, address);
        }

        if(names.isEmpty()) {
            return -1;
        }

        if(chunk == null) {
            candidates.addAll(names);
        } else {
            for (String name : names) {
                if (chunk == null || name.startsWith(chunk)) {
                    candidates.add(name);
                }
            }
        }

        if(candidates.size() == 1) {
            if(address.endsOnType()) {
                candidates.set(0, Util.escapeString(candidates.get(0), ESCAPE_SELECTOR));
            } else {
                candidates.set(0, Util.escapeString(candidates.get(0), ESCAPE_SELECTOR) + '=');
            }
        } else {
            Util.sortAndEscape(candidates, ESCAPE_SELECTOR);
        }

        return parsedCmd.endsOnSeparator() ? parsedCmd.getLastSeparatorIndex() + 1 : parsedCmd.getLastChunkIndex();
    }

    protected CommandLineCompleter getValueCompleter(CommandContext ctx, Iterable<CommandArgument> allArgs, final String argName) {
        CommandLineCompleter result = null;
        for (CommandArgument arg : allArgs) {
            if (arg.getFullName().equals(argName)) {
                return arg.getValueCompleter();
            } else if(arg.getIndex() == Integer.MAX_VALUE) {
                result = arg.getValueCompleter();
            }
        }
        return result;
    }

    protected CommandLineCompleter getValueCompleter(CommandContext ctx, Iterable<CommandArgument> allArgs, int index) {
        for (CommandArgument arg : allArgs) {
            if (arg.getIndex() == index || arg.getIndex() == Integer.MAX_VALUE) {
                return arg.getValueCompleter();
            }
        }
        return null;
    }
}
