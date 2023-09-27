/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.invocation;

import org.jboss.wsf.spi.invocation.InvocationHandler;
import org.jboss.wsf.spi.invocation.InvocationHandlerFactory;
import org.jboss.wsf.spi.invocation.InvocationType;

/**
 * The default invocation model factory for JBoss AS.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class InvocationHandlerFactoryImpl extends InvocationHandlerFactory {

    public InvocationHandler newInvocationHandler(final InvocationType type) {
        InvocationHandler handler = null;

        switch (type) {
            case JAXWS_JSE:
                handler = new InvocationHandlerJAXWS();
                break;
            case JAXWS_EJB3:
                handler = new InvocationHandlerJAXWS();
                break;
            default:
                throw new IllegalArgumentException();
        }

        return handler;
    }

}
