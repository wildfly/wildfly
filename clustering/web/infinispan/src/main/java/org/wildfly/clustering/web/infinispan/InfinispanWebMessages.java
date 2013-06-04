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
package org.wildfly.clustering.web.infinispan;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.metadata.web.jboss.ReplicationGranularity;

/**
 * InfinispanWebMessages
 *
 * logging id range: 10330 - 10339
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface InfinispanWebMessages {
    /**
     * The messages
     */
    InfinispanWebMessages MESSAGES = Messages.getBundle(InfinispanWebMessages.class);

    /**
     * Creates an exception indicating the replication granularity is unknown.
     *
     * @param value the invalid replication granularity.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 10336, value = "Unknown replication granularity: %s")
    IllegalArgumentException unknownReplicationGranularity(ReplicationGranularity value);

    @Message(id = 10337, value = "Session %s is not valid")
    IllegalStateException invalidSession(String sessionId);
}
