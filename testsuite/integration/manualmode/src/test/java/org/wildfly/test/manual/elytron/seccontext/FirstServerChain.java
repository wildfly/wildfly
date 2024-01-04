/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.manual.elytron.seccontext;

import jakarta.ejb.Remote;

/**
 * Interface for the bean used as first bean in chain of 3 servers for authentication/authorization forwarding testing.
 *
 * @author olukas
 */
@Remote
public interface FirstServerChain {

    /**
     * @return The name of the Principal obtained from a call to EJBContext.getCallerPrincipal()
     */
    String whoAmI();

    /**
     * Obtains the name of the Principal obtained from a call to EJBContext.getCallerPrincipal() both for the bean called and
     * also from a call to a second bean (user may be switched before the second call - depending on arguments) and also from a
     * call to a third bean (user may be switched before the second call - depending on arguments).
     *
     * @return An array containing the name from the local call first followed by the name from the second call and third call.
     */
    String[] tripleWhoAmI(CallAnotherBeanInfo firstBeanInfo, CallAnotherBeanInfo secondBeanInfo);
}
