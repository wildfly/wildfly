/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.transaction.cmt.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;

@Stateless
public class StatelessRequired2LifecycleBean extends LifecycleSuperClass {

    @PostConstruct
    public void postConstruct() {
        saveTxState();
    }

}
