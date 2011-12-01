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
package org.jboss.as.cli.parsing.operation.header;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.parsing.CharacterHandler;
import org.jboss.as.cli.parsing.DefaultParsingState;
import org.jboss.as.cli.parsing.EnterStateCharacterHandler;
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.operation.PropertyListState;


/**
 *
 * @author Alexey Loubyansky
 */
public class RolloutPlanState extends DefaultParsingState {

    public static final RolloutPlanState INSTANCE = new RolloutPlanState();
    public static final String ID = "ROLLOUT_PLAN_HEADER";

    RolloutPlanState() {
        this(ServerGroupListState.INSTANCE, new PropertyListState(' ', ' ', ';', '}'));
    }

    RolloutPlanState(ServerGroupListState sgList, PropertyListState props) {
        super(ID);
        this.setIgnoreWhitespaces(true);
        setEnterHandler(new EnterStateCharacterHandler(sgList));
        setDefaultHandler(new EnterStateCharacterHandler(props));
        setReturnHandler(new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx) throws CommandFormatException {
                if(ctx.isEndOfContent()) {
                    return;
                }
                final char ch = ctx.getCharacter();
                if(ch == '}' || ch == ';') {
                    ctx.leaveState();
                }
                //ctx.leaveState();
            }});
    }
}
