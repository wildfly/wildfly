/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.web;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.apache.catalina.ThreadBindingListener;
import org.jboss.as.server.deployment.SetupAction;

/**
 * ThreadBindingListener that runs {@link org.jboss.as.server.deployment.SetupAction}s around a web request.
 *
 * This sets up things such as the {@link org.jboss.as.naming.context.NamespaceContextSelector} context
 * and the {@link org.jboss.ejb.client.EJBClientContext}
 *
 * @author Stuart Douglas
 */
public class ThreadSetupBindingListener implements ThreadBindingListener {

    private final List<SetupAction> actions;

    public ThreadSetupBindingListener(List<SetupAction> setup) {
        this.actions = setup;
    }

    @Override
    public void bind() {
        for (SetupAction action : actions) {
            action.setup(Collections.<String, Object>emptyMap());
        }
    }

    @Override
    public void unbind() {
        final ListIterator<SetupAction> iterator = actions.listIterator(actions.size());
        Throwable error = null;
        while (iterator.hasPrevious()) {
            SetupAction action = iterator.previous();
             try {
                action.teardown(Collections.<String, Object>emptyMap());
             } catch (Throwable e) {
                 error = e;
             }
        }
        if(error != null) {
            throw new RuntimeException(error);
        }
    }
}
