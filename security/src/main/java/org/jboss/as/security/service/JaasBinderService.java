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

package org.jboss.as.security.service;

import javax.naming.Reference;

import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.JavaContextService;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.Value;
import org.jboss.security.SecurityConstants;

/**
 * Service to bind a {@code SecurityDomainObjectFactory} to JNDI under java:/jaas
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 *
 */
public class JaasBinderService extends BinderService<Reference> {

    private static String contextName = "jaas";

    public static ServiceName SERVICE_NAME = JavaContextService.SERVICE_NAME.append(contextName);

    private static final Logger log = Logger.getLogger("org.jboss.as.security");

    public JaasBinderService(Value<Reference> value) {
        super(SecurityConstants.JAAS_CONTEXT_ROOT, value);
    }

    public synchronized void start(StartContext context) throws StartException {
        if (log.isDebugEnabled())
            log.debug("Starting JaasBinderService");
        super.start(context);
    }

}
