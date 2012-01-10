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
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.RolloutPlanGroup;
import org.jboss.as.cli.operation.impl.ParsedRolloutPlanHeader;
import org.jboss.as.cli.operation.impl.SingleRolloutPlanGroup;
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.ParsingStateCallbackHandler;
import org.jboss.as.cli.parsing.operation.HeaderValueState;
import org.jboss.as.cli.parsing.operation.PropertyState;
import org.jboss.as.cli.parsing.operation.PropertyValueState;

/**
 *
 * @author Alexey Loubyansky
 */
public class RolloutPlanHeaderCallbackHandler implements ParsingStateCallbackHandler {

    private final ParsedRolloutPlanHeader header = new ParsedRolloutPlanHeader();
    private final DefaultCallbackHandler handler;

    final StringBuilder buffer = new StringBuilder();

    private String name;
    private RolloutPlanGroup group;
    private boolean concurrent;
    private int nameValueSep;

    public RolloutPlanHeaderCallbackHandler(DefaultCallbackHandler handler) {
        this.handler = handler;
    }

    @Override
    public void enteredState(ParsingContext ctx) throws CommandFormatException {
        final String id = ctx.getState().getId();
        //System.out.println("rollout.entered " + id + " '" + ctx.getCharacter() + "'");

        if(HeaderValueState.ID.equals(id)) {
            ctx.enterState(RolloutPlanState.INSTANCE);
        } else if(ServerGroupState.ID.equals(id)) {
            group = new SingleRolloutPlanGroup();
        } else if(ConcurrentSignState.ID.equals(id)) {
            concurrent = true;
        } else if ("NAME_VALUE_SEPARATOR".equals(id)) {
            name = buffer.length() == 0 ? null : buffer.toString().trim();
            if(name == null || name.isEmpty()) {
                throw new CommandFormatException("Property is missing name at index " + ctx.getLocation());
            }
            nameValueSep = ctx.getLocation();
        }
        buffer.setLength(0);
    }

    @Override
    public void leavingState(ParsingContext ctx) throws CommandFormatException {
        final String id = ctx.getState().getId();
        //System.out.println("rollout.leaving " + id + " '" + ctx.getCharacter() + "'");
        if(id.equals(HeaderValueState.ID)) {
            handler.header(header);
        } else if(PropertyValueState.ID.equals(id)) {
            final String value = buffer.length() == 0 ? null : buffer.toString().trim();
            if(value == null || value.isEmpty()) {
                throw new CommandFormatException("Property '" + name + "' is missing value at index " + ctx.getLocation());
            }

            if(group == null) {
                if("id".equals(name)) {
                    header.setPlanRef(value);
                } else {
                    header.addProperty(name, value);
                }
            } else {
                ((SingleRolloutPlanGroup)group).addProperty(name, value, nameValueSep);
            }
            nameValueSep = -1;
        } else if(PropertyState.ID.equals(id)) {
            if(name == null && buffer.length() > 0) {
                if(group != null) {
                    ((SingleRolloutPlanGroup)group).addProperty(buffer.toString().trim(), "true", -1);
                } else {
                    header.addProperty(buffer.toString().trim(), "true");
                }
                buffer.setLength(0);
            } else {
                name = null;
                buffer.setLength(0);
            }
        } else if(ServerGroupNameState.ID.equals(id)) {
            final String groupName = buffer.toString().trim();
            if(groupName.isEmpty()) {
                throw new CommandFormatException("Empty group name at index " + ctx.getLocation());
            }
            ((SingleRolloutPlanGroup)group).setGroupName(groupName);
        } else if(ServerGroupState.ID.equals(id)) {
            if(concurrent) {
                header.addConcurrentGroup(group);
                concurrent = false;
            } else {
                header.addGroup(group);
            }
            group = null;
        }
    }

    @Override
    public void character(ParsingContext ctx) throws CommandFormatException {
        //System.out.println("rollout.content " + ctx.getState().getId() + " '" + ctx.getCharacter() + "'");
        buffer.append(ctx.getCharacter());
    }
}
