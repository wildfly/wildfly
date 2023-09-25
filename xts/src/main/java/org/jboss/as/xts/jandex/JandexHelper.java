/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
