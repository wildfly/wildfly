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
package org.jboss.as.test.clustering.cluster.singleton.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

public class MyService implements Service<Environment> {

    public static final ServiceName DEFAULT_SERVICE_NAME = ServiceName.JBOSS.append("test", "myservice", "default");
    public static final ServiceName QUORUM_SERVICE_NAME = ServiceName.JBOSS.append("test", "myservice", "quorum");

    private final Value<ServerEnvironment> env;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public MyService(Value<ServerEnvironment> env) {
        this.env = env;
    }

    @Override
    public Environment getValue() {
        if (!this.started.get()) {
            throw new IllegalStateException();
        }
        return new Environment(this.env.getValue().getNodeName());
    }

    @Override
    public void start(StartContext context) {
        this.started.set(true);
    }

    @Override
    public void stop(StopContext context) {
        this.started.set(false);
    }
}
