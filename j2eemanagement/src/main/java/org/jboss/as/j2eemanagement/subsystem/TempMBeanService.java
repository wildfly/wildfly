/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.j2eemanagement.subsystem;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.ejb.client.EJBClientContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A temp mbean to wrap calls to the management ejb while
 * waiting for proper remote ejb calls to be implemented
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TempMBeanService implements Service<Void>{

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(ServiceName.of("temp", "mbean", "lookup"));
    final InjectedValue<MBeanServer> mbeanServerValue = new InjectedValue<MBeanServer>();
    final InjectedValue<EJBClientContext> ejbClientContextValue = new InjectedValue<EJBClientContext>();
    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            mbeanServerValue.getValue().registerMBean(new Temp(ejbClientContextValue.getValue()), new ObjectName("jboss.test:type=lookup"));
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
    }

}
