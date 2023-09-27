/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;

import org.jboss.ejb3.annotation.ResourceAdapter;

/**
 * @author Jaikiran Pai
 */
@MessageDriven(messageListenerInterface = SimpleMessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "someProp", propertyValue = "hello world")})
@ResourceAdapter(value = "ear-containing-rar.ear#rar-within-a-ear.rar")
public class NonJMSMDB implements SimpleMessageListener {

    @Override
    public void onMessage(String msg) {

    }
}
