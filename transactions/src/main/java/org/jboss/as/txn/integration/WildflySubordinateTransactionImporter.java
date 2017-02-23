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

package org.jboss.as.txn.integration;

import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.jboss.tm.SubordinateTransactionImporter;
import org.wildfly.transaction.client.LocalTransactionContext;

/**
 * Transaction importer class implementation which
 * provides importing transaction by defined xid
 * to current transaction context of wildfly transaction client.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public class WildflySubordinateTransactionImporter implements SubordinateTransactionImporter {


    /**
     * Importing transaction by the provided xid to {@link LocalTransactionContext}
     * of wildfly transaction client.
     *
     * @param xid  xid to be imported as a transaction
     * @return imported transaction, not null
     * @throws XAException  when issue with importing transaction happens
     */
    public Transaction getTransaction(Xid xid) throws XAException {
        return LocalTransactionContext.getCurrent().findOrImportTransaction(xid, 0).getTransaction();
    }

}
