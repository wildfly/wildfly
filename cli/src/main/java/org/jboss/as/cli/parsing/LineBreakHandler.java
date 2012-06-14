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
import org.jboss.as.cli.Util;

/**
 *
 * @author Alexey Loubyansky
 */
public class LineBreakHandler implements CharacterHandler {

    private final boolean fallbackToEscape;
    private final boolean leaveOnLnBreak;

    public LineBreakHandler(boolean leaveOnLnBreak, boolean fallbackToEscape) {
        this.leaveOnLnBreak = leaveOnLnBreak;
        this.fallbackToEscape = fallbackToEscape;
    }

    @Override
    public void handle(ParsingContext ctx) throws CommandFormatException {
        if(ctx.getCharacter() == '\\') {
            if(ctx.getInput().regionMatches(ctx.getLocation() + 1, Util.LINE_SEPARATOR, 0, Util.LINE_SEPARATOR.length())) {
                if(leaveOnLnBreak) {
                    ctx.leaveState();
                }
            } else if(fallbackToEscape){
                ctx.enterState(EscapeCharacterState.INSTANCE);
            } else {
                doHandle(ctx);
            }
        } else {
            doHandle(ctx);
        }
    }

    protected void doHandle(ParsingContext ctx) throws CommandFormatException {}
}
