/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.infinispan;

import javax.transaction.SystemException;

import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.read.EntrySetCommand;
import org.infinispan.commands.read.KeySetCommand;
import org.infinispan.commands.read.SizeCommand;
import org.infinispan.commands.read.ValuesCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.LocalTxInvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.transaction.LocalTransaction;

/**
 * Temporary workaround for ISPN-4196.
 * @author Paul Ferraro
 */
public class TxInterceptor extends org.infinispan.interceptors.TxInterceptor {

    @Override
    public Object visitKeySetCommand(InvocationContext ctx, KeySetCommand command) throws Throwable {
        return enlistReadAndInvokeNext(ctx, command);
    }

    @Override
    public Object visitValuesCommand(InvocationContext ctx, ValuesCommand command) throws Throwable {
        return enlistReadAndInvokeNext(ctx, command);
    }

    @Override
    public Object visitEntrySetCommand(InvocationContext ctx, EntrySetCommand command) throws Throwable {
        return enlistReadAndInvokeNext(ctx, command);
    }

    @Override
    public Object visitSizeCommand(InvocationContext ctx, SizeCommand command) throws Throwable {
        return enlistReadAndInvokeNext(ctx, command);
    }

    private Object enlistReadAndInvokeNext(InvocationContext ctx, VisitableCommand command) throws Throwable {
        enlistIfNeeded(ctx);
        return invokeNextInterceptor(ctx, command);
    }

    private void enlistIfNeeded(InvocationContext ctx) throws SystemException {
        if (shouldEnlist(ctx)) {
            LocalTransaction localTransaction = enlist((TxInvocationContext) ctx);
            LocalTxInvocationContext localTxContext = (LocalTxInvocationContext) ctx;
            localTxContext.setLocalTransaction(localTransaction);
        }
    }

    private static boolean shouldEnlist(InvocationContext ctx) {
        return ctx.isInTxScope() && ctx.isOriginLocal();
    }
}
