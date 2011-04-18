/**
 *
 */
package org.jboss.as.controller;

/**
 * Analogue to a JTA {@code Synchronization} for {@link ControllerTransactionContext model controller transactions}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ControllerTransactionSynchronization {

    /**
     *  This method is invoked before the start of the commit
     *  process. The method invocation is done in the context of the
     *  transaction that is about to be committed.
     */
    void beforeCompletion();

    /**
     *  This method is invoked after the transaction has committed or
     *  rolled back.
     *
     *  @param status {@code true} if the transaction successfully committed; {@code false} otherwise
     */
    void afterCompletion(boolean status);

}
