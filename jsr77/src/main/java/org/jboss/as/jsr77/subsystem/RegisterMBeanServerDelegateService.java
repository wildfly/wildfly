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
package org.jboss.as.jsr77.subsystem;

import org.jboss.as.controller.ModelController;
import org.jboss.as.server.jmx.PluggableMBeanServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class RegisterMBeanServerDelegateService implements Service<Void>{

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(ServiceName.of(JSR77ManagementExtension.SUBSYSTEM_NAME, "mbeanwrapper"));

    final InjectedValue<PluggableMBeanServer> injectedMbeanServer = new InjectedValue<PluggableMBeanServer>();
    final InjectedValue<ModelController> injectedController = new InjectedValue<ModelController>();

    volatile JSR77ManagementMBeanServer server;

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
        server = new JSR77ManagementMBeanServer(injectedController.getValue());
        injectedMbeanServer.getValue().addPlugin(server);
    }

    @Override
    public void stop(StopContext context) {
        injectedMbeanServer.getValue().removePlugin(server);
        server = null;
    }

}
