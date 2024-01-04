/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;

import org.jboss.ejb3.annotation.ResourceAdapter;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@MessageDriven(messageListenerInterface = SimpleMessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "someProp", propertyValue = "hello world")})
@ResourceAdapter(value = "#rar-within-a-ear.rar")
public class NonJMSMDBRelativePath implements SimpleMessageListener {

    @Override
    public void onMessage(String msg) {

    }
}
