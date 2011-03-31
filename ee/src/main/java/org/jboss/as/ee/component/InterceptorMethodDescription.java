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

/**
 * Information about an AroundInvoke or lifecycle interceptor method
 *
 * @author Stuart Douglas
 */
public class InterceptorMethodDescription {

    private final MethodIdentifier identifier;
    private final String declaringClass;
    private final String instanceClass;
    private final boolean declaredOnTargetClass;


    /**
     *
     * @param declaringClass The class that declared the method
     * @param instanceClass The class that should be instantiated, this may be a sub class on the declaring class
     * @param identifier The method identifier
     * @param declaredOnTargetClass
     */
    public InterceptorMethodDescription(String declaringClass, String instanceClass, MethodIdentifier identifier, final boolean declaredOnTargetClass) {
        this.declaringClass = declaringClass;
        this.identifier = identifier;
        this.declaredOnTargetClass = declaredOnTargetClass;
        this.instanceClass = instanceClass;
    }

    /**
     *
     * @return The class that declared the method
     */
    public String getDeclaringClass() {
        return declaringClass;
    }

    /**
     *
     * @return The class that should be instantiated, this may be a sub class on the declaring class
     */
    public String getInstanceClass() {
        return  instanceClass;
    }

    public MethodIdentifier getIdentifier() {
        return identifier;
    }

    public static InterceptorMethodDescription create(String declaringClass, String instanceClass, MethodInfo methodInfo, final boolean declaredOnTargetClass) {
        final String[] argTypes = new String[methodInfo.args().length];
        int i = 0;
        for (Type argType : methodInfo.args()) {
            argTypes[i++] = argType.name().toString();
        }
        MethodIdentifier identifier =  MethodIdentifier.getIdentifier(methodInfo.returnType().name().toString(), methodInfo.name(), argTypes);
        return new InterceptorMethodDescription(declaringClass,instanceClass,identifier,declaredOnTargetClass);
    }

    public boolean isDeclaredOnTargetClass() {
        return declaredOnTargetClass;
    }

}
