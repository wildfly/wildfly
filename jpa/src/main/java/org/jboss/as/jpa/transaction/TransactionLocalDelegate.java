/*
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
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
package org.jboss.as.jpa.transaction;

import org.jboss.tm.TransactionLocal;

import javax.transaction.Transaction;

/**
 * The interface to implementated for a transaction local implementation
 *
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 * @version $Revision: 37459 $
 */
public interface TransactionLocalDelegate {
    /**
     * get the transaction local value.
     *
     * @param local the transaction local
     * @param tx    the transcation
     * @return the value
     */
    Object getValue(org.jboss.tm.TransactionLocal local, Transaction tx);

    /**
     * put the value in the transaction local
     *
     * @param local the transaction local
     * @param tx    the transcation
     * @param value the value
     */
    void storeValue(org.jboss.tm.TransactionLocal local, Transaction tx, Object value);

    /**
     * does Transaction contain object?
     *
     * @param local the transaction local
     * @param tx    the transcation
     * @return true if it has the value
     */
    boolean containsValue(org.jboss.tm.TransactionLocal local, Transaction tx);

    /**
     * Lock the transaction local in the context of this transaction
     *
     * @param local the transaction local
     * @param tx    the transcation
     * @throws IllegalStateException if the transaction is not active
     * @throws InterruptedException  if the thread is interrupted
     */
    void lock(org.jboss.tm.TransactionLocal local, Transaction tx) throws InterruptedException;

    /**
     * Unlock the transaction local in the context of this transaction
     *
     * @param local the transaction local
     * @param tx    the transcation
     */
    void unlock(TransactionLocal local, Transaction tx);
}
