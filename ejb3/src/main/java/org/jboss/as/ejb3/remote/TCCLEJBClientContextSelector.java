/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;

/**
 * A {@link TCCLEJBClientContextSelector} is backed by {@link TCCLEJBClientContextSelectorService}
 * which returns an appropriate {@link EJBClientContext} based on the current {@link Thread#getContextClassLoader() context classloader}
 *
 * @author Jaikiran Pai
 */
class TCCLEJBClientContextSelector implements ContextSelector<EJBClientContext> {

    private static final Logger logger = Logger.getLogger(TCCLEJBClientContextSelector.class);

    static final TCCLEJBClientContextSelector INSTANCE = new TCCLEJBClientContextSelector();

    private volatile TCCLEJBClientContextSelectorService tcclEJBClientContextService;
    private volatile EJBClientContext defaultEJBClientContext;

    /**
     * Sets the TCCL based client context service, which will be used to query for EJB client context, and the
     * default EJB client context which will be used when there's no EJB client context associated with the
     * thread context classloader when {@link #getCurrent()} is invoked.
     *
     * @param clientContextService    The {@link TCCLEJBClientContextSelectorService}
     * @param defaultEJBClientContext The default EJB client context to fallback on when the {@link org.jboss.as.ejb3.remote.TCCLEJBClientContextSelectorService#getCurrent()}
     *                                returns null
     */
    void setup(final TCCLEJBClientContextSelectorService clientContextService, final EJBClientContext defaultEJBClientContext) {
        this.tcclEJBClientContextService = clientContextService;
        this.defaultEJBClientContext = defaultEJBClientContext;
    }

    /**
     * Cleans up any reference to a the TCCL based context service and the default EJB client context
     */
    void destroy() {
        this.tcclEJBClientContextService = null;
        this.defaultEJBClientContext = null;
    }

    @Override
    public EJBClientContext getCurrent() {
        if (this.tcclEJBClientContextService == null) {
            return null;
        }
        final EJBClientContext ejbClientContext = this.tcclEJBClientContextService.getCurrent();
        if (ejbClientContext != null) {
            return ejbClientContext;
        }

        // explicit isDebugEnabled() check to ensure that the SecurityActions.getContextClassLoader() isn't
        // unnecessarily executed when debug logging is disabled
        if (logger.isDebugEnabled()) {
            logger.debug("Returning default EJB client context " + this.defaultEJBClientContext + " since no EJB client context could be found for TCCL " + SecurityActions.getContextClassLoader());
        }
        return this.defaultEJBClientContext;
    }
}
