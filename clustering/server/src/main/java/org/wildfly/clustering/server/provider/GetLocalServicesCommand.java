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
package org.wildfly.clustering.server.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.wildfly.clustering.dispatcher.Command;

/**
 * Command to obtain the service providers known to a node.
 * @author Paul Ferraro
 */
public class GetLocalServicesCommand<T> implements Command<Collection<T>, Set<T>> {
    private static final long serialVersionUID = -6038614943434229434L;

    @Override
    public Collection<T> execute(Set<T> services) {
        return new ArrayList<>(services);
    }
}
