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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
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

        if(parsedCmd.isRequestComplete()) {
            return -1;
        }

        if(parsedCmd.hasProperties() || parsedCmd.endsOnPropertyListStart()) {

            if(parsedCmd.endsOnPropertyValueSeparator()) {
                // no value completion
                return -1;
            }

            OperationCandidatesProvider provider = ctx.getOperationCandidatesProvider();

            List<String> propertyNames = provider.getPropertyNames(parsedCmd.getOperationName(), parsedCmd.getAddress());
            if(propertyNames.isEmpty()) {
                if(parsedCmd.endsOnPropertyListStart()) {
                    candidates.add(")");
                    return buffer.length();
                }
                return -1;
            }

            if(parsedCmd.endsOnPropertyListStart()) {
                if(propertyNames.size() == 1) {
                    candidates.add(propertyNames.get(0) + '=');
                } else {
                    candidates.addAll(propertyNames);
                    Collections.sort(candidates);
                }
                return parsedCmd.getLastSeparatorIndex() + 1;
            }

            Set<String> specifiedNames = parsedCmd.getPropertyNames();

            String chunk = null;
            for(String specifiedName : specifiedNames) {
                String value = parsedCmd.getPropertyValue(specifiedName);
                if(value == null) {
                    chunk = specifiedName;
                } else {
                    propertyNames.remove(specifiedName);
                }
            }

            if(chunk == null) {
                if(parsedCmd.endsOnPropertySeparator()) {
                    if(propertyNames.size() == 1) {
                        candidates.add(propertyNames.get(0) + '=');
                    } else {
                        candidates.addAll(propertyNames);
                        Collections.sort(candidates);
                    }
                } else if(propertyNames.isEmpty()) {
                    candidates.add(")");
                }
                return buffer.length();
            }

            for(String candidate : propertyNames) {
                if(candidate.startsWith(chunk)) {
                    candidates.add(candidate);
                }
            }

            if(candidates.size() == 1) {
                candidates.set(0, (String)candidates.get(0) + '=');
            } else {
                Collections.sort(candidates);
            }
            return parsedCmd.endsOnSeparator() ? parsedCmd.getLastSeparatorIndex() + 1 : parsedCmd.getLastChunkIndex();
        }

        if(parsedCmd.hasOperationName() || parsedCmd.endsOnAddressOperationNameSeparator()) {

            if(parsedCmd.getAddress().endsOnType()) {
                return -1;
            }
            OperationCandidatesProvider provider = ctx.getOperationCandidatesProvider();
            final List<String> names = provider.getOperationNames(parsedCmd.getAddress());
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
            return parsedCmd.endsOnSeparator() ? parsedCmd.getLastSeparatorIndex() + 1 : parsedCmd.getLastChunkIndex();
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

        OperationCandidatesProvider provider = ctx.getOperationCandidatesProvider();
        final List<String> names;
        if(address.endsOnType()) {
            names = provider.getNodeNames(address);
        } else {
            names = provider.getNodeTypes(address);
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
}
