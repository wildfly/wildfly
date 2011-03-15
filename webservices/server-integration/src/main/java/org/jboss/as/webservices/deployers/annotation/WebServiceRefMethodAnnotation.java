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
package org.jboss.as.webservices.deployers.annotation;

import org.jboss.as.ee.component.InjectionTargetDescription;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

/**
 * WebServiceRefProcessor for annotated methods.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class WebServiceRefMethodAnnotation extends AbstractWebServiceRefAnnotation<MethodInfo> {

    public WebServiceRefMethodAnnotation(CompositeIndex index) {
        super(index);
    }

    @Override
    public String getName(MethodInfo methodInfo) {
        String name = methodInfo.name().substring(3);
        if (name.length() > 1) {
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
        } else {
            name = name.toLowerCase();
        }
        // declaringClass / name
        return methodInfo.declaringClass().name().toString() + "/" + name;
    }

    @Override
    public InjectionTargetDescription.Type getInjectionType() {
        return InjectionTargetDescription.Type.METHOD;
    }

    @Override
    protected String getInjectionName(MethodInfo methodInfo) {
        return methodInfo.name();
    }

    @Override
    protected String getType(MethodInfo methodInfo) {
        if (methodInfo.args().length != 1)
            throw new IllegalStateException("The method requires one parameter: " + methodInfo.name());

        return methodInfo.args()[0].name().toString();
    }

    @Override
    protected ClassInfo getTypeInfo(MethodInfo methodInfo) {
        if (methodInfo.args().length != 1)
            throw new IllegalStateException("The method requires one parameter: " + methodInfo.name());

        return index.getClassByName(methodInfo.args()[0].name());
    }

    @Override
    public String getDeclaringClass(MethodInfo methodInfo) {
        return methodInfo.declaringClass().name().toString();
    }
}
