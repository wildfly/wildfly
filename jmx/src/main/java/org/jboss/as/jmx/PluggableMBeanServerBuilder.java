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
package org.jboss.as.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;

/**
 * <p>To use this builder, specify {@code -mbeanserverbuildermodule=org.jboss.as.jmx} when bootstrapping
 * jboss-modules, which in turn sets {@code -Djavax.management.builder.initial=org.jboss.as.jmx.PluggableMBeanServerBuilder}
 * (loaded from this module's {code META-INF/services/javax.management.MBeanServerBuilder}. This builder
 * returns an instance of {@link PluggableMBeanServerImpl} which can be used to set the MBeanServer chain, meaning that the
 * platform mbean server gets the extra functionality for TCCL, ModelController and whatever other behaviour we want to add.</p>
 *
 * <p>If the {@code -mbeanserverbuildermodule} option is not specified, the additional behaviour is only added to
 * calls coming in via the remote connector or MBeanServers injected via a dependency on {@link MBeanServerService}.</p>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class PluggableMBeanServerBuilder extends MBeanServerBuilder {

    public PluggableMBeanServerBuilder() {
    }

    @Override
    public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer, MBeanServerDelegate delegate) {
        return new PluggableMBeanServerImpl(super.newMBeanServer(defaultDomain, outer, delegate));
    }
}