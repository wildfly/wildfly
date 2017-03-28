/*
Copyright 2017 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.jboss.as.test.integration.ejb.transaction.cmt.inheritance;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

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
