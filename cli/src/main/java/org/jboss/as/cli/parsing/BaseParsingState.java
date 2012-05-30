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

/**
 *
 * @author Alexey Loubyansky
 */
abstract class BaseParsingState implements ParsingState {

    private CharacterHandler enterHandler = GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER;
    private CharacterHandler leaveHandler = GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER;
    private CharacterHandler returnHandler = GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER;
    private CharacterHandler eoc = GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER;
    private final String id;

    BaseParsingState(String id) {
        this.id = id;
    }

    /* (non-Javadoc)
     * @see org.jboss.as.cli.operation.parsing.ParsingState#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    @Override
    public CharacterHandler getEndContentHandler() {
        return eoc;
    }

    public void setEndContentHandler(CharacterHandler handler) {
        if(handler == null) {
            throw new IllegalStateException("The handler can't be null");
        }
        eoc = handler;
    }

    @Override
    public CharacterHandler getReturnHandler() {
        return returnHandler;
    }

    public void setReturnHandler(CharacterHandler handler) {
        if(handler == null) {
            throw new IllegalStateException("The handler can't be null");
        }
        returnHandler = handler;
    }

    @Override
    public CharacterHandler getEnterHandler() {
        return enterHandler;
    }

    public void setEnterHandler(CharacterHandler handler) {
        if(handler == null) {
            throw new IllegalStateException("The handler can't be null");
        }
        enterHandler = handler;
    }

    @Override
    public CharacterHandler getLeaveHandler() {
        return leaveHandler;
    }

    public void setLeaveHandler(CharacterHandler handler) {
        if(handler == null) {
            throw new IllegalStateException("The handler can't be null");
        }
        leaveHandler = handler;
    }

    @Override
    public boolean updateValueIndex() {
        return true;
    }

    @Override
    public boolean lockValueIndex() {
        return false;
    }
}
