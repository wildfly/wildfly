/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.manual.elytron.seccontext;

import javax.ejb.Remote;

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
