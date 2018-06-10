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
package org.wildfly.clustering.singleton;

import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.singleton.service.SingletonServiceConfigurator;

/**
 * Builds a singleton service.
 * @author Paul Ferraro
 * @param <T> the singleton service value type
 * @deprecated Replaced by {@link SingletonServiceConfigurator}.
 */
@Deprecated
public interface SingletonServiceBuilder<T> extends Builder<T> {

    /**
     * Defines the minimum number of members required before a singleton election will take place.
     * @param quorum the quorum required for electing a primary singleton provider
     * @return a reference to this builder
     */
    SingletonServiceBuilder<T> requireQuorum(int quorum);

    /**
     * Defines the policy for electing a primary singleton provider.
     * @param policy an election policy
     * @return a reference to this builder
     */
    SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy policy);
}
