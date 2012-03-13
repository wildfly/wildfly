/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.single.ejb.remotecall;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.naming.InitialContext;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
@Singleton
@Remote(ServiceRemoteInterface.class)
@Local(ServiceLocalInterface.class)
public class ServiceBean implements ServiceLocalInterface, ServiceRemoteInterface {
    public void test() {

    }

    public void testLocal() throws Exception {
        InitialContext ctx = new InitialContext();

        String lookupStr = "java:module/" + StatefulBean.class.getSimpleName() + "!" + LocalInterface.class.getName();
        LocalInterface stateful1 = (LocalInterface) ctx.lookup(lookupStr);
        LocalInterface stateful2 = (LocalInterface) ctx.lookup(lookupStr);

        lookupStr = "java:module/" + StatefulClusteredBean.class.getSimpleName() + "!" + RemoteInterface.class.getName();
        RemoteInterface statefulClustered1 = (RemoteInterface) ctx.lookup(lookupStr);
        RemoteInterface statefulClustered2 = (RemoteInterface) ctx.lookup(lookupStr);

        lookupStr = "java:module/" + StatelessBean.class.getSimpleName() + "!" + LocalInterface.class.getName();
        LocalInterface stateless1 = (LocalInterface) ctx.lookup(lookupStr);
        LocalInterface stateless2 = (LocalInterface) ctx.lookup(lookupStr);

        lookupStr = "java:module/" + StatelessClusteredBean.class.getSimpleName() + "!" + RemoteInterface.class.getName();
        RemoteInterface statelessClustered1 = (RemoteInterface) ctx.lookup(lookupStr);
        RemoteInterface statelessClustered2 = (RemoteInterface) ctx.lookup(lookupStr);

        lookupStr = "java:module/" + ServiceBean.class.getSimpleName() + "!" + ServiceLocalInterface.class.getName();
        ServiceLocalInterface service1 = (ServiceLocalInterface) ctx.lookup(lookupStr);
        ServiceLocalInterface service2 = (ServiceLocalInterface) ctx.lookup(lookupStr);

        stateful1.test();
        stateful2.test();
        statefulClustered1.test();
        statefulClustered2.test();
        stateless1.test();
        stateless2.test();
        statelessClustered1.test();
        statelessClustered2.test();
        service1.test();
        service2.test();

        assertFalse(stateful1.hashCode() == stateful2.hashCode());
        assertFalse(statefulClustered1.hashCode() == statefulClustered2.hashCode());
        assertTrue(stateless1.hashCode() == stateless2.hashCode());
        assertTrue(statelessClustered1.hashCode() == statelessClustered2.hashCode());
        assertTrue(service1.hashCode() == service2.hashCode());

        assertFalse(stateful1.equals(stateful2));
        assertFalse(statefulClustered1.equals(statefulClustered2));
        assertTrue(stateless1.equals(stateless2));
        assertTrue(statelessClustered1.equals(statelessClustered2));
        assertTrue(service1.equals(service2));
    }

    private void assertFalse(boolean b) {
        if (b) {
            throw new RuntimeException("Assertion failed!");
        }
    }

    private void assertTrue(boolean b) {
        if (!b) {
            throw new RuntimeException("Assertion failed!");
        }
    }

}
