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
import org.jboss.as.xts.XTSException;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class OldTransactionalAnnotation {

    private final BridgeType bridgeType;

    public static final String[] TRANSACTIONAL_ANNOTATIONS = {
            "org.jboss.narayana.txframework.api.annotation.transaction.Transactional",
    };

    private OldTransactionalAnnotation(final BridgeType bridgeType) {
        this.bridgeType = bridgeType;
    }

    public static OldTransactionalAnnotation build(final DeploymentUnit unit, final String endpoint) throws XTSException {
        for (final String annotation : TRANSACTIONAL_ANNOTATIONS) {
            final AnnotationInstance annotationInstance = JandexHelper.getAnnotation(unit, endpoint, annotation);

            if (annotationInstance != null) {
                final BridgeType bridgeType = BridgeType.build(annotationInstance);
                return new OldTransactionalAnnotation(bridgeType);
            }
        }

        return null;
    }

    public BridgeType getBridgeType() {
        return bridgeType;
    }

}
