/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import java.net.UnknownHostException;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.clustering.ClusteringMessages;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.service.StartException;

/**
 * Date: 29.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface InfinispanMessages extends ClusteringMessages {

    /**
     * A logger with the category of the default clustering package.
     */
    InfinispanMessages MESSAGES = Messages.getBundle(InfinispanMessages.class);

    /**
     * Creates an exception indicating a failure to resolve the outbound socket binding represented by the
     * {@code binding} parameter.
     *
     * @param cause the cause of the error.
     * @param binding the outbound socket binding.
     *
     * @return a {@link ConfigurationPersistenceException} for the error.
     */
    @Message(id = 10310, value = "Could not resolve destination address for outbound socket binding named '%s'")
    InjectionException failedToInjectSocketBinding(@Cause UnknownHostException cause, OutboundSocketBinding binding);

    @Message(id = 10311, value = "Failed to add %s %s cache to non-clustered %s cache container.")
    StartException transportRequired(CacheMode mode, String cache, String cacheContainer);
}
