/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.jboss.as.domain.controller.operations.coordination;

import org.jboss.as.controller.OperationContext.AttachmentKey;

/**
 * Contains operation headers and attachments used to communicate the domain controller's lock id to the slaves.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public final class DomainControllerLockIdUtils {

    /**
     * The domain controller lock id header sent by the DC to the slave host. This is used by the slaves
     * to latch onto any ongoing operation in the DC.
     */
    public static final String DOMAIN_CONTROLLER_LOCK_ID = "domain-controller-lock-id";

    /**
     * The attachment used by the slave to keep track of the lock id on the DC (if any)
     */
    public static final AttachmentKey<Integer> DOMAIN_CONTROLLER_LOCK_ID_ATTACHMENT = AttachmentKey.create(Integer.class);

    /**
     * The slave controller lock id header sent by the slaves to the DC. This is used to group several
     * slave requuests onto one DC request.
     */
    public static final String SLAVE_CONTROLLER_LOCK_ID = "slave-controller-lock-id";

    private DomainControllerLockIdUtils() {
    }
}
