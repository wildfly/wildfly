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

package org.jboss.as.cmp.component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import javax.ejb.EJBException;
import org.jboss.as.cmp.CmpMessages;

/**
 * @author John Bailey
 */
public class CmpEntityBeanInvocationHandler implements InvocationHandler {

    private static final Method SET_COMPONENT_INSTANCE;

    static {
        try {
            SET_COMPONENT_INSTANCE = CmpProxy.class.getMethod("setComponentInstance", CmpEntityBeanComponentInstance.class);
        } catch (NoSuchMethodException ignored) {
            throw CmpMessages.MESSAGES.missingSetComponentInstanceMethodOnCmpProxy();
        }
    }

    private final CmpEntityBeanComponent component;
    private CmpEntityBeanComponentInstance componentInstance;

    public CmpEntityBeanInvocationHandler(final CmpEntityBeanComponent component) {
        this.component = component;
    }

    public Object invoke(final Object object, final Method method, final Object[] objects) throws Throwable {
        if (SET_COMPONENT_INSTANCE.equals(method)) {
            componentInstance = (CmpEntityBeanComponentInstance) objects[0];
            return null;
        }

        if (componentInstance == null) {
            throw CmpMessages.MESSAGES.noComponentInstanceSetOnProxy();
        }
        return component.getStoreManager().getInvocationHandler().invoke(componentInstance, object, method, objects);
    }
}
