/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.osgi.service;

import java.util.concurrent.TimeUnit;
import org.jboss.msc.service.StartContext;
import org.jboss.osgi.framework.spi.LockManager;
import org.jboss.osgi.framework.spi.LockManagerPlugin;

/**
 * An integation plugin for the {@link LockManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Apr-2013
 */
public final class LockManagerIntegration extends LockManagerPlugin {

    @Override
    protected LockManager createServiceValue(StartContext startContext) {
        final LockManager defaultManager = super.createServiceValue(startContext);
        return new LockManager() {

            @Override
            public <T extends LockableItem> T getItemForType(Class<T> type) {
                return defaultManager.getItemForType(type);
            }

            @Override
            public LockContext getCurrentContext() {
                return defaultManager.getCurrentContext();
            }

            @Override
            public LockContext lockItems(Method method, LockableItem... items) {
                // No locking in the integration layer.
                // Bundle lifecycle is handled through management ops
                return null;
            }

            @Override
            public LockContext lockItems(Method method, long timeout, TimeUnit unit, LockableItem... items) {
                return defaultManager.lockItems(method, timeout, unit, items);
            }

            @Override
            public void unlockItems(LockContext context) {
                defaultManager.unlockItems(context);
            }
        };
    }
}
