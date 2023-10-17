/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.wildfly.extension.messaging.activemq.BinderServiceUtil.installBinderService;
import static org.wildfly.extension.messaging.activemq._private.MessagingLogger.ROOT_LOGGER;

import java.util.concurrent.CountDownLatch;
import java.util.Locale;

import org.apache.activemq.artemis.spi.core.naming.BindingRegistry;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

/**
 * A {@link BindingRegistry} implementation for WildFly.
 *
 * @author Jason T. Greene
 * @author Jaikiran Pai
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WildFlyBindingRegistry implements BindingRegistry {

    private final ServiceContainer container;

    public WildFlyBindingRegistry(ServiceContainer container) {
        this.container = container;
    }

    // This method is called by ActiveMQ when JNDI entries for its resources
    // are updated using its own management API. We advise against using it in
    // WildFly (and use WildFly own management API) but we must still respect the
    // SPI contract for this method
    @Override
    public Object lookup(String name) {
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);
        ServiceController<?> bindingService = container.getService(bindInfo.getBinderServiceName());
        if (bindingService == null) {
            return null;
        }
        ManagedReferenceFactory managedReferenceFactory = ManagedReferenceFactory.class.cast(bindingService.getValue());
        return managedReferenceFactory.getReference().getInstance();
    }

    @Override
    public boolean bind(String name, Object obj) {
        if (name == null || name.isEmpty()) {
            throw ROOT_LOGGER.cannotBindJndiName();
        }
        installBinderService(container, name, obj);
        ROOT_LOGGER.boundJndiName(name);
        return true;
    }

    /**
     * Unbind the resource and wait until the corresponding binding service is effectively removed.
     */
    @Override
    public void unbind(String name) {
        if (name == null || name.isEmpty()) {
            throw ROOT_LOGGER.cannotUnbindJndiName();
        }
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);
        ServiceController<?> bindingService = container.getService(bindInfo.getBinderServiceName());
        if (bindingService == null) {
            ROOT_LOGGER.debugf("Cannot unbind %s since no binding exists with that name", name);
            return;
        }
        // remove the binding service
        final CountDownLatch latch = new CountDownLatch(1);
        bindingService.addListener(new LifecycleListener() {
            @Override
            public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                if (event == LifecycleEvent.REMOVED) {
                    ROOT_LOGGER.unboundJndiName(bindInfo.getAbsoluteJndiName());
                    latch.countDown();
                }
            }
        });
        try {
            bindingService.setMode(ServiceController.Mode.REMOVE);
        } finally {
            try {
                latch.await();
            } catch (InterruptedException ie) {
                ROOT_LOGGER.failedToUnbindJndiName(name, 5, SECONDS.toString().toLowerCase(Locale.US));
            }
        }
    }

    @Override
    public void close() {
        // NOOP
    }
}
