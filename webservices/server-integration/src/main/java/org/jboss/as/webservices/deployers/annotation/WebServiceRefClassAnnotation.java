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

import org.jboss.as.ee.component.InjectionTarget;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.ClassInfo;

/**
 * WebServiceRefProcessor for annotated classes.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class WebServiceRefClassAnnotation extends AbstractWebServiceRefAnnotation<ClassInfo> {

    public WebServiceRefClassAnnotation(CompositeIndex index) {
        super(index);
    }

    @Override
    public String getName(ClassInfo classInfo) {
        throw new IllegalStateException("@WebServiceRef annotation on type '" + classInfo.name() + "' must define a name.");
    }

    @Override
    public InjectionTarget.Type getInjectionType() {
        return null;
    }

    @Override
    protected String getInjectionName(ClassInfo classInfo) {
        return null;
    }

    @Override
    protected String getType(ClassInfo classInfo) {
        throw new IllegalStateException("@WebServiceRef annotation on type '" + classInfo.name() + "' must define a type.");
    }

    @Override
    protected ClassInfo getTypeInfo(ClassInfo classInfo) {
        return null;
    }

    @Override
    protected String getDeclaringClass(ClassInfo classInfo) {
        return classInfo.name().toString();
    }

}
