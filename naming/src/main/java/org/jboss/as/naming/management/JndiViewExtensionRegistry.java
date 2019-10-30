/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.management;

import java.util.ArrayList;
import java.util.List;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Registry for Jndi view extensions.
 *
 * @author John Bailey
 */
public class JndiViewExtensionRegistry implements Service<JndiViewExtensionRegistry> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("jndi-view", "extension", "registry");
    private List<JndiViewExtension> extensions;

    public synchronized void start(StartContext startContext) throws StartException {
        this.extensions = new ArrayList<JndiViewExtension>();
    }

    public synchronized void stop(StopContext stopContext) {
        this.extensions = null;
    }

    public JndiViewExtensionRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    List<JndiViewExtension> getExtensions() {
        return this.extensions;
    }

    public void addExtension(final JndiViewExtension extension) {
        this.extensions.add(extension);
    }

    public void removeExtension(final JndiViewExtension extension) {
        this.extensions.remove(extension);
    }
}
