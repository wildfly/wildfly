/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.cmt.inheritance;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.Transaction;
import org.jboss.logging.Logger;

/**
 * Child class defining {@link TransactionAttribute} at level
 * of class.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ChildWithClassAnnotationSLSB extends SuperSLSB {
    private static final Logger log = Logger.getLogger(ChildWithClassAnnotationSLSB.class);

    /**
     * {@link TransactionAttribute} of the method should be NEVER.
     */
    @Override
    public Transaction aMethod() {
        log.trace(this.getClass().getName() + ".aMethod called ");
        return getTransaction();
    }

    /**
     * {@link TransactionAttribute} of the method inherited from super class
     * should be SUPPORTS.
     */
    // bMethod() call
}
