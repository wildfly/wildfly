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

import java.util.Set;

import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;

import org.jboss.as.security.SecuritySubsystemElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.security.SecurityConstants;
import org.jboss.security.auth.callback.CallbackHandlerPolicyContextHandler;
import org.jboss.security.jacc.SubjectPolicyContextHandler;

/**
 * Bootstrap service for the security container
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityBootstrapService implements Service<Void> {

    public static final ServiceName SERVICE_NAME = SecuritySubsystemElement.JBOSS_SECURITY.append("bootstrap");

    private static final Logger log = Logger.getLogger("org.jboss.as.security");

    public SecurityBootstrapService() {
    }

    /** {@inheritDoc} */
    public void start(StartContext context) throws StartException {
        if (log.isDebugEnabled())
            log.debug("Starting SecurityBootstrapService");
        try {
            // Register the default active Subject PolicyContextHandler
            SubjectPolicyContextHandler handler = new SubjectPolicyContextHandler();
            PolicyContext.registerHandler(SecurityConstants.SUBJECT_CONTEXT_KEY, handler, true);

            // Register the JAAS CallbackHandler JACC PolicyContextHandlers
            CallbackHandlerPolicyContextHandler chandler = new CallbackHandlerPolicyContextHandler();
            PolicyContext.registerHandler(CallbackHandlerPolicyContextHandler.CALLBACK_HANDLER_KEY, chandler, true);
        } catch (PolicyContextException pce) {
            throw new StartException(pce);
        }
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
        // remove handlers
        Set handlerKeys = PolicyContext.getHandlerKeys();
        handlerKeys.remove(CallbackHandlerPolicyContextHandler.CALLBACK_HANDLER_KEY);
        handlerKeys.remove(SecurityConstants.SUBJECT_CONTEXT_KEY);
    }

    /** {@inheritDoc} */
    public Void getValue() throws IllegalStateException {
        return null;
    }

}
