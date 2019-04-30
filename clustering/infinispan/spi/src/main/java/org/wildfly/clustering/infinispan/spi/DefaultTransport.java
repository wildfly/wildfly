/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.infinispan.spi;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;

/**
 * Custom {@link org.infinispan.remoting.transport.jgroups.JGroupsTransport} with the following workarounds:
 * <ul>
 * <li>Disables redundant JMX mbean registration of the JGroups channel/protocols<li>
 * </ul>
 * @author Paul Ferraro
 */
@Scope(Scopes.GLOBAL)
public class DefaultTransport extends org.infinispan.remoting.transport.jgroups.JGroupsTransport {

    @Override
    public void start() {
        GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
        // WFLY-6685 Prevent Infinispan from registering channel mbeans
        // The JGroups subsystem already does this
        builder.globalJmxStatistics().read(this.configuration.globalJmxStatistics()).disable();
        builder.transport().read(this.configuration.transport());
        this.configuration = builder.build();

        super.start();
    }
}
