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

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transaction;
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
