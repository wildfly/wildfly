/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.messagedriven;

import jakarta.ejb.MessageDrivenBean;
import jakarta.ejb.MessageDrivenContext;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;

/**
 * An interceptor for MDBs, which will invoke the {@link MessageDrivenBean#setMessageDrivenContext(jakarta.ejb.MessageDrivenContext)}
 * method.
 *
 * @author Jaikiran Pai
 */
public class MessageDrivenBeanSetMessageDrivenContextInterceptor implements Interceptor {

    static final MessageDrivenBeanSetMessageDrivenContextInterceptor INSTANCE = new MessageDrivenBeanSetMessageDrivenContextInterceptor();

    private MessageDrivenBeanSetMessageDrivenContextInterceptor() {

    }

    @Override
    public Object processInvocation(InterceptorContext context) throws Exception {
        final MessageDrivenComponentInstance componentInstance = (MessageDrivenComponentInstance) context.getPrivateData(ComponentInstance.class);
        final MessageDrivenContext messageDrivenContext = (MessageDrivenContext) componentInstance.getEjbContext();
        ((MessageDrivenBean) context.getTarget()).setMessageDrivenContext(messageDrivenContext);
        return context.proceed();
    }
}
