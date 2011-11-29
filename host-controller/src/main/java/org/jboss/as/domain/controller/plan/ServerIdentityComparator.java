/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.controller.plan;

import java.util.Comparator;

import org.jboss.as.controller.client.helpers.domain.ServerIdentity;

/** Used to order ServerIdentity instances based on host name */
class ServerIdentityComparator implements Comparator<ServerIdentity> {

    static final ServerIdentityComparator INSTANCE = new ServerIdentityComparator();

    @Override
    public int compare(ServerIdentity o1, ServerIdentity o2) {
        int val = o1.getHostName().compareTo(o2.getHostName());
        if (val == 0) {
            val = o1.getServerName().compareTo(o2.getServerName());
        }
        return val;
    }
}
