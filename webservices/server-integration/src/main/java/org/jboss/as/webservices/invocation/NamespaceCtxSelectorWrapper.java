/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.invocation;

import java.util.Map;

import org.jboss.as.naming.context.NamespaceContextSelector;

/**
 * A wrapper of the NamespaceContextSelector for copying/reading it to/from
 * a provided map (usually message context / exchange from ws stack)
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 *
 */
public class NamespaceCtxSelectorWrapper implements org.jboss.wsf.spi.invocation.NamespaceContextSelectorWrapper {

    private static final String KEY = org.jboss.wsf.spi.invocation.NamespaceContextSelectorWrapper.class.getName();

    @Override
    public void storeCurrentThreadSelector(Map<String, Object> map) {
        map.put(KEY, NamespaceContextSelector.getCurrentSelector());
    }

    @Override
    public void setCurrentThreadSelector(Map<String, Object> map) {
        NamespaceContextSelector.pushCurrentSelector((NamespaceContextSelector)map.get(KEY));
    }

    @Override
    public void clearCurrentThreadSelector(Map<String, Object> map) {
        if (map.containsKey(KEY)) {
            map.remove(KEY);
            NamespaceContextSelector.popCurrentSelector();
        }
    }

}
