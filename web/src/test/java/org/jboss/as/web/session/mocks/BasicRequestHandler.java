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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

/**
 * @author Brian Stansberry
 * 
 */
public class BasicRequestHandler implements RequestHandler {
    private Set<String> namesToCheck = new HashSet<String>();
    private boolean checkAttributeNames;
    private Map<String, Object> checkedAttributes = new HashMap<String, Object>();
    private Set<String> attributeNames = new HashSet<String>();
    private String sessionId;
    private long lastAccessedTime;
    private int maxInactiveInterval;
    private long creationTime;
    private boolean newSession;
    private HttpSession session;

    /**
     * Create a new AbstractRequestHandler.
     * 
     */
    public BasicRequestHandler(Set<String> toCheck, boolean checkNames) {
        if (toCheck != null)
            this.namesToCheck.addAll(toCheck);
        this.checkAttributeNames = checkNames;
    }

    @Override
    public void handleRequest(Request request, Response response) {
        this.session = request.getSession();
        this.sessionId = session.getId();
        this.lastAccessedTime = session.getLastAccessedTime();
        this.maxInactiveInterval = session.getMaxInactiveInterval();
        this.newSession = session.isNew();
        this.creationTime = session.getCreationTime();

        if (this.checkAttributeNames) {
            Enumeration<String> e = session.getAttributeNames();
            while (e.hasMoreElements())
                attributeNames.add(e.nextElement());
        }

        if (namesToCheck != null) {
            for (String name : namesToCheck) {
                checkedAttributes.put(name, session.getAttribute(name));
            }
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public boolean isNewSession() {
        return newSession;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public Map<String, Object> getCheckedAttributes() {
        return checkedAttributes;
    }

    public Set<String> getAttributeNames() {
        return attributeNames;
    }

    public HttpSession getSession() {
        return session;
    }

    public boolean isCheckAttributeNames() {
        return checkAttributeNames;
    }

    @Override
    public void clear() {
        if (this.attributeNames != null)
            this.attributeNames.clear();
        if (this.checkedAttributes != null)
            this.checkedAttributes.clear();
        if (this.namesToCheck != null)
            this.namesToCheck.clear();
        this.session = null;
        this.sessionId = null;
    }

}
