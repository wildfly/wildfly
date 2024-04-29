/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.iiop.tm;

import org.jboss.as.txn.logging.TransactionLogger;
import org.omg.CORBA.LocalObject;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ORBInitInfoPackage.InvalidName;
import org.omg.PortableInterceptor.ORBInitializer;

public class InboundTransactionCurrentInitializer extends LocalObject implements ORBInitializer {

    public void pre_init(ORBInitInfo info) {
        try {
            // Create and register the InboundTransactionCurrent implementation class
            info.register_initial_reference(InboundTransactionCurrent.NAME, new InboundTransactionCurrentImpl());
        } catch (InvalidName e) {
            throw TransactionLogger.ROOT_LOGGER.cannotRegister(e);
        }
    }

    public void post_init(ORBInitInfo info) {
    }
}
