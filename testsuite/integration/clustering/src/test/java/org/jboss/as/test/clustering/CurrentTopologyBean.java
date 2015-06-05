/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering;

import java.util.Set;
import java.util.TreeSet;

import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * {@inheritDoc}
 *
 * @author Tomas Hofman (thofman@redhat.com)
 */
@Stateless
@Remote(CurrentTopology.class)
public class CurrentTopologyBean implements CurrentTopology {

    @Override
    public Set<String> getClusterMembers(String cluster) {
        ServiceRegistry registry = CurrentServiceContainer.getServiceContainer();
        ServiceName serviceName = ServiceName.JBOSS.append("infinispan", cluster);
        EmbeddedCacheManager cacheManager = ServiceContainerHelper.findValue(registry, serviceName);
        if (cacheManager == null) {
            throw new IllegalStateException(String.format("Failed to locate %s.", serviceName));
        }
        return getMembers(cacheManager);
    }

    private static Set<String> getMembers(EmbeddedCacheManager cacheManager) {
        Set<String> members = new TreeSet<>();
        for (Address address: cacheManager.getMembers()) {
            members.add(address.toString());
        }
        return members;
    }

}
