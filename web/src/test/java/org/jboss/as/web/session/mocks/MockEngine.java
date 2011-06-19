/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

import org.apache.catalina.Engine;
import org.apache.catalina.Service;

/**
 * @author Brian Stansberry
 * 
 */
public class MockEngine extends MockContainer implements Engine {
    private Service service;
    private String defaultHost = "localhost";
    private String jvmRoute;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Engine#getDefaultHost()
     */
    public String getDefaultHost() {
        return defaultHost;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Engine#getJvmRoute()
     */
    public String getJvmRoute() {
        return jvmRoute;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Engine#getService()
     */
    public Service getService() {
        return service;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Engine#setDefaultHost(java.lang.String)
     */
    public void setDefaultHost(String arg0) {
        this.defaultHost = arg0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Engine#setJvmRoute(java.lang.String)
     */
    public void setJvmRoute(String arg0) {
        this.jvmRoute = arg0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Engine#setService(org.apache.catalina.Service)
     */
    public void setService(Service arg0) {
        this.service = arg0;
    }

}
