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
package org.jboss.as.cli.parsing.operation.header;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.CharacterHandler;
import org.jboss.as.cli.parsing.DefaultParsingState;
import org.jboss.as.cli.parsing.EnterStateCharacterHandler;
import org.jboss.as.cli.parsing.GlobalCharacterHandlers;
import org.jboss.as.cli.parsing.ParsingContext;


/**
 *
 * @author Alexey Loubyansky
 */
public class ServerGroupListState extends DefaultParsingState {

    public static final ServerGroupListState INSTANCE = new ServerGroupListState();
    public static final String ID = "SG_LIST";

    ServerGroupListState() {
        this(ServerGroupState.INSTANCE, ServerGroupSeparatorState.INSTANCE, ConcurrentSignState.INSTANCE);
    }

    ServerGroupListState(final ServerGroupState sg, final ServerGroupSeparatorState gs, final ConcurrentSignState cs) {
        super(ID);
        this.setIgnoreWhitespaces(true);
        setDefaultHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                ctx.leaveState();
            }});
        setEnterHandler(new EnterStateCharacterHandler(sg));
        putHandler('^', new EnterStateCharacterHandler(cs));
        putHandler(',', new EnterStateCharacterHandler(gs));
        putHandler('}', GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
        putHandler(';', GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
        setReturnHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                if(ctx.isEndOfContent()) {
                    return;
                }
/*                if(Character.isWhitespace(ctx.getCharacter())) {
                    ctx.leaveState();
                } else {
                    getHandler(ctx.getCharacter()).handle(ctx);
                }
*/
                if(Character.isWhitespace(ctx.getCharacter())) {
                    return;
                }

                switch(ctx.getCharacter()) {
                case '^':
                    ctx.enterState(cs);
                    break;
                case ',':
                    ctx.enterState(gs);
                    break;
                case '}':
                case ';':
                    ctx.leaveState();
                    break;
                default:
                    ctx.leaveState();
                }
            }});
    }
}
