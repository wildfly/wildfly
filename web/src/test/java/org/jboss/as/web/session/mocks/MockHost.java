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

import org.apache.catalina.Context;
import org.apache.catalina.Host;

/**
 * @author Brian Stansberry
 * 
 */
public class MockHost extends MockContainer implements Host {
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#addAlias(java.lang.String)
     */
    public void addAlias(String arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#findAliases()
     */
    public String[] findAliases() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#getAppBase()
     */
    public String getAppBase() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#getAutoDeploy()
     */
    public boolean getAutoDeploy() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#getConfigClass()
     */
    public String getConfigClass() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#getDeployOnStartup()
     */
    public boolean getDeployOnStartup() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#getXmlNamespaceAware()
     */
    public boolean getXmlNamespaceAware() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#getXmlValidation()
     */
    public boolean getXmlValidation() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#map(java.lang.String)
     */
    public Context map(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#removeAlias(java.lang.String)
     */
    public void removeAlias(String arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#setAppBase(java.lang.String)
     */
    public void setAppBase(String arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#setAutoDeploy(boolean)
     */
    public void setAutoDeploy(boolean arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#setConfigClass(java.lang.String)
     */
    public void setConfigClass(String arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#setDeployOnStartup(boolean)
     */
    public void setDeployOnStartup(boolean arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#setXmlNamespaceAware(boolean)
     */
    public void setXmlNamespaceAware(boolean arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.catalina.Host#setXmlValidation(boolean)
     */
    public void setXmlValidation(boolean arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getDefaultWebapp() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDefaultWebapp(String defaultWebapp) {
        // TODO Auto-generated method stub

    }

}
