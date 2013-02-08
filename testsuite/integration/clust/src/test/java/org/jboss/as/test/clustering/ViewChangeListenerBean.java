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
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartException;

@Stateless
@Remote(ViewChangeListener.class)
@Listener(sync = false)
public class ViewChangeListenerBean implements ViewChangeListener {
    public static final long TIMEOUT = 15000;

    @Override
    public void establishView(String cluster, String... names) throws InterruptedException {
        Set<String> expectedMembers = new TreeSet<String>();
        if (names != null) {
            for (String name: names) {
                expectedMembers.add(name + "/" + cluster);
            }
        }
        ServiceRegistry registry = ServiceContainerHelper.getCurrentServiceContainer();
        ServiceController<?> controller = registry.getService(ServiceName.JBOSS.append("infinispan", cluster));
        if (controller == null) {
            throw new IllegalStateException(String.format("Failed to locate service for cluster '%s'", cluster));
        }
        try {
            EmbeddedCacheManager manager = ServiceContainerHelper.getValue(controller, EmbeddedCacheManager.class);
            manager.addListener(this);
            try
            {
                long start = System.currentTimeMillis();
                long now = start;
                long timeout = start + TIMEOUT;
                synchronized (this) {
                    Set<String> members = this.getMembers(manager);
                    while (!expectedMembers.equals(members)) {
                        System.out.println(String.format("%s != %s, waiting for a view change event...", expectedMembers, members));
                        this.wait(timeout - now);
                        now = System.currentTimeMillis();
                        if (now >= timeout) {
                            throw new InterruptedException(String.format("Cluster '%s' failed to establish view %s within %d ms.  Current view is: %s", cluster, expectedMembers, TIMEOUT, members));
                        }
                        members = this.getMembers(manager);
                    }
                    System.out.println(String.format("Cluster '%s' successfully established view %s within %d ms.", cluster, expectedMembers, now - start));
                }
            } finally {
                manager.removeListener(this);
            }
        } catch (StartException e) {
            throw new IllegalStateException(e);
        }
    }

    private Set<String> getMembers(EmbeddedCacheManager manager) {
        Set<String> members = new TreeSet<String>();
        for (Address address: manager.getMembers()) {
            members.add(address.toString());
        }
        return members;
    }

    @ViewChanged
    public void viewChanged(ViewChangedEvent event) {
        synchronized (this) {
            this.notify();
        }
    }
}
