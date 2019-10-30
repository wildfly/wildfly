/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
