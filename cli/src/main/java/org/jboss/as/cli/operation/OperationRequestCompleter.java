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
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.EscapeSelector;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.impl.DefaultOperationCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;

import jline.Completor;

/**
 *
 * @author Alexey Loubyansky
 */
public class OperationRequestCompleter implements CommandLineCompleter, Completor {

    public static final OperationRequestCompleter INSTANCE = new OperationRequestCompleter(null);

    public static final EscapeSelector ESCAPE_SELECTOR = new EscapeSelector() {
        @Override
        public boolean isEscape(char ch) {
            return ch == ':' || ch == '/' || ch == '=' || ch == ' ' || ch == '"' || ch == '\\';
        }
    };

    private final CommandContext ctx;

    public OperationRequestCompleter(CommandContext ctx) {
        this.ctx = ctx;
    }

    /* (non-Javadoc)
     * @see jline.Completor#complete(java.lang.String, int, java.util.List)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" } )
    @Override
    public int complete(String buffer, int cursor, List candidates) {
        return complete(ctx, buffer, cursor, candidates);
    }

    @Override
    public int complete(CommandContext ctx, final String buffer, int cursor, List<String> candidates) {
        int firstCharIndex = 0;
        while(firstCharIndex < buffer.length()) {
            if(!Character.isWhitespace(buffer.charAt(firstCharIndex))) {
                break;
            }
            ++firstCharIndex;
        }

        // if it ends on .. then the completion shouldn't happen
        // since the node is selected and it doesn't end on a separator
        if(buffer.endsWith("..")) {
            return 0;
        }

        DefaultOperationCallbackHandler handler = new DefaultOperationCallbackHandler(new DefaultOperationRequestAddress(ctx.getPrefix()));

        try {
            ctx.getOperationRequestParser().parse(buffer, handler);
        } catch (OperationFormatException e1) {
            return -1;
        }

        if(handler.isRequestComplete()) {
            return -1;
        }

        if(handler.hasProperties() || handler.endsOnPropertyListStart()) {

            if(handler.endsOnPropertyValueSeparator()) {
                // no value completion
                return -1;
            }

            OperationCandidatesProvider provider = ctx.getOperationCandidatesProvider();

            List<String> propertyNames = provider.getPropertyNames(handler.getOperationName(), handler.getAddress());
            if(propertyNames.isEmpty()) {
                if(handler.endsOnPropertyListStart()) {
                    candidates.add(")");
                    return buffer.length();
                }
                return -1;
            }

            if(handler.endsOnPropertyListStart()) {
                if(propertyNames.size() == 1) {
                    candidates.add(propertyNames.get(0) + '=');
                } else {
                    candidates.addAll(propertyNames);
                    Collections.sort(candidates);
                }
                //return handler.getLastSeparatorIndex() + 1;
                return buffer.length();
            }

            Set<String> specifiedNames = handler.getPropertyNames();

            String chunk = null;
            for(String specifiedName : specifiedNames) {
                String value = handler.getPropertyValue(specifiedName);
                if(value == null) {
                    chunk = specifiedName;
                } else {
                    propertyNames.remove(specifiedName);
                }
            }

            if(chunk == null) {
                if(handler.endsOnPropertySeparator()) {
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
                //return handler.getLastSeparatorIndex() + 1;
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
            return handler.getLastSeparatorIndex() + 1;
        }

        if(handler.hasOperationName() || handler.endsOnAddressOperationNameSeparator()) {

            if(handler.getAddress().endsOnType()) {
                return -1;
            }
            OperationCandidatesProvider provider = ctx.getOperationCandidatesProvider();
            final List<String> names = provider.getOperationNames(handler.getAddress());
            if(names.isEmpty()) {
                return -1;
            }

            final String chunk = handler.getOperationName();
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
            return handler.getLastSeparatorIndex() + 1;
        }

        final OperationRequestAddress address = handler.getAddress();

        final String chunk;
        if (address.isEmpty() || handler.endsOnNodeSeparator()
                || handler.endsOnNodeTypeNameSeparator()
                || address.equals(ctx.getPrefix())
                // TODO this is not nice
                || buffer.endsWith("..")) {
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
                String onlyType = (String) candidates.get(0);
                address.toNodeType(onlyType);
                List<String> childNames = provider.getNodeNames(address);
                if (!childNames.isEmpty()) {
                    onlyType = Util.escapeString(onlyType, ESCAPE_SELECTOR);
                    candidates.clear();
                    if(childNames.size() == 1) {
                        candidates.add(onlyType  + '=' + Util.escapeString(childNames.get(0), ESCAPE_SELECTOR));
                    } else {
                        Util.sortAndEscape(childNames, ESCAPE_SELECTOR);
                        for (String name : childNames) {
                            candidates.add(onlyType + '=' + name);
                        }
                    }
                }
            }
        } else {
            Util.sortAndEscape(candidates, ESCAPE_SELECTOR);
        }

        return handler.getLastSeparatorIndex() + 1;
    }
}
