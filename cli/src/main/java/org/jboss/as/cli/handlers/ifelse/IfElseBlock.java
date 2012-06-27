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
package org.jboss.as.cli.handlers.ifelse;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineException;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Alexey Loubyansky
 */
public class IfElseBlock {

    private static final String IF_BLOCK = "IF";

    public static IfElseBlock create(CommandContext ctx) throws CommandLineException {
        if(ctx.get(IF_BLOCK) != null) {
            throw new CommandLineException("Nesting if blocks are not supported.");
        }
        final IfElseBlock ifBlock = new IfElseBlock();
        ctx.set(IF_BLOCK, ifBlock);
        return ifBlock;
    }

    public static IfElseBlock get(CommandContext ctx) throws CommandLineException {
        final IfElseBlock ifBlock = (IfElseBlock) ctx.get(IF_BLOCK);
        if(ifBlock == null) {
            throw new CommandLineException("Not in an if block.");
        }
        return ifBlock;
    }

    public static IfElseBlock remove(CommandContext ctx) throws CommandLineException {
        final IfElseBlock ifBlock = (IfElseBlock) ctx.remove(IF_BLOCK);
        if(ifBlock == null) {
            throw new CommandLineException("Not in an if block.");
        }
        return ifBlock;
    }

    private static final byte IN_IF = 0;
    private static final byte IN_ELSE = 1;

    private static final String EQ = "==";

    private byte state;

    private String[] path;
    private String value;
    private ModelNode conditionRequest;
    private ModelNode ifRequest;

    public void setCondition(String condition, ModelNode request) throws CommandFormatException {
        if(condition == null) {
            throw new CommandFormatException("The path of the if condition can't be null.");
        }
        if(request == null) {
            throw new CommandFormatException("The request in the if condition can't be null.");
        }

        int i = condition.indexOf(EQ);
        if(i < 0) {
            throw new CommandFormatException("Failed to locate " + EQ + " in the if condition: " + condition);
        }
        final String pathStr = condition.substring(0, i).trim();
        if(pathStr.isEmpty()) {
            throw new CommandFormatException("The path in the if condition is empty: " + condition);
        }
        final String[] path = pathStr.split("\\.");
        if(path.length == 0) {
            throw new CommandFormatException("The path in the if condition is empty: '" + pathStr + "'");
        }
        final String value = condition.substring(i + EQ.length()).trim();
        if(value.isEmpty()) {
            throw new CommandFormatException("The value in the if condition is empty: " + condition);
        }

        this.path = path;
        this.value = value;
        this.conditionRequest = request;
        state = IN_IF;
    }

    public ModelNode getConditionRequest() {
        return this.conditionRequest;
    }

    public String[] getConditionPath() {
        return path;
    }

    public String getConditionValue() {
        return value;
    }

    public void setIfRequest(ModelNode request) throws CommandLineException {
        if(request == null) {
            throw new CommandLineException("if request is null.");
        }
        if(this.ifRequest != null) {
            throw new CommandLineException("if request is already initialized: " + this.ifRequest);
        }
        this.ifRequest = request;
    }

    public ModelNode getIfRequest() {
        return ifRequest;
    }

    public boolean isInIf() {
        return state == IN_IF;
    }

    public boolean isInElse() {
        return state == IN_ELSE;
    }

    public void setInElse() {
        this.state = IN_ELSE;
    }
}
