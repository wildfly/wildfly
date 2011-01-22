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

package org.jboss.as.jpa.transaction;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * A TransactionLocal is similar to ThreadLocal except it is keyed on the
 * Transactions. A transaction local variable is cleared after the transaction
 * completes.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author adrian@jboss.org
 * @author Scott Marlow
 * @version $Revision: 37459 $
 */
public class TransactionLocal {

    /**
     * To simplify null values handling in the preloaded data pool we use
     * this value instead of 'null'
     */
    private static final Object NULL_VALUE = new Object();

    /**
     * The transaction manager is maintained by the system and
     * manges the assocation of transaction to threads.
     */
    private static volatile TransactionManager transactionManager;

    /**
     * The delegate
     */
    private static volatile TransactionLocalDelegateImpl delegate;

    /**
     * Creates a thread local variable.
     *
     * @throws IllegalStateException if there is no system transaction manager
     */
    public static void setTransactionManager(TransactionManager tm) {
        if (transactionManager == null) {
            transactionManager = tm;
            delegate = new TransactionLocalDelegateImpl(tm);
        }

    }

    public static TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public TransactionLocal() {

    }

    /**
     * Creates a transaction local variable. Using the given transaction manager
     *
     * @param tm the transaction manager
     */
    public TransactionLocal(TransactionManager tm) {
        if (tm == null)
            throw new IllegalArgumentException("Null transaction manager");
        this.transactionManager = tm;
        delegate = new TransactionLocalDelegateImpl(tm);
    }

    /**
     * Lock the TransactionLocal using the current transaction<p>
     * <p/>
     * WARN: The current implemention just "locks the transactions"
     *
     * @throws IllegalStateException if the transaction is not active
     * @throws InterruptedException  if the thread is interrupted
     */
    public void lock() throws InterruptedException {
        lock(getTransaction());
    }

    /**
     * Lock the TransactionLocal using the provided transaction<p>
     * <p/>
     * WARN: The current implemention just "locks the transactions"
     *
     * @param transaction the transaction
     * @throws IllegalStateException if the transaction is not active
     * @throws InterruptedException  if the thread is interrupted
     */
    public void lock(Transaction transaction) throws InterruptedException {
        // ignore when there is no transaction
        if (transaction == null)
            return;

        delegate.lock(this, transaction);
    }

    /**
     * Unlock the TransactionLocal using the current transaction
     */
    public void unlock() {
        unlock(getTransaction());
    }

    /**
     * Unlock the ThreadLocal using the provided transaction
     *
     * @param transaction the transaction
     */
    public void unlock(Transaction transaction) {
        // ignore when there is no transaction
        if (transaction == null)
            return;

        delegate.unlock(this, transaction);
    }

    /**
     * Returns the initial value for this thransaction local.  This method
     * will be called once per accessing transaction for each TransactionLocal,
     * the first time each transaction accesses the variable with get or set.
     * If the programmer desires TransactionLocal variables to be initialized to
     * some value other than null, TransactionLocal must be subclassed, and this
     * method overridden. Typically, an anonymous inner class will be used.
     * Typical implementations of initialValue will call an appropriate
     * constructor and return the newly constructed object.
     *
     * @return the initial value for this TransactionLocal
     */
    protected Object initialValue() {
        return null;
    }


    /**
     * get the transaction local value.
     *
     * @param tx the transaction
     * @return the obejct
     */
    protected Object getValue(Transaction tx) {
        return delegate.getValue(this, tx);
    }

    /**
     * put the value in the TransactionImpl map
     *
     * @param tx    the transaction
     * @param value the value
     */
    protected void storeValue(Transaction tx, Object value) {
        delegate.storeValue(this, tx, value);
    }

    /**
     * does Transaction contain object?
     *
     * @param tx the transaction
     * @return true if it has an object
     */
    protected boolean containsValue(Transaction tx) {
        return delegate.containsValue(this, tx);
    }

    /**
     * Returns the value of this TransactionLocal variable associated with the
     * thread context transaction. Creates and initializes the copy if this is
     * the first time the method is called in a transaction.
     *
     * @return the value of this TransactionLocal
     */
    public Object get() {
        return get(getTransaction());
    }


    /**
     * Returns the value of this TransactionLocal variable associated with the
     * specified transaction. Creates and initializes the copy if this is the
     * first time the method is called in a transaction.
     *
     * @param transaction the transaction for which the variable it to
     *                    be retrieved
     * @return the value of this TransactionLocal
     * @throws IllegalStateException if an error occures while registering
     *                               a synchronization callback with the transaction
     */
    public Object get(Transaction transaction) {
        if (transaction == null) return initialValue();

        Object value = getValue(transaction);

        // is we didn't get a value initalize this object with initialValue()
        if (value == null) {
            // get the initial value
            value = initialValue();

            // if value is null replace it with the null value standin
            if (value == null) {
                value = NULL_VALUE;
            }

            // store the value
            try {
                storeValue(transaction, value);
            } catch (IllegalStateException e) {
                // depending on the delegate implementation it may be considered an error to
                // call storeValue after the tx has ended. Further, the tx ending may have
                // caused the disposal of a previously stored initial value.
                // for user convenience we ignore such errors and return the initialvalue here.
                return initialValue();
            }
        }

        // if the value is the null standin return null
        if (value == NULL_VALUE) {
            return null;
        }

        // finall return the value
        return value;
    }

    /**
     * Sets the value of this TransactionLocal variable associtated with the
     * thread context transaction. This is only used to change the value from
     * the one assigned by the initialValue method, and many applications will
     * have no need for this functionality.
     *
     * @param value the value to be associated with the thread context
     *              transactions's TransactionLocal
     */
    public void set(Object value) {
        set(getTransaction(), value);
    }

    /**
     * Sets the value of this TransactionLocal variable associtated with the
     * specified transaction. This is only used to change the value from
     * the one assigned by the initialValue method, and many applications will
     * have no need for this functionality.
     *
     * @param transaction the transaction for which the value will be set
     * @param value       the value to be associated with the thread context
     *                    transactions's TransactionLocal
     */
    public void set(Transaction transaction, Object value) {
        if (transaction == null) throw new IllegalStateException("there is no transaction");
        // If this transaction is unknown, register for synchroniztion callback,
        // and call initialValue to give subclasses a chance to do some
        // initialization.
        if (!containsValue(transaction)) {
            initialValue();
        }

        // if value is null replace it with the null value standin
        if (value == null) {
            value = NULL_VALUE;
        }

        // finally store the value
        storeValue(transaction, value);
    }

    public Transaction getTransaction() {
        try {
            return transactionManager.getTransaction();
        } catch (SystemException e) {
            throw new IllegalStateException("An error occured while getting the " +
                "transaction associated with the current thread: " + e);
        }
    }

}
