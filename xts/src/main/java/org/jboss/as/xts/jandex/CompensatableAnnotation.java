/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.xts.jandex;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.xts.XTSException;
import org.jboss.narayana.compensations.api.CancelOnFailure;
import org.jboss.narayana.compensations.api.Compensatable;
import org.jboss.narayana.compensations.api.CompensationScoped;
import org.jboss.narayana.compensations.api.TxCompensate;
import org.jboss.narayana.compensations.api.TxConfirm;
import org.jboss.narayana.compensations.api.TxLogged;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 * @author <a href="mailto:paul.robinson@redhat.com">Paul Robinson</a>
 */
public class CompensatableAnnotation {

    public static final String[] COMPENSATABLE_ANNOTATIONS = {
            Compensatable.class.getName(),
            CancelOnFailure.class.getName(),
            CompensationScoped.class.getName(),
            TxCompensate.class.getName(),
            TxConfirm.class.getName(),
            TxLogged.class.getName()
    };

    private CompensatableAnnotation() {
    }

    public static CompensatableAnnotation build(final DeploymentUnit unit, final String endpoint) throws XTSException {
        for (final String annotation : COMPENSATABLE_ANNOTATIONS) {
            if (JandexHelper.getAnnotation(unit, endpoint, annotation) != null) {
                return new CompensatableAnnotation();
            }
        }

        return null;
    }
}
