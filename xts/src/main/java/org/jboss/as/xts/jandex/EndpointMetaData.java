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
 * @author paul.robinson@redhat.com, 2012-02-06
 */
public class EndpointMetaData {

    private TransactionalAnnotation transactionalAnnotation;
    private CompensatableAnnotation compensatableAnnotation;
    private OldCompensatableAnnotation oldCompensatableAnnotation;
    private WebServiceAnnotation webServiceAnnotation;
    private StatelessAnnotation statelessAnnotation;

    private boolean isTXFrameworkEnabled;

    private EndpointMetaData(StatelessAnnotation statelessAnnotation, TransactionalAnnotation transactionalAnnotation, OldCompensatableAnnotation oldCompensatableAnnotation, CompensatableAnnotation compensatableAnnotation, WebServiceAnnotation webServiceAnnotation, boolean isTXFrameworkEnabled) {
        this.statelessAnnotation = statelessAnnotation;
        this.transactionalAnnotation = transactionalAnnotation;
        this.compensatableAnnotation = compensatableAnnotation;
        this.oldCompensatableAnnotation = oldCompensatableAnnotation;
        this.webServiceAnnotation = webServiceAnnotation;
        this.isTXFrameworkEnabled = isTXFrameworkEnabled;
    }

    public static EndpointMetaData build(DeploymentUnit unit, String endpoint) throws XTSException {
        final StatelessAnnotation statelessAnnotation = StatelessAnnotation.build(unit, endpoint);
        final TransactionalAnnotation transactionalAnnotation = TransactionalAnnotation.build(unit, endpoint);
        final CompensatableAnnotation compensatableAnnotation = CompensatableAnnotation.build(unit, endpoint);
        final OldCompensatableAnnotation oldCompensatableAnnotation = OldCompensatableAnnotation.build(unit, endpoint);
        final WebServiceAnnotation webServiceAnnotation = WebServiceAnnotation.build(unit, endpoint);
        final boolean isTXFrameworkEnabled = (transactionalAnnotation != null || oldCompensatableAnnotation != null || compensatableAnnotation != null);

        return new EndpointMetaData(statelessAnnotation, transactionalAnnotation, oldCompensatableAnnotation, compensatableAnnotation, webServiceAnnotation, isTXFrameworkEnabled);
    }

    public TransactionalAnnotation getTransactionalAnnotation() {
        return transactionalAnnotation;
    }

    public OldCompensatableAnnotation getOldCompensatableAnnotation() {
        return oldCompensatableAnnotation;
    }

    public CompensatableAnnotation getCompensatableAnnotation() {
        return compensatableAnnotation;
    }

    public WebServiceAnnotation getWebServiceAnnotation() {
        return webServiceAnnotation;
    }

    public boolean isTXFrameworkEnabled() {
        return isTXFrameworkEnabled;
    }

    public boolean isWebservice() {
        return webServiceAnnotation != null;
    }

    public boolean isEJB() {
        return statelessAnnotation != null;
    }
}
