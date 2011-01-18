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

package org.jboss.as.security.context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.jboss.as.naming.ServiceAwareObjectFactory;
import org.jboss.as.naming.context.ModularReference;
import org.jboss.as.security.plugins.JNDIBasedSecurityManagement;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.security.SecurityConstants;

/**
 * Implementation of {@code ObjectFactory} that returns {@code AuthenticationManager}s
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityDomainObjectFactory implements ServiceAwareObjectFactory, InvocationHandler {

    private JNDIBasedSecurityManagement securityManagement;

    private ConcurrentHashMap<String, SecurityDomainContext> securityManagerMap;

    private volatile ServiceRegistry serviceRegistry;

    /**
     * Create a complete reference to a {@code SecurityDomainObjectFactory) for a given context identifier
     *
     * @param contextIdentifier the context identifier
     * @return a {@code Reference}
     */
    public static Reference createReference(final String contextIdentifier) {
        return ModularReference.create(Context.class, new StringRefAddr("nns", contextIdentifier),
                SecurityDomainObjectFactory.class);
    }

    /**
     * Object factory implementation. This method returns a Context proxy that is only able to handle a lookup operation for an
     * atomic name of a security domain.
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        ClassLoader loader = SecurityActions.getModuleClassLoader();
        Class<?>[] interfaces = { Context.class };
        Context ctx = (Context) Proxy.newProxyInstance(loader, interfaces, this);
        return ctx;
    }

    /**
     * This is the InvocationHandler callback for the Context interface that was created by our getObjectInstance() method. We
     * handle the java:/jaas/domain level operations here.
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Context ctx = new InitialContext();
        NameParser parser = ctx.getNameParser("");
        String securityDomain = null;
        Name name = null;

        if (securityManagement == null) {
            final ServiceController<?> controller;
            final ServiceName serviceName = SecurityManagementService.SERVICE_NAME;
            try {
                controller = serviceRegistry.getRequiredService(serviceName);
            } catch (ServiceNotFoundException e) {
                throw new NamingException("Could not resolve service " + serviceName);
            }
            if (controller.getState() == State.UP) {
                securityManagement = (JNDIBasedSecurityManagement) controller.getValue();
                securityManagerMap = securityManagement.getSecurityManagerMap();
            }
            else {
                throw new NamingException("Could not resolve service " + serviceName);
            }
        }

        String methodName = method.getName();
        if (methodName.equals("toString") == true)
            return SecurityConstants.JAAS_CONTEXT_ROOT + " Context proxy";

        if (methodName.equals("list") == true)
            return new DomainEnumeration(securityManagerMap.keys(), securityManagerMap);

        if (methodName.equals("bind") || methodName.equals("rebind")) {
            if (args[0] instanceof String)
                name = parser.parse((String) args[0]);
            else
                name = (Name) args[0];
            securityDomain = name.get(0);
            SecurityDomainContext val = (SecurityDomainContext) args[1];
            securityManagerMap.put(securityDomain, val);
            return proxy;
        }
        if (methodName.equals("lookup") == false)
            throw new OperationNotSupportedException("Operation not supported: " + method);
        if (args[0] instanceof String)
            name = parser.parse((String) args[0]);
        else
            name = (Name) args[0];
        securityDomain = name.get(0);
        SecurityDomainContext securityDomainCtx = lookupSecurityDomain(securityDomain);
        Object binding = securityDomainCtx.getAuthenticationManager();
        // Look for requests against the security domain context
        if (name.size() == 2) {
            String request = name.get(1);
            binding = securityDomainCtx.lookup(request);
        }
        return binding;
    }

    /**
     * Creates a {@code SecurityDomainContext} if one cannot be found in JNDI for a given security domain
     *
     * @param securityDomain the name of the security domain
     * @return an instance of {@code SecurityDomainContext}
     * @throws Exception if an error occurs
     */
    private SecurityDomainContext lookupSecurityDomain(String securityDomain) throws Exception {
        SecurityDomainContext sdc = (SecurityDomainContext) securityManagerMap.get(securityDomain);
        if (sdc == null) {
            sdc = securityManagement.createSecurityDomainContext(securityDomain);
            securityManagerMap.put(securityDomain, sdc);
        }
        return sdc;
    }

    /**
     * Enumeration for the bound security domains in JNDI
     *
     * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
     */
    class DomainEnumeration implements NamingEnumeration<NameClassPair> {
        Enumeration<String> domains;
        Map<String, SecurityDomainContext> ctxMap;

        DomainEnumeration(Enumeration<String> domains, Map<String, SecurityDomainContext> ctxMap) {
            this.domains = domains;
            this.ctxMap = ctxMap;
        }

        public void close() {
        }

        public boolean hasMoreElements() {
            return domains.hasMoreElements();
        }

        public boolean hasMore() {
            return domains.hasMoreElements();
        }

        public NameClassPair next() {
            String name = (String) domains.nextElement();
            Object value = ctxMap.get(name);
            String className = value.getClass().getName();
            NameClassPair pair = new NameClassPair(name, className);
            return pair;
        }

        public NameClassPair nextElement() {
            return next();
        }
    }

    public void injectServiceRegistry(ServiceRegistry registry) {
        this.serviceRegistry = registry;
    }
}
