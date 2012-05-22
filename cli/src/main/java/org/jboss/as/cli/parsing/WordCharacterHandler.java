/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
public class WordCharacterHandler implements CharacterHandler {

    public static final WordCharacterHandler IGNORE_LB_ESCAPE_ON = new WordCharacterHandler(false, true);
    public static final WordCharacterHandler IGNORE_LB_ESCAPE_OFF = new WordCharacterHandler(false, false);

    private static final String LN_SEP = Util.getLineSeparator();
    private final boolean fallbackToEscape;
    private final boolean leaveOnLnBreak;

    public WordCharacterHandler(boolean leaveOnLnBreak, boolean fallbackToEscape) {
        this.leaveOnLnBreak = leaveOnLnBreak;
        this.fallbackToEscape = fallbackToEscape;
    }

    @Override
    public void handle(ParsingContext ctx) throws CommandFormatException {
        //System.out.println("word: '" + ctx.getCharacter() + "'");
        if(ctx.getCharacter() == '\\') {
            if(ctx.getInput().regionMatches(ctx.getLocation() + 1, LN_SEP, 0, LN_SEP.length())) {
                System.err.println("line break in '" + ctx.getInput().substring(0, ctx.getLocation()) + "'");
                if(leaveOnLnBreak) {
                    ctx.leaveState();
                }
            } else if(fallbackToEscape){
                ctx.enterState(EscapeCharacterState.INSTANCE);
            } else {
                ctx.getCallbackHandler().character(ctx);
            }
        } else {
            ctx.getCallbackHandler().character(ctx);
        }
    }
}