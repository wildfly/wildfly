/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
