/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component.concurrent;

import org.jboss.as.ee.concurrent.handle.ContextHandle;
import org.jboss.as.ee.concurrent.handle.ContextHandleFactory;
import org.jboss.as.ejb3.context.CurrentInvocationContext;
import org.jboss.invocation.InterceptorContext;

import javax.enterprise.concurrent.ContextService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * The context handle factory responsible for saving and setting the ejb context.
 * @author Eduardo Martins
 */
public class EJBContextHandleFactory implements ContextHandleFactory {

    public static final String NAME = "EJB";

    public static final EJBContextHandleFactory INSTANCE = new EJBContextHandleFactory();

    private EJBContextHandleFactory() {
    }

    @Override
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        return new EJBContextHandle();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getChainPriority() {
        return 500;
    }

    @Override
    public void writeHandle(ContextHandle contextHandle, ObjectOutputStream out) throws IOException {
        out.writeObject(contextHandle);
    }

    @Override
    public ContextHandle readHandle(ObjectInputStream in) throws IOException, ClassNotFoundException {
        return (ContextHandle) in.readObject();
    }

    private static class EJBContextHandle implements ContextHandle {

        private final transient InterceptorContext interceptorContext;

        private EJBContextHandle() {
            interceptorContext = CurrentInvocationContext.get();
        }

        @Override
        public String getFactoryName() {
            return NAME;
        }

        @Override
        public void setup() throws IllegalStateException {
            if(interceptorContext != null) {
                CurrentInvocationContext.push(interceptorContext);
            }
        }

        @Override
        public void reset() {
            if(interceptorContext != null) {
                CurrentInvocationContext.pop();
            }
        }
    }
}
