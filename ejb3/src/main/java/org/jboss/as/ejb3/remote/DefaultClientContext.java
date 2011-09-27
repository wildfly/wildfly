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
package org.jboss.as.ejb3.remote;

import org.jboss.as.server.deployment.SetupAction;
import org.jboss.ejb.client.EJBClientContext;

import java.util.Map;

/**
 * TODO: HACK to setup a {@link org.jboss.ejb.client.EJBClientContext} for the server
 *
 * @author Stuart Douglas
 */
public class DefaultClientContext {

    private static final EJBClientContext INSTANCE;

    static {
        EJBClientContext.create();
        INSTANCE = EJBClientContext.suspendCurrent();
    }

    public static void associate() {
        EJBClientContext.restoreCurrent(INSTANCE);
    }

    public static void disasociate() {
        EJBClientContext.suspendCurrent();
    }

    public static EJBClientContext getInstance() {
        return INSTANCE;
    }

    /**
     * SetupAction that sets up EJB invocations for arquillian
     */
    public static final SetupAction SETUP_ACTION = new SetupAction() {
        @Override
        public void setup(final Map<String, Object> properties) {
            associate();
        }

        @Override
        public void teardown(final Map<String, Object> properties) {
            disasociate();
        }

        @Override
        public int priority() {
            return 0;
        }
    };

}
