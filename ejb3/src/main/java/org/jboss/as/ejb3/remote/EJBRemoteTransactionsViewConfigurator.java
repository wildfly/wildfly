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

package org.jboss.as.ejb3.remote;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ViewConfiguration;
import org.jboss.as.ee.component.ViewConfigurator;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.component.interceptors.InterceptorOrder;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;

/**
 * Responsible for setting up a {@link EJBRemoteTransactionPropagatingInterceptor} for a (remote) view.
 * This view configurator does <i>not</i> check whether the view is remote or not. It's the responsibility
 * of whoever sets up this view configurator to make sure that the view being configured is a remote view
 *
 * @author Jaikiran Pai
 */
public class EJBRemoteTransactionsViewConfigurator implements ViewConfigurator {

    @Override
    public void configure(final DeploymentPhaseContext context, final ComponentConfiguration componentConfiguration,
                          final ViewDescription viewDescription, final ViewConfiguration viewConfiguration) throws DeploymentUnitProcessingException {
        // setup a view interceptor which propagates remote transactions. This interceptor
        // should appear before the CMT/BMT interceptors so that the latter interceptors know about any existing
        // tx for the invocation
        viewConfiguration.addViewInterceptor(EJBRemoteTransactionPropagatingInterceptorFactory.INSTANCE, InterceptorOrder.View.REMOTE_TRANSACTION_PROPAGATION_INTERCEPTOR);

    }
}
