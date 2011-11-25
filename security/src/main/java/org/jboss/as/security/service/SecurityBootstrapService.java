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

import java.lang.reflect.Constructor;
import java.security.Policy;
import java.util.Properties;
import java.util.Set;

import javax.security.jacc.PolicyContext;

import org.jboss.as.security.SecurityExtension;
import org.jboss.as.security.plugins.ModuleClassLoaderLocator;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.security.SecurityConstants;
import org.jboss.security.auth.callback.CallbackHandlerPolicyContextHandler;
import org.jboss.security.jacc.SubjectPolicyContextHandler;
import org.jboss.security.plugins.ClassLoaderLocatorFactory;

/**
 * Bootstrap service for the security container
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 * @author Anil Saldhana
 */
public class SecurityBootstrapService implements Service<Void> {

    public static final ServiceName SERVICE_NAME = SecurityExtension.JBOSS_SECURITY.append("bootstrap");

    private static final Logger log = Logger.getLogger("org.jboss.as.security");

    protected volatile Properties securityProperty;

    private Policy oldPolicy;

    private Policy jaccPolicy;

    private static final String JACC_POLICY_PROVIDER = "javax.security.jacc.policy.provider";

    public SecurityBootstrapService() {
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting SecurityBootstrapService");
        try {
            //Print out the current version of PicketBox
            log.info("Picketbox version="+org.picketbox.Version.VERSION);

            // Get the current Policy impl
            oldPolicy = Policy.getPolicy();
            String provider = SecurityActions.getSystemProperty(JACC_POLICY_PROVIDER,
                    "org.jboss.security.jacc.DelegatingPolicy");
            Class<?> providerClass = SecurityActions.loadClass(provider);
            try {
                // Look for a ctor(Policy) signature
                Class<?>[] ctorSig = { Policy.class };
                Constructor<?> ctor = providerClass.getConstructor(ctorSig);
                Object[] ctorArgs = { oldPolicy };
                jaccPolicy = (Policy) ctor.newInstance(ctorArgs);
            } catch (NoSuchMethodException e) {
                log.debugf("Provider does not support ctor(Policy)");
                try {
                    jaccPolicy = (Policy) providerClass.newInstance();
                } catch (Exception e1) {
                    throw new StartException(e1);
                }
            } catch (Exception e) {
                throw new StartException(e);
            }

            // Install the JACC policy provider
            Policy.setPolicy(jaccPolicy);

            // Have the policy load/update itself
            jaccPolicy.refresh();

            // Register the default active Subject PolicyContextHandler
            SubjectPolicyContextHandler handler = new SubjectPolicyContextHandler();
            PolicyContext.registerHandler(SecurityConstants.SUBJECT_CONTEXT_KEY, handler, true);

            // Register the JAAS CallbackHandler JACC PolicyContextHandlers
            CallbackHandlerPolicyContextHandler chandler = new CallbackHandlerPolicyContextHandler();
            PolicyContext.registerHandler(SecurityConstants.CALLBACK_HANDLER_KEY, chandler, true);

            //Register a module classloader locator
            ClassLoaderLocatorFactory.set(new ModuleClassLoaderLocator());
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public void stop(StopContext context) {
        // remove handlers
        Set handlerKeys = PolicyContext.getHandlerKeys();
        handlerKeys.remove(SecurityConstants.CALLBACK_HANDLER_KEY);
        handlerKeys.remove(SecurityConstants.SUBJECT_CONTEXT_KEY);

        // Install the policy provider that existed on startup
        if (jaccPolicy != null)
            Policy.setPolicy(oldPolicy);
    }

    /** {@inheritDoc} */
    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

}