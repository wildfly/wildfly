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
package org.jboss.as.test.integration.ws.injection.ejb.basic.webservice;

import org.jboss.as.test.integration.ws.injection.ejb.basic.shared.BeanIface;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.xml.ws.WebServiceException;

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
