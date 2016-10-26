/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
