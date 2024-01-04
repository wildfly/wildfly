/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.cmt.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
public class StatelessNeverLifecycleBean extends LifecycleSuperClass {

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    @PostConstruct
    public void postConstruct() {
        saveTxState();
    }

}
