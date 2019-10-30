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

package org.jboss.as.ee.component;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.naming.ContextListManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.msc.inject.InjectionException;

/**
 * A managed reference factory for a component view.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ViewManagedReferenceFactory implements ContextListManagedReferenceFactory {
    private final ComponentView view;

    /**
     * Construct a new instance.
     *
     * @param view the component view
     */
    public ViewManagedReferenceFactory(final ComponentView view) {
        this.view = view;
    }

    @Override
    public String getInstanceClassName() {
        return view.getComponent().getComponentClass().getName();
    }

    /** {@inheritDoc} */
    public ManagedReference getReference() {
        try {
            return view.createInstance();
        } catch (Exception e) {
            throw EeLogger.ROOT_LOGGER.componentViewConstructionFailure(e);
        }
    }

    /**
     * The bridge injector for binding views into JNDI.  Injects a {@link ComponentView}
     * wrapped as a {@link ManagedReferenceFactory}.
     */
    public static class Injector implements org.jboss.msc.inject.Injector<ComponentView> {
        private final org.jboss.msc.inject.Injector<ManagedReferenceFactory> referenceFactoryInjector;

        /**
         * Construct a new instance.
         *
         * @param referenceFactoryInjector the injector from the binder service
         */
        public Injector(final org.jboss.msc.inject.Injector<ManagedReferenceFactory> referenceFactoryInjector) {
            this.referenceFactoryInjector = referenceFactoryInjector;
        }

        /** {@inheritDoc} */
        public void inject(final ComponentView value) throws InjectionException {
            referenceFactoryInjector.inject(new ViewManagedReferenceFactory(value));
        }

        /** {@inheritDoc} */
        public void uninject() {
            referenceFactoryInjector.uninject();
        }
    }

}
