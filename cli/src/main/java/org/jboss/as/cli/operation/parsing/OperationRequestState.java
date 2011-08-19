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
package org.jboss.as.cli.operation.parsing;

import org.jboss.as.cli.CommandFormatException;


/**
 *
 * @author Alexey Loubyansky
 */
public class OperationRequestState extends DefaultParsingState {

    public static final String ID = "OP_REQ";
    public static final OperationRequestState INSTANCE = new OperationRequestState();

    public OperationRequestState() {
        this(NodeState.INSTANCE, OperationNameState.INSTANCE, PropertyListState.INSTANCE, OutputRedirectionState.INSTANCE);
    }

    public OperationRequestState(final NodeState nodeState, final OperationNameState opState, final PropertyListState propList, final OutputRedirectionState outRedirect) {
        super(ID);
        setDefaultHandler(new EnterStateCharacterHandler(nodeState));
        enterState(':', opState);
        enterState('(', propList);
        enterState(OutputRedirectionState.OUTPUT_REDIRECT_CHAR, outRedirect);
        setReturnHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx)
                    throws CommandFormatException {
                if(ctx.isEndOfContent()) {
                    return;
                }
                CharacterHandler handler = enterStateHandlers.getHandler(ctx.getCharacter());
                if(handler != null) {
                    handler.handle(ctx);
                }
            }});
        setIgnoreWhitespaces(true);
    }
}
