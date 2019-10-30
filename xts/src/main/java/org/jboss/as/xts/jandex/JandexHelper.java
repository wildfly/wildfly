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

package org.jboss.as.xts.jandex;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import java.util.List;


/**
 * @author paul.robinson@redhat.com, 2012-02-06
 */
public class JandexHelper {

    public static AnnotationInstance getAnnotation(DeploymentUnit unit, String endpoint, String annotationClassName) {

        final List<AnnotationInstance> annotations = ASHelper.getAnnotations(unit, DotName.createSimple(annotationClassName));
        for (AnnotationInstance annotationInstance : annotations) {

            Object target = annotationInstance.target();

            if (target instanceof ClassInfo) {
                final ClassInfo classInfo = (ClassInfo) target;
                final String endpointClass = classInfo.name().toString();

                if (endpointClass.equals(endpoint)) {
                    return annotationInstance;
                }
            } else if (target instanceof MethodInfo) {
                final MethodInfo methodInfo = (MethodInfo) target;
                final String endpointClass = methodInfo.declaringClass().name().toString();

                if (endpointClass.equals(endpoint)) {
                    return annotationInstance;
                }
            }
        }

        return null;
    }
}
