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
package org.jboss.as.cli.parsing;


import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.command.CommandState;
import org.jboss.as.cli.parsing.operation.OperationRequestState;


/**
 *
 * @author Alexey Loubyansky
 */
public class InitialState extends DefaultParsingState {

    public static final InitialState INSTANCE;
    static {
        OperationRequestState opState = new OperationRequestState();
        opState.setHandleEntrance(true);
        INSTANCE = new InitialState(opState, CommandState.INSTANCE);
    }
    public static final String ID = "INITIAL";

    InitialState() {
        this(OperationRequestState.INSTANCE, CommandState.INSTANCE);
    }

    InitialState(OperationRequestState opState, final CommandState cmdState) {
        super(ID);
        enterState('.', opState);
        enterState(':', opState);
        enterState('/', opState);
        setDefaultHandler(new EnterStateCharacterHandler(cmdState));
        setIgnoreWhitespaces(true);

        final boolean[] returned = new boolean[1];
        setEnterHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                returned[0] = false;
            }});
        setReturnHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                returned[0] = true;
            }});
        setLeaveHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                if(!returned[0]) {
                    ctx.enterState(cmdState);
                }
            }});
    }
}
