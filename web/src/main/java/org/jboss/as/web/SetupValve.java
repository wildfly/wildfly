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

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.as.server.deployment.SetupAction;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * Valve that runs {@link org.jboss.as.server.deployment.SetupAction}s around a web request.
 *
 * @author Stuart Douglas
 */
public class SetupValve extends ValveBase {

    /**
     * Thread local used to initialise the Listener after startup.
     * <p/>
     * TODO: figure out a better way to do this
     */
    private static final ThreadLocal<List<SetupAction>> localSelector = new ThreadLocal<List<SetupAction>>();

    private final ChainItem chain;

    public SetupValve() {
        List<SetupAction> setup = localSelector.get();
        assert setup != null : "setup is null";

        final ListIterator<SetupAction> iterator = setup.listIterator(setup.size());
        ChainItem currentChainItem = null;
        while (iterator.hasPrevious()) {
            final SetupAction action = iterator.previous();
            currentChainItem = new ChainItem(currentChainItem, action);
        }
        chain = currentChainItem;
    }

    @Override
    public void invoke(final Request request, final Response response) throws IOException, ServletException {
        if(chain == null) {
            getNext().invoke(request, response);
        } else {
            chain.invoke(request, response);
        }
    }

    public static void beginComponentStart(List<SetupAction> selector) {
        localSelector.set(selector);
    }

    public static void endComponentStart() {
        localSelector.set(null);
    }

    private class ChainItem {

        private final ChainItem next;
        private final SetupAction action;

        public ChainItem(final ChainItem next, final SetupAction action) {
            this.next = next;
            this.action = action;
        }

        void invoke(final Request request, final Response response)  throws IOException, ServletException {

            try {
                action.setup(Collections.<String, Object>emptyMap());
                if(next == null) {
                    getNext().invoke(request, response);
                } else {
                    next.invoke(request, response);
                }
            } finally {
                action.teardown(Collections.<String, Object>emptyMap());
            }
        }

    }

}
