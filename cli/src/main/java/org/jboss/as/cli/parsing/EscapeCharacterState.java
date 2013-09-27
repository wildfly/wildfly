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
public final class EscapeCharacterState extends BaseParsingState {

    public static final String ID = "ESCAPED_CHARACTER";

/*    private static final CharacterHandler EOC = GlobalCharacterHandlers.newErrorCharacterHandler(
    "Error parsing escaped character: the character after '\' is missing.");
*/

    public static final EscapeCharacterState INSTANCE = new EscapeCharacterState(false);
    /**
     * This one is useful when the escaping should be recognized but postponed
     * (for characters that otherwise would have affected the parsing flow, such as '"').
     */
    public static final EscapeCharacterState KEEP_ESCAPE = new EscapeCharacterState(true);

    private static final CharacterHandler HANDLER = new CharacterHandler() {
        @Override
        public void handle(ParsingContext ctx)
                throws CommandFormatException {
            ctx.getCallbackHandler().character(ctx);
            ctx.leaveState();
        }
    };

    private final boolean keepEscape;

    EscapeCharacterState() {
        this(false);
    }

    EscapeCharacterState(boolean keepEscape) {
        super(ID);
        this.keepEscape = keepEscape;
        setEnterHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                if(EscapeCharacterState.this.keepEscape) {
                    ctx.getCallbackHandler().character(ctx);
                } else if(ctx.getLocation() + 1 < ctx.getInput().length() &&
                        ctx.getInput().charAt(ctx.getLocation() + 1) == '\\') {
                    ctx.getCallbackHandler().character(ctx);
                }
            }});
    }

    @Override
    public CharacterHandler getHandler(char ch) {
        return HANDLER;
    }
}
