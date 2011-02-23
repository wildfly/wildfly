/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web;

import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.jboss.as.ee.naming.NamespaceSelectorService;

/**
 * An InstanceListener used to push/pop the application naming context.
 *
 * @author Stuart Douglas
 */
public class NamingListener implements InstanceListener {

    private final NamespaceSelectorService selector;

    /**
     * Thread local used to initialise the Listener after startup.
     *
     * TODO: figure out a better way to do this
     */
    private static final ThreadLocal<NamespaceSelectorService> localSelector = new ThreadLocal<NamespaceSelectorService>();

    public NamingListener() {
        selector = localSelector.get();
        assert selector != null : "selector is null";
    }

    public void instanceEvent(InstanceEvent event) {
        String type = event.getType();
        // Push the identity on the before init/destroy
        if (type.equals(InstanceEvent.BEFORE_DISPATCH_EVENT)
                || type.equals(InstanceEvent.BEFORE_REQUEST_EVENT)
                || type.equals(InstanceEvent.BEFORE_DESTROY_EVENT)
                || type.equals(InstanceEvent.BEFORE_INIT_EVENT)) {
            // Push naming id
            selector.activate();
        }
        // Pop the identity on the after init/destroy
        else if (type.equals(InstanceEvent.AFTER_DISPATCH_EVENT)
                || type.equals(InstanceEvent.AFTER_REQUEST_EVENT)
                || type.equals(InstanceEvent.AFTER_DESTROY_EVENT)
                || type.equals(InstanceEvent.AFTER_INIT_EVENT)) {
            // Pop naming id
            selector.deactivate();
        }
    }

    public static void beginComponentStart(NamespaceSelectorService selector) {
        localSelector.set(selector);
        selector.activate();
    }

    public static void endComponentStart() {
        localSelector.get().deactivate();
        localSelector.set(null);
    }

}
