/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.invocation.InterceptorContext;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MethodIntfHelper {
    // centralize this hack
    public static MethodIntf of(final InterceptorContext invocation) {
        //for timer invocations there is no view, so the methodInf is attached directly
        //to the context. Otherwise we retrieve it from the invoked view
        MethodIntf methodIntf = invocation.getPrivateData(MethodIntf.class);
        if (methodIntf == null) {
            final ComponentView componentView = invocation.getPrivateData(ComponentView.class);
            if (componentView != null) {
                methodIntf = componentView.getPrivateData(MethodIntf.class);
            } else {
                methodIntf = MethodIntf.BEAN;
            }
        }
        return methodIntf;
    }
}
