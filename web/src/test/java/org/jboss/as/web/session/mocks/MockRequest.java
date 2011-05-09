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

package org.jboss.as.web.session.mocks;

import java.io.IOException;
import java.util.Random;

import org.apache.catalina.Context;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.connector.Request;
import org.apache.catalina.core.StandardContext;

/**
 * @author Brian Stansberry
 * 
 */
public class MockRequest extends Request {
    private Session session;
    private String requestedSessionId;
    private boolean requestedSessionIdFromURL;

    /**
     * Create a new MockRequest.
     * 
     */
    public MockRequest(Manager manager) {
        Context context = (Context) manager.getContainer();
        if (context != null) {
            this.setContext(context);
        } else {
            this.setContext(new StandardContext());
            this.getContext().setManager(manager);
        }
    }

    @Override
    public String getRequestedSessionId() {
        return requestedSessionId;
    }

    public void setRequestedSessionId(String requestedSessionId) {
        this.requestedSessionId = requestedSessionId;
    }

    @Override
    protected Session doGetSession(boolean create) {
        if (session == null) {
            Manager manager = getContext().getManager();
            if (requestedSessionId != null) {
                try {
                    session = manager.findSession(requestedSessionId);
                } catch (IOException e) {
                    session = null;
                }
            }

            if (session == null && create) {
                session = manager.createSession(requestedSessionId, new Random());
            }

            if (session != null) {
                session.access();
            }

        }

        if (session != null && !session.isValid()) {
            session = null;
            doGetSession(create);
        }

        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return requestedSessionIdFromURL;
    }

    public void setRequestedSessionIdFromURL(boolean requestedSessionIdFromURL) {
        this.requestedSessionIdFromURL = requestedSessionIdFromURL;
    }

    @Override
    public void recycle() {
        if (session != null)
            session.endAccess();
    }

}
