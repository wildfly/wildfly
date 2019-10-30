/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.iiop.tm;

import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.omg.CORBA.LocalObject;
import org.wildfly.transaction.client.ContextTransactionManager;
import org.wildfly.transaction.client.LocalTransactionContext;

public class InboundTransactionCurrentImpl extends LocalObject implements InboundTransactionCurrent {

    private static final long serialVersionUID = - 7415245830690060507L;

    public Transaction getCurrentTransaction() {
        final LocalTransactionContext current = LocalTransactionContext.getCurrent();
        try {
            current.importProviderTransaction();
        } catch (SystemException e) {
            throw new RuntimeException("InboundTransactionCurrentImpl unable to determine inbound transaction context", e);
        }

        try {
            return ContextTransactionManager.getInstance().suspend();
        } catch (SystemException e) {
            throw new RuntimeException("InboundTransactionCurrentImpl unable to suspend inbound transaction context", e);
        }
    }
}
