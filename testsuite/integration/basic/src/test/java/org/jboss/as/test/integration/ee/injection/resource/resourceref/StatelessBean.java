/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.SessionContext;
import javax.jms.Queue;
import javax.transaction.UserTransaction;

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
