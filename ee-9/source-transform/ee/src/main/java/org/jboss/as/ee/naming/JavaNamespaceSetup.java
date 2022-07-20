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
package org.jboss.as.ee.naming;

import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.deployment.SetupAction;
import org.jboss.msc.service.ServiceName;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Sets and restores the <code>java:</code> contexts
 *
 * @author Stuart Douglas
 *
 */
public class JavaNamespaceSetup implements SetupAction {

    private final NamespaceContextSelector namespaceSelector;
    private final ServiceName deploymentUnitServiceName;

    public JavaNamespaceSetup(final NamespaceContextSelector namespaceSelector, final ServiceName deploymentUnitServiceName) {
        this.namespaceSelector = namespaceSelector;
        this.deploymentUnitServiceName = deploymentUnitServiceName;
    }

    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public Set<ServiceName> dependencies() {
        return Collections.emptySet();
    }

    @Override
    public void setup(Map<String, Object> properties) {
        NamespaceContextSelector.pushCurrentSelector(namespaceSelector);
        WritableServiceBasedNamingStore.pushOwner(deploymentUnitServiceName);
    }

    @Override
    public void teardown(Map<String, Object> properties) {
        NamespaceContextSelector.popCurrentSelector();
        WritableServiceBasedNamingStore.popOwner();
    }

}
