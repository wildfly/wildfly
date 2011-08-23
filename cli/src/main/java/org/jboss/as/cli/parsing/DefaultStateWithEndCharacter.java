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

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultStateWithEndCharacter extends DefaultParsingState {

    private final char leaveStateChar;

    DefaultStateWithEndCharacter(String id, char leaveStateChar) {
        this(id, leaveStateChar, true);
    }

    DefaultStateWithEndCharacter(String id, char leaveStateChar, boolean endRequired) {
        this(id, leaveStateChar, endRequired, false);
    }

    DefaultStateWithEndCharacter(String id, char leaveStateChar, boolean endRequired, boolean enterLeaveContent) {
        this(id, leaveStateChar, endRequired, enterLeaveContent, new DefaultCharacterHandlerMap());
    }

    public DefaultStateWithEndCharacter(String id, final char leaveStateChar, boolean endRequired, boolean enterLeaveContent, CharacterHandlerMap enterStateHandlers) {
        super(id, enterLeaveContent, enterStateHandlers);
        this.leaveStateChar = leaveStateChar;
        if(enterLeaveContent) {
            setLeaveHandler(new CharacterHandler() {
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    if(ctx.getCharacter() == leaveStateChar) {
                        GlobalCharacterHandlers.CONTENT_CHARACTER_HANDLER.handle(ctx);
                    }
                }});
        }
        if(endRequired) {
           this.setEndContentHandler(new ErrorCharacterHandler(("Closing '" + leaveStateChar + "' is missing.")));
        }
        this.setDefaultHandler(GlobalCharacterHandlers.CONTENT_CHARACTER_HANDLER);
    }

    @Override
    public CharacterHandler getHandler(char ch) {
        return leaveStateChar == ch ? GlobalCharacterHandlers.LEAVE_STATE_HANDLER : super.getHandler(ch);
    }
}
