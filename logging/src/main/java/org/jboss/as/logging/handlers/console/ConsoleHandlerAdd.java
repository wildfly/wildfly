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

package org.jboss.as.logging.handlers.console;

import static org.jboss.as.logging.CommonAttributes.TARGET;

import java.util.List;
import java.util.logging.Handler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.logging.handlers.FlushingHandlerAddProperties;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 */
public class ConsoleHandlerAdd extends FlushingHandlerAddProperties<ConsoleHandlerService> {

    public static final ConsoleHandlerAdd INSTANCE = new ConsoleHandlerAdd();

    private ConsoleHandlerAdd() {
        super(TARGET);
    }

    @Override
    protected ConsoleHandlerService createHandlerService(OperationContext context, final ModelNode model) throws OperationFailedException {
        return new ConsoleHandlerService();
    }

    @Override
    protected void updateRuntime(final OperationContext context, final ServiceBuilder<Handler> serviceBuilder, final String name, final ConsoleHandlerService service, final ModelNode model, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        super.updateRuntime(context, serviceBuilder, name, service, model, newControllers);
        final ModelNode target = TARGET.resolveModelAttribute(context, model);
        if (target.isDefined()) {
            service.setTarget(Target.fromString(target.asString()));
        }
    }
}
