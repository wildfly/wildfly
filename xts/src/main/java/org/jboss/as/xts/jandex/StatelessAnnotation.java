/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts.jandex;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.xts.XTSException;
import org.jboss.jandex.AnnotationInstance;

import jakarta.ejb.Stateless;

/**
 * @author paul.robinson@redhat.com, 2012-02-06
 */
public class StatelessAnnotation {

    private static final String STATELESS_ANNOTATION = Stateless.class.getName();


    public static StatelessAnnotation build(DeploymentUnit unit, String endpoint) throws XTSException {

        final AnnotationInstance annotationInstance = JandexHelper.getAnnotation(unit, endpoint, STATELESS_ANNOTATION);
        if (annotationInstance == null) {
            return null;
        }

        return new StatelessAnnotation();
    }
}
