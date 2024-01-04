/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
