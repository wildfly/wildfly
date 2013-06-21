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

package org.wildfly.as.concurrent;

import org.wildfly.as.concurrent.context.Context;
import org.wildfly.as.concurrent.context.Utils;

import javax.enterprise.concurrent.ManageableThread;

/**
 * @author Eduardo Martins
 */
class ManageableThreadImpl extends Thread implements ManageableThread {

    private final Context context;
    private final ManagedThreadFactoryImpl parent;
    private volatile boolean shutdown;

    ManageableThreadImpl(Context context, ManagedThreadFactoryImpl parent) {
        this.context = context;
        this.parent = parent;
    }

    ManageableThreadImpl(Runnable target, Context context, ManagedThreadFactoryImpl parent) {
        super(target);
        this.context = context;
        this.parent = parent;
    }

    ManageableThreadImpl(ThreadGroup group, Runnable target, Context context, ManagedThreadFactoryImpl parent) {
        super(group, target);
        this.context = context;
        this.parent = parent;
    }

    ManageableThreadImpl(String name, Context context, ManagedThreadFactoryImpl parent) {
        super(name);
        this.context = context;
        this.parent = parent;
    }

    ManageableThreadImpl(ThreadGroup group, String name, Context context, ManagedThreadFactoryImpl parent) {
        super(group, name);
        this.context = context;
        this.parent = parent;
    }

    ManageableThreadImpl(Runnable target, String name, Context context, ManagedThreadFactoryImpl parent) {
        super(target, name);
        this.context = context;
        this.parent = parent;
    }

    ManageableThreadImpl(ThreadGroup group, Runnable target, String name, Context context, ManagedThreadFactoryImpl parent) {
        super(group, target, name);
        this.context = context;
        this.parent = parent;
    }

    ManageableThreadImpl(ThreadGroup group, Runnable target, String name, long stackSize, Context context, ManagedThreadFactoryImpl parent) {
        super(group, target, name, stackSize);
        this.context = context;
        this.parent = parent;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    void shutdown() {
        this.shutdown = true;
    }

    @Override
    public void run() {
        try {
            if (shutdown) {
                this.interrupt();
            }
            if (context == null) {
                super.run();
            } else {
                final Context previousContext = Utils.setContext(context);
                try {
                    super.run();
                } finally {
                    Utils.setContext(previousContext);
                }
            }
        } finally {
            parent.remove(this);
        }
    }
}
