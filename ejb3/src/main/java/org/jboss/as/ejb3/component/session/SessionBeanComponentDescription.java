/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.session;

import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jaikiran Pai
 */
public abstract class SessionBeanComponentDescription extends EJBComponentDescription {

    /**
     * Flag marking the presence/absence of a no-interface view on the session bean
     */
    private boolean noInterfaceViewPresent;

    private Map<String, MethodIntf> viewTypes = new HashMap<String, MethodIntf>();

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param moduleName         the module name
     * @param applicationName    the application name
     */
    public SessionBeanComponentDescription(final String componentName, final String componentClassName, final String moduleName, final String applicationName) {
        super(componentName, componentClassName, moduleName, applicationName);
    }

    public void addLocalBusinessInterfaceViews(Collection<String> classNames) {
        this.getViewClassNames().addAll(classNames);
    }

    public void addNoInterfaceView() {
        this.noInterfaceViewPresent = true;
        this.getViewClassNames().add(this.getEJBClassName());
        viewTypes.put(getEJBClassName(), MethodIntf.LOCAL);
    }

    @Override
    public MethodIntf getMethodIntf(String viewClassName) {
        MethodIntf methodIntf = viewTypes.get(viewClassName);
        assert methodIntf != null : "no view type known for " + viewClassName;
        return methodIntf;
    }

    public boolean hasNoInterfaceView() {
        return this.noInterfaceViewPresent;
    }
}
