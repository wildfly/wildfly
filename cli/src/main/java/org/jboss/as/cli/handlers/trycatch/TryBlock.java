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
package org.jboss.as.cli.handlers.trycatch;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.dmr.ModelNode;

/**
 * Represents a try-catch block.
 *
 * @author Alexey Loubyansky
 */
public class TryBlock {

    private static final String TRY_BLOCK = "TRY";

    public static TryBlock create(CommandContext ctx) throws CommandLineException {
        if(ctx.get(TRY_BLOCK) != null) {
            throw new CommandLineException("Nesting try blocks is not supported.");
        }
        final TryBlock tryBlock = new TryBlock();
        ctx.set(TRY_BLOCK, tryBlock);
        return tryBlock;
    }

    public static TryBlock get(CommandContext ctx) throws CommandLineException {
        final TryBlock tryBlock = (TryBlock) ctx.get(TRY_BLOCK);
        if(tryBlock == null) {
            throw new CommandLineException("Not in a try block.");
        }
        return tryBlock;
    }

    public static TryBlock remove(CommandContext ctx) throws CommandLineException {
        final TryBlock tryBlock = (TryBlock) ctx.remove(TRY_BLOCK);
        if(tryBlock == null) {
            throw new CommandLineException("Not in a try block.");
        }
        return tryBlock;
    }

    private static final byte IN_TRY = 0;
    private static final byte IN_CATCH = 1;
    private static final byte IN_FINALLY = 2;

    private byte state;

    private ModelNode tryRequest;
    private ModelNode catchRequest;

    public ModelNode getTryRequest() {
        return tryRequest;
    }

    public void setTryRequest(ModelNode tryRequest) throws CommandLineException {
        if(catchRequest != null) {
            throw new CommandLineException("Only one catch is allowed.");
        }
        if(this.tryRequest != null) {
            throw new CommandLineException("try request is already initialized.");
        }
        this.tryRequest = tryRequest;
    }

    public ModelNode getCatchRequest() {
        return catchRequest;
    }

    public void setCatchRequest(ModelNode catchRequest) throws CommandLineException {
        if(this.catchRequest != null) {
            throw new CommandLineException("catch request is already initialized.");
        }
        this.catchRequest = catchRequest;
    }

    public boolean isInTry() {
        return state == IN_TRY;
    }

    public boolean isInCatch() {
        return state == IN_CATCH;
    }

    public boolean isInFinally() {
        return state == IN_FINALLY;
    }

    public void setInCatch() {
        this.state = IN_CATCH;
    }

    public void setInFinally() {
        this.state = IN_FINALLY;
    }
}
