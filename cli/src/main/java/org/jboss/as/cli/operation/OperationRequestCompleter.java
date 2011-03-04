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
import org.jboss.as.cli.operation.impl.DefaultOperationCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;

import jline.Completor;

/**
 *
 * @author Alexey Loubyansky
 */
public class OperationRequestCompleter implements Completor {

    private final CommandContext ctx;

    public OperationRequestCompleter(CommandContext ctx) {
        this.ctx = ctx;
    }

    /* (non-Javadoc)
     * @see jline.Completor#complete(java.lang.String, int, java.util.List)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int complete(String buffer, int cursor, List candidates) {

        if(!buffer.isEmpty() && buffer.charAt(0) == '/')
            return -1;

        DefaultOperationCallbackHandler handler = new DefaultOperationCallbackHandler(new DefaultOperationRequestAddress(ctx.getPrefix()));

        try {
            ctx.getOperationRequestParser().parse(buffer, handler);
        } catch (OperationFormatException e1) {
            return -1;
        }

        if(handler.isRequestComplete()) {
            return -1;
        }

        if(handler.hasProperties() || handler.endsOnArgumentListStart()) {

            if(handler.endsOnArgumentValueSeparator()) {
                // no value completion
                return -1;
            }

            OperationCandidatesProvider provider = ctx.getOperationCandidatesProvider();

            List<String> propertyNames = provider.getPropertyNames(handler.getOperationName(), handler.getAddress());
            if(propertyNames.isEmpty()) {
                return -1;
            }

            if(handler.endsOnArgumentListStart()/*specifiedNames.isEmpty()*/) {
                candidates.addAll(propertyNames);
                Collections.sort(candidates);
                return handler.getLastSeparatorIndex() + 1;
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
                if(handler.endsOnArgumentSeparator()) {
                    candidates.addAll(propertyNames);
                    Collections.sort(candidates);
                }
                return handler.getLastSeparatorIndex() + 1;
            }

            for(String candidate : propertyNames) {
                if(candidate.startsWith(chunk)) {
                    candidates.add(candidate);
                }
            }

            Collections.sort(candidates);
            return handler.getLastSeparatorIndex() + 1;
        }

        if(handler.hasOperationName() || handler.endsOnAddressOperationNameSeparator()) {

            OperationCandidatesProvider provider = ctx.getOperationCandidatesProvider();
            final List<String> names = provider.getOperationNames(ctx.getPrefix());
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

        Collections.sort(candidates);
        return handler.getLastSeparatorIndex() + 1;
    }
}
