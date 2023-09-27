/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.cmt.never;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author Stuart Douglas
 */
@Stateful
public class NeverSFSB {

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Long[] doStuff(String[] stuff, String value, long anotherValue) {
        return null;
    }

}
