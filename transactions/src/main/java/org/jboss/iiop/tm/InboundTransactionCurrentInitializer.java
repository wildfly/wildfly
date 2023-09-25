/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.iiop.tm;

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
            throw new RuntimeException("Could not register initial " +
                "reference for InboundTransactionCurrent implementation: " + e, e);
        }
    }

    public void post_init(ORBInitInfo info) {
    }
}
