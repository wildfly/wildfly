/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.invocation;

import org.jboss.wsf.spi.invocation.NamespaceContextSelectorWrapper;

/**
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 *
 */
public class NamespaceCtxSelectorWrapperFactory implements org.jboss.wsf.spi.invocation.NamespaceContextSelectorWrapperFactory {

    private static NamespaceContextSelectorWrapper instance = new NamespaceCtxSelectorWrapper();

    @Override
    public NamespaceContextSelectorWrapper getWrapper() {
        return instance;
    }

}
