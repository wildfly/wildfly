/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerChainMetaData;

/**
 * A service providing metadata for the ws handlers contributed by XTS subsystem
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public class XTSHandlersService implements Service<UnifiedHandlerChainMetaData> {
    private final boolean isDefaultContextPropagation;
    private volatile UnifiedHandlerChainMetaData handlerChainMetaData;

    private XTSHandlersService(boolean isDefaultContextPropagation) {
        this.isDefaultContextPropagation = isDefaultContextPropagation;
    }

    @Override
    public UnifiedHandlerChainMetaData getValue() throws IllegalStateException {
        return handlerChainMetaData;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        final XTSHandlersManager xtsHandlerManager = new XTSHandlersManager(isDefaultContextPropagation);
        handlerChainMetaData = xtsHandlerManager.getHandlerChain();
    }

    public void stop(final StopContext context) {
        handlerChainMetaData = null;
    }

    public static void install(final ServiceTarget target, final boolean isDefaultContextPropagation) {
        final XTSHandlersService xtsHandlersService = new XTSHandlersService(isDefaultContextPropagation);
        ServiceBuilder<?> builder = target.addService(XTSServices.JBOSS_XTS_HANDLERS, xtsHandlersService);
        builder.setInitialMode(Mode.ACTIVE);
        builder.install();
    }
}
