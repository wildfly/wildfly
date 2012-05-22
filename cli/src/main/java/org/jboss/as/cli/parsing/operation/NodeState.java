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
package org.jboss.as.cli.parsing.operation;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.CharacterHandler;
import org.jboss.as.cli.parsing.DefaultParsingState;
import org.jboss.as.cli.parsing.GlobalCharacterHandlers;
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.QuotesState;
import org.jboss.as.cli.parsing.WordCharacterHandler;

/**
 *
 * @author Alexey Loubyansky
 */
public class NodeState extends DefaultParsingState {

    public static final String ID = "NODE";
    public static final NodeState INSTANCE = new NodeState();

    public NodeState() {
        super(ID);

        setEnterHandler(new CharacterHandler(){

            @Override
            public void handle(ParsingContext ctx)
                    throws CommandFormatException {
                if(ctx.getCharacter() == '/') {
                    ctx.leaveState();
                } else {
                    getHandler(ctx.getCharacter()).handle(ctx);
                }
            }});

        setIgnoreWhitespaces(true);
        setDefaultHandler(WordCharacterHandler.IGNORE_LB_ESCAPE_ON);

        putHandler('=', new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx)
                    throws CommandFormatException {
                ctx.leaveState();
            }});

        putHandler('/', new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx)
                    throws CommandFormatException {
                ctx.leaveState();
            }});

        putHandler(':', GlobalCharacterHandlers.LEAVE_STATE_HANDLER);

        enterState('"', QuotesState.QUOTES_EXCLUDED);
    }
}
