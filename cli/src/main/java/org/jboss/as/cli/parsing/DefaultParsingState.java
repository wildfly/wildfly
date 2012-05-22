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
public class DefaultParsingState extends BaseParsingState {

    protected final CharacterHandlerMap enterStateHandlers;
    private final CharacterHandlerMap handlers;
    private boolean ignoreWhitespaces;
    private boolean leaveOnWhitespace;

    private CharacterHandler defaultHandler = GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER;

    public DefaultParsingState(String id) {
        this(id, false);
    }

    public DefaultParsingState(String id, boolean enterLeaveContent) {
        this(id, enterLeaveContent, new DefaultCharacterHandlerMap());
    }

    public DefaultParsingState(String id, boolean enterLeaveContent, CharacterHandlerMap enterStateHandlers) {
        super(id);

        if(enterLeaveContent) {
            setEnterHandler(GlobalCharacterHandlers.CONTENT_CHARACTER_HANDLER);
            setLeaveHandler(GlobalCharacterHandlers.CONTENT_CHARACTER_HANDLER);
        }

        this.handlers = new DefaultCharacterHandlerMap();
        this.enterStateHandlers = enterStateHandlers;
    }

    public void setIgnoreWhitespaces(boolean ignoreWhitespaces) {
        this.ignoreWhitespaces = ignoreWhitespaces;
    }

    public boolean isIgnoreWhitespaces() {
        return this.ignoreWhitespaces;
    }

    public void setLeaveOnWhitespace(boolean leaveOnWhitespace) {
        this.leaveOnWhitespace = leaveOnWhitespace;
    }

    public boolean isLeaveOnWhitespace() {
        return this.leaveOnWhitespace;
    }

    public void setDefaultHandler(CharacterHandler handler) {
        if(handler == null) {
            throw new IllegalArgumentException("Default handler can't be null.");
        }
        this.defaultHandler = handler;
    }

    public CharacterHandler getDefaultHandler() {
        return this.defaultHandler;
    }

    public void putHandler(char ch, CharacterHandler handler) {
        handlers.putHandler(ch, handler);
    }

    public void enterState(char ch, ParsingState state) {
        enterStateHandlers.putHandler(ch, new EnterStateCharacterHandler(state));
    }

    public void leaveState(char ch) {
        enterStateHandlers.putHandler(ch, GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
    }

    public void setHandleEntrance(boolean handleEntrance) {
        if (handleEntrance) {
            setEnterHandler(new CharacterHandler() {
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    getHandler(ctx.getCharacter()).handle(ctx);
                }
            });
        } else {
            setEnterHandler(GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER);
        }
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.parsing.ParsingState#getHandler(char)
     */
    @Override
    public CharacterHandler getHandler(char ch) {

        if(ignoreWhitespaces && Character.isWhitespace(ch)) {
            return GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER;
        }

        if(leaveOnWhitespace && Character.isWhitespace(ch)) {
            return GlobalCharacterHandlers.LEAVE_STATE_HANDLER;
        }

        CharacterHandler handler = enterStateHandlers.getHandler(ch);
        if(handler != null) {
            return handler;
        }
        handler = handlers.getHandler(ch);
        if(handler != null) {
            return handler;
        }
        return defaultHandler;
    }
}
