/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@MessageDriven(messageListenerInterface = SimpleMessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "someProp", propertyValue = "hello world")})
public class NonJMSMDBRelativePathWithDeploymentDescriptor implements SimpleMessageListener {

    @Override
    public void onMessage(String msg) {

    }
}
