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
package org.jboss.as.cli.operation.parsing;

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestParser;

/**
 *
 * @author Alexey Loubyansky
 */
public class ParsingUtil {

    public static void parseParameters(String str, final int startIndex,
            final OperationRequestParser.CallbackHandler opCallbackHandler)
            throws OperationFormatException {

        StateParser parser = new StateParser();

        PropertyValueState valueState = new PropertyValueState();
        PropertyState propState = new PropertyState(valueState);
        PropertyListState listState = new PropertyListState(propState);
        parser.addState('(', listState);

        ParsingStateCallbackHandler stateCallbackHandler = new ParsingStateCallbackHandler() {

            int nameValueSep = -1;
            StringBuilder propName = new StringBuilder();
            StringBuilder propValue = new StringBuilder();
            boolean valueContent;

            @Override
            public void enteredState(ParsingContext ctx) {

                String stateId = ctx.getState().getId();
                if (stateId.equals(PropertyListState.ID)) {
                    opCallbackHandler.propertyListStart(startIndex + ctx.getLocation());
                } else if(stateId.equals(PropertyState.ID)) {
                    propName.setLength(0);
                    propValue.setLength(0);
                    nameValueSep = -1;
                } else if(stateId.equals(PropertyValueState.ID)) {
                    nameValueSep = ctx.getLocation();
                    valueContent = true;
                }
            }

            @Override
            public void leavingState(ParsingContext ctx) throws OperationFormatException {

                String stateId = ctx.getState().getId();
                if (stateId.equals(PropertyListState.ID)) {
                    if(ctx.getCharacter() == ')') {
                        opCallbackHandler.propertyListEnd(startIndex + ctx.getLocation());
                    }
                } else if(stateId.equals(PropertyState.ID)) {
                    if(propValue.length() > 0) {
                        opCallbackHandler.property(propName.toString().trim(), propValue.toString().trim(), nameValueSep);
                    } else {
                        opCallbackHandler.propertyName(propName.toString().trim());
                        if(nameValueSep != -1) {
                            opCallbackHandler.propertyNameValueSeparator(nameValueSep);
                        }
                    }

                    if(ctx.getCharacter() == ',') {
                        opCallbackHandler.propertySeparator(startIndex + ctx.getLocation());
                    }
                } else if(stateId.equals(PropertyValueState.ID)) {
                    valueContent = false;
                }
            }

            @Override
            public void character(ParsingContext ctx)
                    throws OperationFormatException {

                if(valueContent) {
                    propValue.append(ctx.getCharacter());
                } else {
                    String stateId = ctx.getState().getId();
                    if (stateId.equals(PropertyState.ID)) {
                        propName.append(ctx.getCharacter());
                    }
                }
            }
        };

        String paramStr = startIndex == 0 ? str : str.substring(startIndex);
        parser.parse(paramStr, stateCallbackHandler);
    }
}
