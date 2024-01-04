/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ws.injection.ejb.as1675;

import org.jboss.as.test.integration.ws.injection.ejb.as1675.shared.BeanIface;
import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.xml.ws.WebServiceException;

/**
 * Abstract endpoint implementation.
 *
 * @author <a href="mailto:richard.opalka@jboss.org">Richard Opalka</a>
 */
public abstract class AbstractEndpointImpl implements EndpointIface {
    private static final Logger LOG = Logger.getLogger(AbstractEndpointImpl.class);
    private boolean correctState;
    private BeanIface testBean1;
    @EJB
    private BeanIface testBean2;
    private Boolean boolean1;

    @EJB(name = "as1675/BeanImpl/local-org.jboss.as.test.integration.ws.injection.ejb.as1675.shared.BeanIface")
    private void setBean(BeanIface bean) {
        this.testBean1 = bean;
    }

    @Resource(name = "boolean1")
    private void setBoolean1(final Boolean b) {
        this.boolean1 = b;
    }

    public String echo(final String msg) {
        if (!this.correctState) {
            throw new WebServiceException("Injection failed");
        }

        LOG.trace("echo: " + msg);
        return msg;
    }

    @PostConstruct
    private void init() {
        boolean currentState = true;

        if (this.testBean1 == null) {
            LOG.error("Annotation driven initialization for testBean1 failed");
            currentState = false;
        }
        if (!"Injected hello message".equals(testBean1.printString())) {
            LOG.error("Annotation driven initialization for testBean1 failed");
            currentState = false;
        }
        if (this.testBean2 == null) {
            LOG.error("Annotation driven initialization for testBean2 failed");
            currentState = false;
        }
        if (!"Injected hello message".equals(testBean2.printString())) {
            LOG.error("Annotation driven initialization for testBean2 failed");
            currentState = false;
        }
        if (this.boolean1 == null || this.boolean1 != true) {
            LOG.error("Annotation driven initialization for boolean1 failed");
            currentState = false;
        }

        this.correctState = currentState;
    }

}
