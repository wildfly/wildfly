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
