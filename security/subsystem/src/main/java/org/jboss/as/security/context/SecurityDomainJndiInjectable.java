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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.jboss.as.naming.ContextListAndJndiViewManagedReferenceFactory;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.as.security.logging.SecurityLogger;
import org.jboss.as.security.plugins.DefaultAuthenticationCacheFactory;
import org.jboss.as.security.plugins.JNDIBasedSecurityManagement;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.SecurityConstants;

/**
 * Implementation of {@code ManagedReferenceFactory} that returns {@code AuthenticationManager}s
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class SecurityDomainJndiInjectable implements InvocationHandler, ContextListAndJndiViewManagedReferenceFactory {

    private static final String ACTIVE_SUBJECT = "subject";
    private static final String AUTHENTICATION_MGR = "authenticationMgr";
    private static final String AUTHORIZATION_MGR = "authorizationMgr";
    private static final String AUDIT_MGR = "auditMgr";
    private static final String MAPPING_MGR = "mappingMgr";
    private static final String IDENTITY_TRUST_MGR = "identityTrustMgr";
    private static final String DOMAIN_CONTEXT = "domainContext";
    private static final String JSSE = "jsse";

    private final InjectedValue<ISecurityManagement> securityManagementValue = new InjectedValue<ISecurityManagement>();

    @Override
    public String getInstanceClassName() {
        return getReference().getInstance().getClass().getName();
    }

    @Override
    public String getJndiViewInstanceValue() {
        return String.valueOf(getReference().getInstance());
    }

    /**
     * {@inheritDoc}
     *
     * This method returns a Context proxy that is only able to handle a lookup operation for an atomic name of a security
     * domain.
     */
    public ManagedReference getReference() {
        final ClassLoader loader;
        try {
            loader = SecurityActions.getModuleClassLoader();
        } catch (ModuleLoadException e) {
            throw SecurityLogger.ROOT_LOGGER.unableToGetModuleClassLoader(e);
        }
        Class<?>[] interfaces = { Context.class };
        return new ValueManagedReference(new ImmediateValue<Object>(Proxy.newProxyInstance(loader, interfaces, this)));
    }

    /**
     * This is the InvocationHandler callback for the Context interface that was created by our getObjectInstance() method. We
     * handle the java:jboss/jaas/domain level operations here.
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Context ctx = new InitialContext();
        NameParser parser = ctx.getNameParser("");
        String securityDomain = null;
        Name name = null;

        final JNDIBasedSecurityManagement securityManagement = JNDIBasedSecurityManagement.class.cast(securityManagementValue
                .getValue());
        final ConcurrentHashMap<String, SecurityDomainContext> securityManagerMap = securityManagement.getSecurityManagerMap();

        String methodName = method.getName();
        if (methodName.equals("toString"))
            return SecurityConstants.JAAS_CONTEXT_ROOT + " Context proxy";

        if (methodName.equals("list"))
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
        if (!methodName.equals("lookup"))
            throw SecurityLogger.ROOT_LOGGER.operationNotSupported(method);
        if (args[0] instanceof String)
            name = parser.parse((String) args[0]);
        else
            name = (Name) args[0];
        securityDomain = name.get(0);
        SecurityDomainContext securityDomainCtx = lookupSecurityDomain(securityManagement, securityManagerMap, securityDomain);
        Object binding = securityDomainCtx.getAuthenticationManager();
        // Look for requests against the security domain context
        if (name.size() == 2) {
            String request = name.get(1);
            binding = lookup(securityDomainCtx, request);
        }
        return binding;
    }

    /**
     * Creates a {@code SecurityDomainContext} if one cannot be found in JNDI for a given security domain
     *
     * @param securityManagement security management
     * @param securityManagerMap security manager map
     * @param securityDomain the name of the security domain
     * @return an instance of {@code SecurityDomainContext}
     * @throws Exception if an error occurs
     */
    private SecurityDomainContext lookupSecurityDomain(final JNDIBasedSecurityManagement securityManagement,
            final ConcurrentHashMap<String, SecurityDomainContext> securityManagerMap, final String securityDomain)
            throws Exception {
        SecurityDomainContext sdc = securityManagerMap.get(securityDomain);
        if (sdc == null) {
            sdc = securityManagement.createSecurityDomainContext(securityDomain, new DefaultAuthenticationCacheFactory());
            securityManagerMap.put(securityDomain, sdc);
        }
        return sdc;
    }

    private static Object lookup(SecurityDomainContext securityDomainContext, String name) throws NamingException {
        Object binding = null;
        if (name == null || name.length() == 0)
            throw SecurityLogger.ROOT_LOGGER.nullName();

        if (name.equals(ACTIVE_SUBJECT))
            binding = securityDomainContext.getSubject();
        else if (name.equals(AUTHENTICATION_MGR))
            binding = securityDomainContext.getAuthenticationManager();
        else if (name.equals(AUTHORIZATION_MGR))
            binding = securityDomainContext.getAuthorizationManager();
        else if (name.equals(AUDIT_MGR))
            binding = securityDomainContext.getAuditManager();
        else if (name.equals(MAPPING_MGR))
            binding = securityDomainContext.getMappingManager();
        else if (name.equals(IDENTITY_TRUST_MGR))
            binding = securityDomainContext.getIdentityTrustManager();
        else if (name.equals(DOMAIN_CONTEXT))
            binding = securityDomainContext;
        else if (name.equals(JSSE))
            binding = securityDomainContext.getJSSE();

        return binding;

    }

    public Injector<ISecurityManagement> getSecurityManagementInjector() {
        return securityManagementValue;
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
}
