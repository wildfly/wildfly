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
public interface ParsingState {

    String getId();

    CharacterHandler getEnterHandler();

    CharacterHandler getLeaveHandler();

    CharacterHandler getHandler(char ch);

    CharacterHandler getReturnHandler();

    CharacterHandler getEndContentHandler();

    /**
     * Whether the index of the value corresponding to this state
     * in the command line being parsed should be set to the index
     * when parsing enters this state.
     *
     * @return true if the index of the current value should be updated
     * when parsing enters this state, false - otherwise.
     */
    boolean updateValueIndex();

    /**
     * Whether the index of the current value being parsed should remain
     * the same until parsing leaves this state even if there are other
     * nested states that might want to update the value index
     * (i.e. states that return true from updateValueIndex).
     *
     * @return true if the value index should remain unchanged until
     * this state is left.
     */
    boolean lockValueIndex();
}
