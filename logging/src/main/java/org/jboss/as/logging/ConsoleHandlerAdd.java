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

import java.io.UnsupportedEncodingException;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ConsoleHandlerAdd extends AbstractHandlerAdd {

    private static final long serialVersionUID = -4769503787024853339L;

    private Target target = Target.SYSTEM_OUT;

    public ConsoleHandlerAdd(final String name) {
        super(name);
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(final Target target) {
        this.target = target;
    }

    protected AbstractHandlerElement<?> createElement(final String name) {
        final ConsoleHandlerElement element = new ConsoleHandlerElement(name);
        element.setTarget(target);
        return element;
    }

    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> handler, final P param) {
        try {
            final ServiceTarget target = updateContext.getServiceTarget();
            final ConsoleHandlerService service = new ConsoleHandlerService();
            final ServiceBuilder<Handler> serviceBuilder = target.addService(LogServices.handlerName(getName()), service);
            service.setLevel(Level.parse(getLevelName()));
            final Boolean autoFlush = getAutoflush();
            if (autoFlush != null) service.setAutoflush(autoFlush.booleanValue());
            try {
                service.setEncoding(getEncoding());
            } catch (UnsupportedEncodingException e) {
                handler.handleFailure(e, param);
                return;
            }
            service.setFormatterSpec(getFormatter());
            serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
            serviceBuilder.addListener(new UpdateResultHandler.ServiceStartListener<P>(handler, param));
            serviceBuilder.install();
        } catch (Throwable t) {
            handler.handleFailure(t, param);
            return;
        }
    }
}
