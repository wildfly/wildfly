/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.container.interceptor;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@LocalBean
public class AnotherFlowTrackingBean extends FlowTrackingBean {

    public String echoWithMethodSpecificContainerInterceptor(final String msg) {
        return msg;
    }

    public String echoInSpecificOrderOfContainerInterceptors(final String msg) {
        return msg;
    }

    public String failingEcho(final String msg) {
        return msg;
    }
}
