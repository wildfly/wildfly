/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.operations;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;

/**
 * WFLY-1904: performs deferred resolution and storage of system properties,
 * after any security vault has had a chance to initialize. This makes
 * it possible for vault expressions to be used in the configured
 * property value.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public interface SystemPropertyDeferredProcessor {

    /**
     * Resolve and store any system properties that could not be resolved during
     * the normal handling of system properties.
     *
     * @param context the operation context
     * @throws OperationFailedException if any properties could still not be resolved
     */
    void processDeferredProperties(OperationContext context) throws OperationFailedException;

    /** Key under which the {@code SystemPropertyDeferredProcessor} should be attached to the operation context. */
    OperationContext.AttachmentKey<SystemPropertyDeferredProcessor> ATTACHMENT_KEY = OperationContext.AttachmentKey.create(SystemPropertyDeferredProcessor.class);
}
