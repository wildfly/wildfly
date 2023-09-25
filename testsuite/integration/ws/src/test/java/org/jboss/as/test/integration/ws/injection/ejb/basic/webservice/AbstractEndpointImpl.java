/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ws.injection.ejb.basic.webservice;

import org.jboss.as.test.integration.ws.injection.ejb.basic.shared.BeanIface;
import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.xml.ws.WebServiceException;

/**
 * Basic endpoint implementation.
 *
 * @author <a href="mailto:rsvoboda@redhat.com">Rostislav Svoboda</a>
 */
public abstract class AbstractEndpointImpl implements EndpointIface {

    private static final Logger LOG = Logger.getLogger(AbstractEndpointImpl.class);

    private boolean correctState;
    @EJB
    private BeanIface testBean2;

    private BeanIface testBean1;

    @EJB
    private void setBean(BeanIface bean) {
        this.testBean1 = bean;
    }

    private Boolean boolean1;

    /**
     * EJB 3.0 16.2.2: "By default, the name of the field is combined with the
     * name of the class in which the annotation is used and is used directly
     * as the name in the bean’s naming context
     */
    @Resource(name = "boolean1")
    private void setBoolean1(Boolean b) {
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
