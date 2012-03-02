/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.Collection;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.ArgumentValueConverter.DMRWithFallbackConverter;
import org.jboss.as.cli.operation.ParsedOperationRequestHeader;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.parsing.DefaultParsingState;
import org.jboss.as.cli.parsing.ParserUtil;
import org.jboss.as.cli.parsing.operation.HeaderListState;
import org.jboss.dmr.ModelNode;

/**
*
* @author Alexey Loubyansky
*/
public final class HeadersArgumentValueConverter extends DMRWithFallbackConverter {

    private final DefaultCallbackHandler callback = new DefaultCallbackHandler();
    private final DefaultParsingState initialState = new DefaultParsingState("INITIAL_STATE");
    {
        initialState.enterState('{', HeaderListState.INSTANCE);
    }

    private final CommandContext ctx;

    public HeadersArgumentValueConverter(CommandContext ctx) {
        if(ctx == null) {
            throw new IllegalArgumentException("The command context can't be null.");
        }
        this.ctx = ctx;
    }

    @Override
    protected ModelNode fromNonDMRString(String value) throws CommandFormatException {
        callback.reset();
        ParserUtil.parse(value, callback, initialState);
        final Collection<ParsedOperationRequestHeader> headers = callback.getHeaders();
        if(headers.isEmpty()) {
            throw new CommandFormatException("'" + value +
                    "' doesn't follow format {[rollout server_group_list [rollback-across-groups];] (<header_name>=<header_value>;)*}");
        }
        final ModelNode node = new ModelNode();
        for(ParsedOperationRequestHeader header : headers) {
            header.addTo(ctx, node);
        }
        return node;
    }
}