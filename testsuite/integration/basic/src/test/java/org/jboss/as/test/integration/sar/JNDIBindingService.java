/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar;

import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.InitialContext;

import org.jboss.logging.Logger;

/**
 * An MBean that binds to JNDI in its start method and unbinds from JNDI in its stop method
 *
 * @author Jaikiran Pai
 *
 */
public class JNDIBindingService implements JNDIBindingServiceMBean {

    private static final Logger logger = Logger.getLogger(JNDIBindingService.class);

    private static final String NAME = "java:global/env/foo/legacy";
    private static final String VALUE = "BAR";
    private static final AtomicInteger count = new AtomicInteger(1);

    private String jndiName;

    public void create() throws Exception {
        logger.trace("create()");
        this.jndiName =  NAME + count.getAndIncrement();
    }

    public void start() throws Exception {
        logger.trace("start()");
        new InitialContext().bind(jndiName, VALUE);
        logger.trace("Bound to JNDI " + jndiName);
    }

    public void stop() throws Exception {
        logger.trace("stop()");
        new InitialContext().unbind(jndiName);
        logger.trace("Unbound from jndi " + jndiName);
    }

    public void destroy() throws Exception {
        logger.trace("destroy()");
    }

    @Override
    public void sayHello() {
        logger.trace("Hello from " + this);
    }
}
