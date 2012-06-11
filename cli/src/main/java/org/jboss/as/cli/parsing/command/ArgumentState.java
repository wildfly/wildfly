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
package org.jboss.as.cli.parsing.command;


import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.CharacterHandler;
import org.jboss.as.cli.parsing.DefaultParsingState;
import org.jboss.as.cli.parsing.EnterStateCharacterHandler;
import org.jboss.as.cli.parsing.GlobalCharacterHandlers;
import org.jboss.as.cli.parsing.LineBreakHandler;
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.WordCharacterHandler;


/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentState extends DefaultParsingState {

    public static final ArgumentState INSTANCE = new ArgumentState();
    public static final String ID = "PROP";

    ArgumentState() {
        this(ArgumentValueState.INSTANCE);
    }

    ArgumentState(ArgumentValueState valueState) {
        super(ID);
        setEnterHandler(GlobalCharacterHandlers.CONTENT_CHARACTER_HANDLER);
        final NameValueSeparatorState nvSep = new NameValueSeparatorState(valueState);
        enterState('=', nvSep);
        //setLeaveOnWhitespace(true);
        setDefaultHandler(WordCharacterHandler.IGNORE_LB_ESCAPE_ON);
        setWhitespaceHandler(new EnterStateCharacterHandler(new WhitespaceState()));
        setReturnHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                if(ctx.isEndOfContent()) {
                    ctx.leaveState();
                } else if(ctx.getCharacter() == '=') {
                    ctx.enterState(nvSep);
                } else {
                    ctx.leaveState();
                }
            }});
    }

    private static class WhitespaceState extends DefaultParsingState {
        public WhitespaceState() {
            super("WS");
            setDefaultHandler(new CharacterHandler(){
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    if(!Character.isWhitespace(ctx.getCharacter())) {
                        ctx.leaveState();
                    }
                }});
        }
        @Override
        public boolean updateValueIndex() {
            return false;
        }
    }

    private static class NameValueSeparatorState extends DefaultParsingState {
        NameValueSeparatorState(final ArgumentValueState valueState) {
            super("NAME_VALUE_SEPARATOR");
            setDefaultHandler(new LineBreakHandler(false, false){
                @Override
                protected void doHandle(ParsingContext ctx) throws CommandFormatException {
                    ctx.enterState(valueState);
                }
            });
            setReturnHandler(GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
            setIgnoreWhitespaces(true);
        }
    };
}
