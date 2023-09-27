/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.cmt.mandatory;

import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author Stuart Douglas
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class MandatorySFSB {

    public void doStuff() {

    }

}
