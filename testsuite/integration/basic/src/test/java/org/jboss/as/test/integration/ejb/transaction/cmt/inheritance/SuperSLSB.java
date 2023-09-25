/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.cmt.inheritance;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.jboss.logging.Logger;

/**
 * Parent bean which shape is based on ejb spec <code>class SomeClass</code>
 * from chapter <pre>8.3.7.1Specification of Transaction Attributes
 * with Metadata Annotations</pre>
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SuperSLSB {
    private static final Logger log = Logger.getLogger(SuperSLSB.class);

    @Resource(lookup = "java:/TransactionManager")
    protected TransactionManager tm;

    public Transaction aMethod() {
        log.trace(this.getClass().getName() + ".aMethod called ");
        return getTransaction();
    }

    public Transaction bMethod() {
        log.trace(this.getClass().getName() + ".bMethod called ");
        return getTransaction();
    }

    public Transaction cMethod() {
        log.trace(this.getClass().getName() + ".cMethod called ");
        return getTransaction();
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Transaction neverMethod() {
        log.trace(this.getClass().getName() + ".neverMethod called ");
        return getTransaction();
    }

    protected Transaction getTransaction() {
        try {
            return tm.getTransaction();
        } catch (SystemException se) {
            throw new IllegalStateException("Can't get transaction from tm '"
                + tm + "'");
        }
    }
}
