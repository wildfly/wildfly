/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import jakarta.annotation.Resource;
import jakarta.ejb.EJBContext;
import jakarta.ejb.SessionContext;
import jakarta.jms.Queue;
import jakarta.transaction.UserTransaction;

/**
 * @author Jaikiran Pai
 */
public class StatelessBean implements StatelessBeanRemote {
    @Resource
    private SessionContext sessionContext;

    public boolean isEJBContextAvailableThroughResourceEnvRef() {
        // resource-env-ref which setups up the EJBContext to be
        // available (also) under java:comp/env/EJBContext
        EJBContext ejbContext = (EJBContext) this.sessionContext.lookup("MyEJBContext");
        // successful if found. An exception (eg: NameNotFound) will be
        // thrown otherwise
        return ejbContext != null;
    }

    public boolean isUserTransactionAvailableThroughResourceEnvRef() {
        // resource-env-ref which setups up the UserTransaction to be
        // available (also) under java:comp/env/UserTransaction
        UserTransaction userTransaction = (UserTransaction) this.sessionContext.lookup("MyUserTransaction");
        // successful if found. An exception (eg: NameNotFound) will be
        // thrown otherwise
        return userTransaction != null;
    }

    public boolean isOtherResourceAvailableThroughResourceEnvRef() {
        // resource-env-ref which setups up the Queue to be
        // available under java:comp/env/MyQueue
        Queue queue = (Queue) this.sessionContext.lookup("MyQueue");
        // successful if found. An exception (eg: NameNotFound) will be
        // thrown otherwise
        return queue != null;

    }

}
