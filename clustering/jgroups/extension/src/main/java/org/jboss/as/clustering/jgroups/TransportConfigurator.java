/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.jgroups;

import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolHook;
import org.jgroups.util.DefaultThreadFactory;
import org.jgroups.util.LazyThreadFactory;

/**
 * {@link ProtocolHook} that overrides the default thread factories for a transport.
 * @author Paul Ferraro
 */
public class TransportConfigurator implements ProtocolHook {

    @Override
    public void afterCreation(Protocol protocol) throws Exception {
        if (protocol instanceof TP) {
            TP transport = (TP) protocol;
            ClassLoader loader = JChannelFactory.class.getClassLoader();
            transport.setThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("", false), loader));
            transport.setTimerThreadFactory(new ClassLoaderThreadFactory(new LazyThreadFactory("Timer", true, true), loader));
            transport.setDefaultThreadPoolThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("Incoming", false, true), loader));
            transport.setOOBThreadPoolThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("OOB", false, true), loader));
            transport.setInternalThreadPoolThreadFactory(new ClassLoaderThreadFactory(new DefaultThreadFactory("INT", false, true), loader));
        }
    }
}
