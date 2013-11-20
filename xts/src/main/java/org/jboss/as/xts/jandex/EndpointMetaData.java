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

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 * @author <a href="mailto:paul.robinson@redhat.com">Paul Robinson</a>
 */
public class EndpointMetaData {

    private final TransactionalAnnotation transactionalAnnotation;
    private final CompensatableAnnotation compensatableAnnotation;
    private final OldCompensatableAnnotation oldCompensatableAnnotation;
    private final StatelessAnnotation statelessAnnotation;
    private final WebServiceAnnotation webServiceAnnotation;

    private EndpointMetaData(final StatelessAnnotation statelessAnnotation,
            final TransactionalAnnotation transactionalAnnotation, final CompensatableAnnotation compensatableAnnotation,
            final OldCompensatableAnnotation oldCompensatableAnnotation,
            final WebServiceAnnotation webServiceAnnotation) {

        this.statelessAnnotation = statelessAnnotation;
        this.transactionalAnnotation = transactionalAnnotation;
        this.compensatableAnnotation = compensatableAnnotation;
        this.oldCompensatableAnnotation = oldCompensatableAnnotation;
        this.webServiceAnnotation = webServiceAnnotation;
    }

    public static EndpointMetaData build(final DeploymentUnit unit, final String endpoint) throws XTSException {
        final TransactionalAnnotation transactionalAnnotation = TransactionalAnnotation.build(unit, endpoint);
        final CompensatableAnnotation compensatableAnnotation = CompensatableAnnotation.build(unit, endpoint);
        final OldCompensatableAnnotation oldCompensatableAnnotation = OldCompensatableAnnotation.build(unit, endpoint);
        final StatelessAnnotation statelessAnnotation = StatelessAnnotation.build(unit, endpoint);
        final WebServiceAnnotation webServiceAnnotation = WebServiceAnnotation.build(unit, endpoint);

        return new EndpointMetaData(statelessAnnotation, transactionalAnnotation, compensatableAnnotation, oldCompensatableAnnotation, webServiceAnnotation);
    }

    public WebServiceAnnotation getWebServiceAnnotation() {
        return webServiceAnnotation;
    }

    public boolean isWebservice() {
        return webServiceAnnotation != null;
    }

    public boolean isEJB() {
        return statelessAnnotation != null;
    }

    public boolean isBridgeEnabled() {
        return transactionalAnnotation != null;
    }

    public boolean isXTSEnabled() {
        return transactionalAnnotation != null || compensatableAnnotation != null
                || oldCompensatableAnnotation != null;
    }
}
