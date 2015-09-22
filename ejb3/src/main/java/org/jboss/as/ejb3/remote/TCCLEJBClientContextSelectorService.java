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

import java.util.WeakHashMap;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.common.selector.Selector;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An EJB client context selector which returns an {@link EJBClientContext} based on the thread context classloader
 * that's present when the {@link #get()} is invoked.
 *
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 *
 * @author Jaikiran Pai
 *
 */
public class TCCLEJBClientContextSelectorService extends Selector<EJBClientContext> implements Service<TCCLEJBClientContextSelectorService> {

    public static final ServiceName TCCL_BASED_EJB_CLIENT_CONTEXT_SELECTOR_SERVICE_NAME = ServiceName.JBOSS.append("ejb").append("remote").append("tccl-based-ejb-client-context-selector");

    /**
     * EJB client contexts mapped against the classloader
     */
    private final WeakHashMap<ClassLoader, EJBClientContext> ejbClientContexts = new WeakHashMap<ClassLoader, EJBClientContext>();

//    private final ConcurrentMap<EJBClientContextIdentifier, EJBClientContext> identifiableContexts = new ConcurrentHashMap<EJBClientContextIdentifier, org.jboss.ejb.client.EJBClientContext>();

    @Override
    public EJBClientContext get() {
        final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        // TODO: Do we fall back on some other CL?
        if (tccl == null) {
            throw EjbLogger.ROOT_LOGGER.tcclNotAvailable();
        }
        synchronized (this.ejbClientContexts) {
            return this.ejbClientContexts.get(tccl);
        }
    }

    @Override
    public void start(StartContext context) throws StartException {
        Selector.setSelectorFor(EJBClientContext.class, this);
    }

    @Override
    public void stop(StopContext context) {
        // clear the contexts
        synchronized (this.ejbClientContexts) {
            this.ejbClientContexts.clear();
        }
//        this.identifiableContexts.clear();
    }

    @Override
    public TCCLEJBClientContextSelectorService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Associates a {@link EJBClientContext} with the passed <code>classLoader</code>.
     *
     * @param ejbClientContext The EJB client context
     * @param classLoader      The classloader with which the EJB client context has to be associated
     */
    public void registerEJBClientContext(final EJBClientContext ejbClientContext, final ClassLoader classLoader) {
        synchronized (this.ejbClientContexts) {
            this.ejbClientContexts.put(classLoader, ejbClientContext);
        }
    }

    /**
     * Unassociates a {@link EJBClientContext} with the passed <code>classLoader</code>.
     *
     * @param classLoader      The classloader with which the EJB client context has to be associated
     */
    public EJBClientContext unRegisterEJBClientContext(final ClassLoader classLoader) {
        synchronized (this.ejbClientContexts) {
            return this.ejbClientContexts.remove(classLoader);
        }
    }

}
