/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.xts.jandex;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.xts.XTSException;

import jakarta.ejb.TransactionAttribute;
import jakarta.transaction.Transactional;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 * @author <a href="mailto:paul.robinson@redhat.com">Paul Robinson</a>
 */
public class TransactionalAnnotation {

    public static final String[] TRANSACTIONAL_ANNOTATIONS = {
            TransactionAttribute.class.getName(),
            Transactional.class.getName()
    };

    private TransactionalAnnotation() {
    }

    public static TransactionalAnnotation build(DeploymentUnit unit, String endpoint) throws XTSException {
        for (final String annotation : TRANSACTIONAL_ANNOTATIONS) {
            if (JandexHelper.getAnnotation(unit, endpoint, annotation) != null) {
                return new TransactionalAnnotation();
            }
        }

        return null;
    }
}
