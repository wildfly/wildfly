/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.auditing;

import org.jboss.logging.Logger;
import org.jboss.security.audit.AbstractAuditProvider;
import org.jboss.security.audit.AuditEvent;

/**
 * A simple {@link org.jboss.security.audit.AuditProvider} implementation that just logs on the INFO level.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CustomAuditProviderModule extends AbstractAuditProvider {

    private static final Logger log = Logger.getLogger(CustomAuditProviderModule.class);

    public void audit(AuditEvent auditEvent) {
        Exception e = auditEvent.getUnderlyingException();
        if (e != null) {
            log.info(auditEvent, e);
        } else {
            log.info(auditEvent);
        }
    }
}