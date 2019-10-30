/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonElectionListener;

/**
 * @author Paul Ferraro
 */
public class SingletonElectionListenerService implements Service, SingletonElectionListener, Supplier<Node> {
    private final Consumer<Supplier<Node>> injector;
    private volatile Node primaryMember = null;

    public SingletonElectionListenerService(Consumer<Supplier<Node>> injector) {
        this.injector = injector;
    }

    @Override
    public Node get() {
        return this.primaryMember;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.injector.accept(this);
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public void elected(List<Node> candidateMembers, Node electedMember) {
        this.primaryMember = electedMember;
    }
}
