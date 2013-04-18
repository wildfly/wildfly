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

import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.service.StartContext;
import org.jboss.osgi.framework.spi.LockManager;
import org.jboss.osgi.framework.spi.LockManagerPlugin;
import org.jboss.osgi.spi.Attachable;
import org.jboss.osgi.spi.AttachmentKey;

/**
 * An integation plugin for the {@link LockManager}.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Apr-2013
 */
public final class LockManagerIntegration extends LockManagerPlugin {

    @SuppressWarnings("rawtypes")
    private static AttachmentKey<Stack> LOCK_CONTEXT_KEY = AttachmentKey.create(Stack.class);

    @Override
    protected LockManager createServiceValue(StartContext startContext) {
        final LockManager delegate = super.createServiceValue(startContext);
        return new LockManager() {

            @Override
            public <T extends LockableItem> T getItemForType(Class<T> type) {
                return delegate.getItemForType(type);
            }

            @Override
            public LockContext getCurrentLockContext() {
                return delegate.getCurrentLockContext();
            }

            @Override
            public LockContext lockItems(Method method, LockableItem... items) {
                LockContext context = null;
                if (!skipLocking(method, items)) {
                    context = delegate.lockItems(method, items);
                    pushAttachedLockContext(context, items);
                }
                return context;
            }

            @Override
            public LockContext lockItems(Method method, long timeout, TimeUnit unit, LockableItem... items) {
                LockContext context = null;
                if (!skipLocking(method, items)) {
                    context = delegate.lockItems(method, timeout, unit, items);
                    pushAttachedLockContext(context, items);
                }
                return context;
            }

            @Override
            public void unlockItems(LockContext context) {
                popAttachedLockContext(context);
                delegate.unlockItems(context);
            }

            @SuppressWarnings("unchecked")
            private synchronized boolean skipLocking(Method method, LockableItem... items) {

                // Another thread might qualify to skip locking

                // #1 There must be no current context associated with the thread
                if (getCurrentLockContext() != null)
                    return false;

                LockContext context = null;

                // #2 All items must have have same context attached
                for (LockableItem item : items) {
                    if (item instanceof Attachable) {
                        Attachable attachableItem = (Attachable) item;
                        Stack<LockContext> stack = attachableItem.getAttachment(LOCK_CONTEXT_KEY);
                        LockContext aux = stack != null && !stack.isEmpty() ? stack.peek() : null;
                        if (context == null && aux != null) {
                            context = aux;
                        }
                        if (context != aux) {
                            return false;
                        }
                    }
                }

                // #3 There must be an attached context
                if (context == null)
                    return false;

                // Skip the lock when UPDATE started another thread
                if (context.getMethod() == Method.UPDATE) {
                    if (method == Method.STOP || method == Method.INSTALL || method == Method.START) {
                        return true;
                    }
                }

                // Skip the lock when REFRESH started another thread
                if (context.getMethod() == Method.REFRESH) {
                    if (method == Method.STOP || method == Method.UNINSTALL || method == Method.INSTALL || method == Method.START) {
                        return true;
                    }
                }

                // Skip the lock when UNINSTALL started another thread
                if (context.getMethod() == Method.UNINSTALL) {
                    if (method == Method.STOP || method == Method.UNINSTALL) {
                        return true;
                    }
                }

                return false;
            }

            @SuppressWarnings("unchecked")
            private synchronized void pushAttachedLockContext(LockContext context, LockableItem... items) {
                for (LockableItem item : items) {
                    if (item instanceof Attachable) {
                        Attachable attachableItem = (Attachable) item;
                        Stack<LockContext> stack = attachableItem.getAttachment(LOCK_CONTEXT_KEY);
                        if (stack == null) {
                            stack = new Stack<LockContext>();
                            attachableItem.putAttachment(LOCK_CONTEXT_KEY, stack);
                        }
                        stack.push(context);
                    }
                }
            }

            private synchronized void popAttachedLockContext(LockContext context) {
                if (context != null) {
                    for (LockableItem item : context.getItems()) {
                        if (item instanceof Attachable) {
                            Attachable attachableItem = (Attachable) item;
                            Stack<?> stack = attachableItem.getAttachment(LOCK_CONTEXT_KEY);
                            if (stack.size() == 1) {
                                attachableItem.removeAttachment(LOCK_CONTEXT_KEY);
                            } else {
                                stack.pop();
                            }
                        }
                    }
                }
            }
        };
    }
}
