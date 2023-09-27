/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.msc.service.ServiceName;

/**
 * Jakarta Enterprise Beans specific view configuration.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class EJBViewConfiguration extends ViewConfiguration {

    private final MethodInterfaceType methodIntf;

    public EJBViewConfiguration(final Class<?> viewClass, final ComponentConfiguration componentConfiguration, final ServiceName viewServiceName, final ProxyFactory<?> proxyFactory, final MethodInterfaceType methodIntf) {
        super(viewClass, componentConfiguration, viewServiceName, proxyFactory);
        this.methodIntf = methodIntf;
    }

    public MethodInterfaceType getMethodIntf() {
        return methodIntf;
    }

}
