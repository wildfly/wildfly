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
package org.jboss.as.webservices.invocation;

import javax.xml.rpc.handler.MessageContext;

import org.jboss.invocation.InterceptorContext;
import org.jboss.wsf.spi.invocation.HandlerCallback;
import org.jboss.wsf.spi.invocation.Invocation;

/**
 * Handles invocations on EJB21 endpoints.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class InvocationHandlerEJB21 extends AbstractInvocationHandler {

    @Override
    protected void prepareForInvocation(final InterceptorContext context, final Invocation wsInvocation) {
        final MessageContext msgContext = wsInvocation.getInvocationContext().getAttachment(MessageContext.class);
        final HandlerCallback callback = wsInvocation.getInvocationContext().getAttachment(HandlerCallback.class);
        context.putPrivateData(MessageContext.class, msgContext);
        context.putPrivateData(HandlerCallback.class, callback);
        context.putPrivateData(Invocation.class, wsInvocation);
    }

}
