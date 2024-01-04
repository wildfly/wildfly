/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.naming.rmi;

import jakarta.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 *
 * @author Eduardo Martins
 */
@Stateless
public class RmiContextLookupBean {

    public void testRmiContextLookup(String serverAddress, int serverPort) throws Exception {
        final Context rmiContext = InitialContext.doLookup("rmi://" + serverAddress + ":" + serverPort);
        rmiContext.close();
    }
}
