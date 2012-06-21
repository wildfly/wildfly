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
public interface ParsingContext {

    /**
     * The complete string being parsed.
     *
     * @return  the complete string being parsed
     */
    String getInput();

    /**
     * The current state.
     *
     * @return current state
     */
    ParsingState getState();

    /**
     * Enters the state passed in as the argument which then becomes the current state.
     *
     * @param state  the state to enter
     * @throws CommandFormatException  in case something went wrong
     */
    void enterState(ParsingState state) throws CommandFormatException;

    /**
     * Leaves the current state and and returns it.
     *
     * @return  the state that's been left
     * @throws CommandFormatException  in case something went wrong
     */
    ParsingState leaveState() throws CommandFormatException;

    /**
     * Leaves the current state and then enters it again.
     *
     * @throws CommandFormatException  in case something went wrong
     */
    void reenterState() throws CommandFormatException;

    /**
     * The callback handler used for current parsing.
     *
     * @return  the callback handler used for the current parsing
     */
    ParsingStateCallbackHandler getCallbackHandler();

    /**
     * The character at the current location in the input string.
     *
     * @return  the character at the current location in the input string
     */
    char getCharacter();

    /**
     * The current location in the input string.
     *
     * @return  the current location in the input string
     */
    int getLocation();

    /**
     * Checks whether the end of the input string has been reached.
     *
     * @return  true if the end of the input stream has been reached
     */
    boolean isEndOfContent();

    /**
     * Advances the current location by skipping the specified number of characters.
     *
     * @param offset  the number of characters to skip
     * @throws IndexOutOfBoundsException  if the new location exceeds the input string length
     */
    void advanceLocation(int offset) throws IndexOutOfBoundsException;
}
