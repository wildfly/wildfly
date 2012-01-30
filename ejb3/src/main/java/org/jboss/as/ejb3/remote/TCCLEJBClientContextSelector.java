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

/**
 * A {@link TCCLEJBClientContextSelector} is backed by {@link TCCLEJBClientContextSelectorService}
 * which returns an appropriate {@link EJBClientContext} based on the current {@link Thread#getContextClassLoader() context classloader}
 *
 * @author Jaikiran Pai
 */
class TCCLEJBClientContextSelector implements ContextSelector<EJBClientContext> {

    static final TCCLEJBClientContextSelector INSTANCE = new TCCLEJBClientContextSelector();

    private volatile TCCLEJBClientContextSelectorService tcclEJBClientContextService;

    void setTCCLEJBClientContextService(final TCCLEJBClientContextSelectorService clientContextService) {
        this.tcclEJBClientContextService = clientContextService;
    }

    @Override
    public EJBClientContext getCurrent() {
        if (this.tcclEJBClientContextService == null) {
            return null;
        }
        return this.tcclEJBClientContextService.getCurrent();
    }
}
