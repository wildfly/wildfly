/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.weld.ejb;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ejb.TransactionAttributeType.NEVER;

import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;
import javax.ejb.AccessTimeout;
import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;

/**
 * A simple stateful session bean.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateful
@LocalBean
// do not wait, but throw immediately
@AccessTimeout(0)
// we don't want any interference from tx association (probably a bug)
@TransactionAttribute(NEVER)
public class SimpleStatefulSessionBean {
    public String echo(CountDownLatch latch, String msg) throws InterruptedException {
        boolean tripped = latch.await(5, SECONDS);
        if (!tripped)
            return "Timed out";
        return "Echo " + msg;
    }

    @PostConstruct
    public void postConstruct() {
        SimpleServlet.propagated.set("Shared context");
    }
}
