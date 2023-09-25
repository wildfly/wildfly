/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
