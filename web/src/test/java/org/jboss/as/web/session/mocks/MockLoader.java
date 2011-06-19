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

import java.beans.PropertyChangeListener;

import org.apache.catalina.Container;
import org.apache.catalina.Loader;

/**
 * Mock Loader impl for use in unit tests.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 93046 $
 */
public class MockLoader implements Loader {
    private Container container;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub
    }

    public void addRepository(String repository) {
        // TODO Auto-generated method stub
    }

    public void backgroundProcess() {
        // TODO Auto-generated method stub
    }

    public String[] findRepositories() {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] findLoaderRepositories() {
        // TODO Auto-generated method stub
        return null;
    }

    public ClassLoader getClassLoader() {
        return container == null ? getClass().getClassLoader() : container.getClass().getClassLoader();
    }

    public Container getContainer() {
        return container;
    }

    public boolean getDelegate() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getReloadable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean modified() {
        // TODO Auto-generated method stub
        return false;
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        // TODO Auto-generated method stub

    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public void setDelegate(boolean delegate) {
        // TODO Auto-generated method stub

    }

    public void setReloadable(boolean reloadable) {
        // TODO Auto-generated method stub

    }

}
