/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;

@Stateful
public class SimpleIncrementorBean implements Incrementor {
    @Resource
    private SessionContext context;

    private final AtomicInteger count = new AtomicInteger(0);
    private transient boolean activated = false;
    private boolean passivated = false;

    @Override
    public int increment() {
        if (this.count.get() > 0) {
            assert this.passivated;
            assert this.activated;
            this.passivated = false;
        }
        return this.count.incrementAndGet();
    }

    @PrePassivate
    public void prePassivate() {
        this.passivated = true;
    }

    @PostActivate
    public void postActivate() {
        this.activated = true;
    }
}
