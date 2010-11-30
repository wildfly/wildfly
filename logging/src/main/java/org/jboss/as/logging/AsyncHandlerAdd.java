/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.InjectedValue;

import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class AsyncHandlerAdd extends AbstractHandlerAdd {

    private static final long serialVersionUID = 3144252544518106859L;

    private OverflowAction overflowAction;

    private int queueLength;

    protected AsyncHandlerAdd(final String name) {
        super(name);
    }

    protected AbstractHandlerElement<?> createElement(final String name) {
        final AsyncHandlerElement element = new AsyncHandlerElement(name);
        element.setOverflowAction(overflowAction);
        element.setQueueLength(queueLength);
        element.setSubhandlers(getSubhandlers());
        return element;
    }

    public OverflowAction getOverflowAction() {
        return overflowAction;
    }

    public void setOverflowAction(final OverflowAction overflowAction) {
        this.overflowAction = overflowAction;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public void setQueueLength(final int queueLength) {
        this.queueLength = queueLength;
    }

    /**
     * {@inheritDoc}
     */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        try {
            final BatchBuilder builder = updateContext.getBatchBuilder();
            final AsyncHandlerService service = new AsyncHandlerService();
            final ServiceBuilder<Handler> serviceBuilder = builder.addService(LogServices.handlerName(getName()), service);
            final List<InjectedValue<Handler>> list = new ArrayList<InjectedValue<Handler>>();
            for (String handlerName : getSubhandlers()) {
                final InjectedValue<Handler> injectedValue = new InjectedValue<Handler>();
                serviceBuilder.addDependency(LogServices.handlerName(handlerName), Handler.class, injectedValue);
                list.add(injectedValue);
            }
            service.addHandlers(list);
            service.setQueueLength(queueLength);
            service.setLevel(Level.parse(getLevelName()));
            service.setOverflowAction(overflowAction);
            serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
            serviceBuilder.addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param));
            serviceBuilder.install();
        } catch (Throwable t) {
            resultHandler.handleFailure(t, param);
            return;
        }
    }
}
