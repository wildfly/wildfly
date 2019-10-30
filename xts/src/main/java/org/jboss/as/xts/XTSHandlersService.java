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

package org.jboss.as.xts;

import org.jboss.msc.service.AbstractService;
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
public class XTSHandlersService extends AbstractService<UnifiedHandlerChainMetaData> {
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
