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

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.parsing.CharacterHandler;
import org.jboss.as.cli.operation.parsing.DefaultParsingState;
import org.jboss.as.cli.operation.parsing.DefaultStateWithEndCharacter;
import org.jboss.as.cli.operation.parsing.EscapeCharacterState;
import org.jboss.as.cli.operation.parsing.GlobalCharacterHandlers;
import org.jboss.as.cli.operation.parsing.ParsingContext;
import org.jboss.as.cli.operation.parsing.QuotesState;

/**
 *
 * @author Alexey Loubyansky
 */
public class ArgumentValueState extends DefaultParsingState {

    public static final ArgumentValueState INSTANCE = new ArgumentValueState();
    public static final String ID = "ARG_VALUE";

    ArgumentValueState() {
        super(ID);
        this.setEnterHandler(new CharacterHandler() {
            @Override
            public void handle(ParsingContext ctx) throws OperationFormatException {
                if(ctx.getCharacter() != '=') {
                    getHandler(ctx.getCharacter()).handle(ctx);
                }
            }});
        putHandler(' ', GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
        enterState('"', QuotesState.QUOTES_INCLUDED);
        enterState('[', new DefaultStateWithEndCharacter("BRACKETS", ']', true, true, enterStateHandlers));
        enterState('(', new DefaultStateWithEndCharacter("PARENTHESIS", ')', true, true, enterStateHandlers));
        enterState('{', new DefaultStateWithEndCharacter("BRACES", '}', true, true, enterStateHandlers));
        enterState('\\', EscapeCharacterState.INSTANCE);
        setDefaultHandler(GlobalCharacterHandlers.CONTENT_CHARACTER_HANDLER);
    }
}
