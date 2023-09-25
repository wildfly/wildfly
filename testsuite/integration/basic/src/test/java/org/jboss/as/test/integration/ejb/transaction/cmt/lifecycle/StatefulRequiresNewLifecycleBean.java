/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.cmt.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateful
public class StatefulRequiresNewLifecycleBean extends LifecycleSuperClass {

    @PostConstruct
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void postConstruct() {
        saveTxState();
    }

}
