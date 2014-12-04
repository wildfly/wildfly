/*
* JBoss, Home of Professional Open Source.
* Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.messaging.activemq.jms;


import org.jboss.activemq.artemis.wildfly.integration.recovery.WildFlyActiveMQRegistry;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.tm.XAResourceRecoveryRegistry;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 *         9/22/11
 */
public class WildFlyRecoveryRegistry extends WildFlyActiveMQRegistry {
    static volatile ServiceContainer container;

    private XAResourceRecoveryRegistry registry;

    public WildFlyRecoveryRegistry() {
       registry = getXAResourceRecoveryRegistry();
       if (registry == null) {
          throw new IllegalStateException("Unable to find Recovery Registry");
       }
    }

    public XAResourceRecoveryRegistry getTMRegistry() {
       return registry;
    }

    private static XAResourceRecoveryRegistry getXAResourceRecoveryRegistry() {
        @SuppressWarnings("unchecked")
        ServiceController<XAResourceRecoveryRegistry> service = (ServiceController<XAResourceRecoveryRegistry>) container.getService(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER);
        return service == null ? null : service.getValue();
    }
}
