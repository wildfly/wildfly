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

package org.wildfly.clustering.singleton.server;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.SingletonElectionListener;

/**
 * Default singleton election listener that logs the results of the singleton election.
 * @author Paul Ferraro
 */
public class DefaultSingletonElectionListener implements SingletonElectionListener {

    private final ServiceName name;
    private final Supplier<Group> group;
    private final AtomicReference<Node> primaryMember = new AtomicReference<>();

    public DefaultSingletonElectionListener(ServiceName name, Supplier<Group> group) {
        this.name = name;
        this.group = group;
    }

    @Override
    public void elected(List<Node> candidateMembers, Node electedMember) {
        Node localMember = this.group.get().getLocalMember();
        Node previousElectedMember = this.primaryMember.getAndSet(electedMember);

        if (electedMember != null) {
            SingletonLogger.ROOT_LOGGER.elected(electedMember.getName(), this.name.getCanonicalName());
        } else {
            SingletonLogger.ROOT_LOGGER.noPrimaryElected(this.name.getCanonicalName());
        }
        if (localMember.equals(electedMember)) {
            SingletonLogger.ROOT_LOGGER.startSingleton(this.name.getCanonicalName());
        } else if (localMember.equals(previousElectedMember)) {
            SingletonLogger.ROOT_LOGGER.stopSingleton(this.name.getCanonicalName());
        }
    }
}
