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

package org.jboss.as.ee.component;

import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import javax.interceptor.InvocationContext;

/**
 * Information about an AroundInvoke or lifecycle interceptor method
 *
 * @author Stuart Douglas
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class InterceptorMethodDescription {

    private final MethodIdentifier identifier;
    private final String declaringClass;
    private final boolean acceptsContext;

    /**
     * Construct a new instance.
     *
     * @param declaringClass the class that declared the interceptor method
     * @param identifier the interceptor method identifier
     */
    public InterceptorMethodDescription(String declaringClass, MethodIdentifier identifier) {
        this.declaringClass = declaringClass;
        this.identifier = identifier;
        final String[] parameterTypes = identifier.getParameterTypes();
        acceptsContext = parameterTypes.length == 1 && parameterTypes[0].equals(InvocationContext.class.getName());
    }

    /**
     *
     * @return The class that declared the method
     */
    public String getDeclaringClass() {
        return declaringClass;
    }

    /**
     * Determine whether the method accepts an invocation context (EJB-style).
     *
     * @return {@code true} if the method accepts an invocation context, {@code false} otherwise
     */
    public boolean isAcceptsContext() {
        return acceptsContext;
    }

    /**
     * Get the method identifier of this interceptor method.
     *
     * @return the method identifier
     */
    public MethodIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Create an interceptor method description.
     *
     * @param declaringClass the name of the declaring class
     * @param methodInfo the jandex method info
     * @return the interceptor method description
     */
    public static InterceptorMethodDescription create(String declaringClass, MethodInfo methodInfo) {
        final String[] argTypes = new String[methodInfo.args().length];
        int i = 0;
        for (Type argType : methodInfo.args()) {
            argTypes[i++] = argType.name().toString();
        }
        MethodIdentifier identifier =  MethodIdentifier.getIdentifier(methodInfo.returnType().name().toString(), methodInfo.name(), argTypes);
        return new InterceptorMethodDescription(declaringClass, identifier);
    }

    /**
     * Create an interceptor method description.
     *
     * @param methodInfo the jandex method info
     * @return the interceptor method description
     */
    public static InterceptorMethodDescription create(MethodInfo methodInfo) {
        return create(methodInfo.declaringClass().name().toString(), methodInfo);
    }
}
