/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.context;

import org.jboss.as.ejb3.component.EjbComponentInstance;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponent;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenContext extends EJBContextImpl implements jakarta.ejb.MessageDrivenContext{


    public MessageDrivenContext(final EjbComponentInstance instance) {
        super(instance);
    }

    @Override
    public MessageDrivenComponent getComponent() {
        return (MessageDrivenComponent) super.getComponent();
    }
}
