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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

/**
 * @author Brian Stansberry
 * 
 */
public class SetAttributesRequestHandler extends BasicRequestHandler {
    private final Map<String, Object> toStore = new HashMap<String, Object>();

    /**
     * Create a new EstablishSessionRequestHandler.
     * 
     */
    public SetAttributesRequestHandler(Map<String, Object> toStore, boolean checkNames) {
        super(toStore.keySet(), checkNames);
        this.toStore.putAll(toStore);
    }

    public void handleRequest(Request request, Response response) {
        super.handleRequest(request, response);

        HttpSession session = getSession();
        for (Map.Entry<String, Object> entry : toStore.entrySet()) {
            session.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        super.clear();
        if (this.toStore != null)
            this.toStore.clear();
    }

}
