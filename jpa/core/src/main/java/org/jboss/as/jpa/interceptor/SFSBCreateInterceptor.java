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

package org.jboss.as.jpa.interceptor;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.jpa.container.SFSBXPCMap;
import org.jboss.as.jpa.ejb3.SFSBContextHandleImpl;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;

/**
 * For SFSB life cycle management.
 * Handles the (@PostConstruct time) creation of the extended persistence context (XPC).
 *
 * @author Scott Marlow
 */
public class SFSBCreateInterceptor implements Interceptor {

    private final SFSBXPCMap sfsbxpcMap;

    private SFSBCreateInterceptor(final SFSBXPCMap sfsbxpcMap) {
        this.sfsbxpcMap = sfsbxpcMap;
    }

    @Override
    public Object processInvocation(InterceptorContext interceptorContext) throws Exception {
        StatefulSessionComponentInstance sfsb = (StatefulSessionComponentInstance) interceptorContext.getPrivateData(ComponentInstance.class);
        SFSBContextHandleImpl sfsbContextHandle = new SFSBContextHandleImpl(sfsb.getId());
        sfsbxpcMap.finishRegistrationOfPersistenceContext(sfsbContextHandle);
        return interceptorContext.proceed();
    }


    public static class Factory implements InterceptorFactory {

        private final SFSBXPCMap sfsbxpcMap;

        public Factory(final SFSBXPCMap sfsbxpcMap) {
            this.sfsbxpcMap = sfsbxpcMap;
        }

        @Override
        public Interceptor create(final InterceptorFactoryContext context) {
            return new SFSBCreateInterceptor(sfsbxpcMap);
        }
    }
}
