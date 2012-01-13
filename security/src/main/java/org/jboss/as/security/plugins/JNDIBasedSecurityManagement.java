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

package org.jboss.as.security.plugins;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.security.auth.callback.CallbackHandler;

import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.security.SecurityMessages;
import org.jboss.logging.Logger;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.CacheableManager;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.JSSESecurityDomain;
import org.jboss.security.SecurityConstants;
import org.jboss.security.audit.AuditManager;
import org.jboss.security.identitytrust.IdentityTrustManager;
import org.jboss.security.mapping.MappingManager;

/**
 * JNDI based implementation of {@code ISecurityManagement}
 *
 * @author Anil.Saldhana@redhat.com
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class JNDIBasedSecurityManagement implements ISecurityManagement {

    private static final long serialVersionUID = 1924631329555621041L;

    protected static Logger log = Logger.getLogger("org.jboss.as.security");

    private transient ConcurrentHashMap<String, SecurityDomainContext> securityMgrMap = new ConcurrentHashMap<String, SecurityDomainContext>();
    private transient ConcurrentHashMap<String, AuthenticationManager> authMgrMap = new ConcurrentHashMap<String, AuthenticationManager>();
    private transient ConcurrentHashMap<String, AuthorizationManager> authzMgrMap = new ConcurrentHashMap<String, AuthorizationManager>();
    private transient ConcurrentHashMap<String, AuditManager> auditMgrMap = new ConcurrentHashMap<String, AuditManager>();
    private transient ConcurrentHashMap<String, IdentityTrustManager> idmMgrMap = new ConcurrentHashMap<String, IdentityTrustManager>();
    private transient ConcurrentHashMap<String, MappingManager> mappingMgrMap = new ConcurrentHashMap<String, MappingManager>();
    private transient ConcurrentHashMap<String, JSSESecurityDomain> jsseMap = new ConcurrentHashMap<String, JSSESecurityDomain>();

    private String authenticationManagerClassName;
    private boolean deepCopySubjectMode;
    private String callbackHandlerClassName;
    private String authorizationManagerClassName;
    private String auditManagerClassName;
    private String identityTrustManagerClassName;
    private String mappingManagerClassName;

    // creating a singleton
    public JNDIBasedSecurityManagement() {
    }

    public ConcurrentHashMap<String, SecurityDomainContext> getSecurityManagerMap() {
        return securityMgrMap;
    }

    /** {@inheritDoc} */
    public AuditManager getAuditManager(String securityDomain) {
        AuditManager am = null;
        try {
            am = auditMgrMap.get(securityDomain);
            if (am == null) {
                am = (AuditManager) lookUpJNDI(securityDomain + "/auditMgr");
                auditMgrMap.put(securityDomain, am);
            }
        } catch (Exception e) {
            log.tracef("Exception getting AuditManager for domain=" + securityDomain, e);
        }
        return am;
    }

    /** {@inheritDoc} */
    public AuthenticationManager getAuthenticationManager(String securityDomain) {
        AuthenticationManager am = null;
        try {
            am = authMgrMap.get(securityDomain);
            if (am == null) {
                am = (AuthenticationManager) lookUpJNDI(securityDomain + "/authenticationMgr");
                authMgrMap.put(securityDomain, am);
            }
        } catch (Exception e) {
            log.tracef("Exception getting AuthenticationManager for domain=" + securityDomain, e);
        }
        return am;
    }

    /** {@inheritDoc} */
    public AuthorizationManager getAuthorizationManager(String securityDomain) {
        AuthorizationManager am = null;
        try {
            am = authzMgrMap.get(securityDomain);
            if (am == null) {
                am = (AuthorizationManager) lookUpJNDI(securityDomain + "/authorizationMgr");
                authzMgrMap.put(securityDomain, am);
            }
        } catch (Exception e) {
            log.tracef("Exception getting AuthorizationManager for domain=", e);
        }
        return am;
    }

    /** {@inheritDoc} */
    public IdentityTrustManager getIdentityTrustManager(String securityDomain) {
        IdentityTrustManager itm = null;
        try {
            itm = idmMgrMap.get(securityDomain);
            if (itm == null) {
                itm = (IdentityTrustManager) lookUpJNDI(securityDomain + "/identityTrustMgr");
                idmMgrMap.put(securityDomain, itm);
            }
        } catch (Exception e) {
            log.tracef("Exception getting IdentityTrustManager for domain=" + securityDomain, e);
        }
        return itm;
    }

    /** {@inheritDoc} */
    public MappingManager getMappingManager(String securityDomain) {
        MappingManager mm = null;
        try {
            mm = mappingMgrMap.get(securityDomain);
            if (mm == null) {
                mm = (MappingManager) lookUpJNDI(securityDomain + "/mappingMgr");
                mappingMgrMap.put(securityDomain, mm);
            }
        } catch (Exception e) {
            log.tracef("Exception getting MappingManager for domain=" + securityDomain, e);
        }
        return mm;
    }

    /** {@inheritDoc} */
    public JSSESecurityDomain getJSSE(String securityDomain) {
        JSSESecurityDomain jsse = null;
        try {
            jsse = jsseMap.get(securityDomain);
            if (jsse == null) {
                jsse = (JSSESecurityDomain) lookUpJNDI(securityDomain + "/jsse");
                jsseMap.put(securityDomain, jsse);
            }
        } catch (Exception e) {
            log.tracef("Exception getting JSSESecurityDomain for domain=" + securityDomain, e);
        }
        return jsse;
    }

    public String getAuthenticationManagerClassName() {
        return authenticationManagerClassName;
    }

    public void setAuthenticationManagerClassName(String authenticationManagerClassName) {
        this.authenticationManagerClassName = authenticationManagerClassName;
    }

    public boolean isDeepCopySubjectMode() {
        return deepCopySubjectMode;
    }

    public void setDeepCopySubjectMode(boolean deepCopySubjectMode) {
        this.deepCopySubjectMode = deepCopySubjectMode;
    }

    public String getCallbackHandlerClassName() {
        return callbackHandlerClassName;
    }

    public void setCallbackHandlerClassName(String callbackHandlerClassName) {
        this.callbackHandlerClassName = callbackHandlerClassName;
    }

    public String getAuthorizationManagerClassName() {
        return authorizationManagerClassName;
    }

    public void setAuthorizationManagerClassName(String authorizationManagerClassName) {
        this.authorizationManagerClassName = authorizationManagerClassName;
    }

    public String getAuditManagerClassName() {
        return auditManagerClassName;
    }

    public void setAuditManagerClassName(String auditManagerClassName) {
        this.auditManagerClassName = auditManagerClassName;
    }

    public String getIdentityTrustManagerClassName() {
        return identityTrustManagerClassName;
    }

    public void setIdentityTrustManagerClassName(String identityTrustManagerClassName) {
        this.identityTrustManagerClassName = identityTrustManagerClassName;
    }

    public String getMappingManagerClassName() {
        return mappingManagerClassName;
    }

    public void setMappingManagerClassName(String mappingManagerClassName) {
        this.mappingManagerClassName = mappingManagerClassName;
    }

    /**
     * Removes one security domain from the maps
     *
     * @param securityDomain name of the security domain
     */
    public void removeSecurityDomain(String securityDomain) {
        securityMgrMap.remove(securityDomain);
        auditMgrMap.remove(securityDomain);
        authMgrMap.remove(securityDomain);
        authzMgrMap.remove(securityDomain);
        idmMgrMap.remove(securityDomain);
        mappingMgrMap.remove(securityDomain);
        jsseMap.remove(securityDomain);
    }

    /**
     * Lookup a context in JNDI
     *
     * @param contextName the context
     * @return the Object found at the context or null if there is nothing bound
     */
    private Object lookUpJNDI(String contextName) {
        Object result = null;
        try {
            Context ctx = new InitialContext();
            if (contextName.startsWith(SecurityConstants.JAAS_CONTEXT_ROOT))
                result = ctx.lookup(contextName);
            else
                result = ctx.lookup(SecurityConstants.JAAS_CONTEXT_ROOT + contextName);
        } catch (Exception e) {
            log.tracef("Look up of JNDI for " + contextName + " failed with " + e.getLocalizedMessage());
            return null;
        }
        return result;
    }

    /**
     * Creates a {@code SecurityDomainContext}
     *
     * @param securityDomain name of the security domain
     * @param cacheFactory creates a cache implementation
     * @return an instance of {@code SecurityDomainContext}
     * @throws Exception if an error occurs during creation
     */
    public SecurityDomainContext createSecurityDomainContext(String securityDomain, Object cacheFactory) throws Exception {
        log.debugf("Creating SDC for domain=" + securityDomain);
        AuthenticationManager am = createAuthenticationManager(securityDomain);
        // create authentication cache
        if (cacheFactory instanceof EmbeddedCacheManager) {
            EmbeddedCacheManager cacheManager = EmbeddedCacheManager.class.cast(cacheFactory);
            @SuppressWarnings("rawtypes")
            Cache cache = null;
            if (cacheManager != null) {
                // TODO override global settings with security domain specific
                cacheManager.defineConfiguration(securityDomain, "auth-cache", new Configuration());
                cache = cacheManager.getCache(securityDomain);
            }
            if (cache != null && am instanceof CacheableManager) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                CacheableManager<Map, Principal> cm = (CacheableManager<Map, Principal>) am;
                cm.setCache(cache);
            }
        } else if (cacheFactory instanceof DefaultAuthenticationCacheFactory) {
            DefaultAuthenticationCacheFactory cacheManager = DefaultAuthenticationCacheFactory.class.cast(cacheFactory);
            @SuppressWarnings("rawtypes")
            Map cache = cacheManager.getCache();
            if (cache != null && am instanceof CacheableManager) {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                CacheableManager<Map, Principal> cm = (CacheableManager<Map, Principal>) am;
                cm.setCache(cache);
            }
        }

        // set DeepCopySubject option if supported
        if (deepCopySubjectMode) {
            setDeepCopySubjectMode(am);
        }

        SecurityDomainContext securityDomainContext = new SecurityDomainContext(am);
        securityDomainContext.setAuthorizationManager(createAuthorizationManager(securityDomain));
        securityDomainContext.setAuditManager(createAuditManager(securityDomain));
        securityDomainContext.setIdentityTrustManager(createIdentityTrustManager(securityDomain));
        securityDomainContext.setMappingManager(createMappingManager(securityDomain));
        return securityDomainContext;
    }

    /**
     * Creates an {@code AuthenticationManager}
     *
     * @param securityDomain name of the security domain
     * @return an instance of {@code AuthenticationManager}
     * @throws Exception if creation fails
     */
    private AuthenticationManager createAuthenticationManager(String securityDomain) throws Exception {
        int i = callbackHandlerClassName.lastIndexOf(":");
        if (i == -1)
            throw SecurityMessages.MESSAGES.missingModuleName("default-callback-handler-class-name attribute");
        String moduleSpec = callbackHandlerClassName.substring(0, i);
        String className = callbackHandlerClassName.substring(i + 1);
        Class<?> callbackHandlerClazz = SecurityActions.getModuleClassLoader(moduleSpec).loadClass(className);
        CallbackHandler ch = (CallbackHandler) callbackHandlerClazz.newInstance();

        i = authenticationManagerClassName.lastIndexOf(":");
        if (i == -1)
            throw SecurityMessages.MESSAGES.missingModuleName("authentication-manager-class-name attribute");
        moduleSpec = authenticationManagerClassName.substring(0, i);
        className = authenticationManagerClassName.substring(i + 1);
        Class<?> clazz = SecurityActions.getModuleClassLoader(moduleSpec).loadClass(className);
        Constructor<?> ctr = clazz.getConstructor(new Class[] { String.class, CallbackHandler.class });
        return (AuthenticationManager) ctr.newInstance(new Object[] { securityDomain, ch });
    }

    /**
     * Creates an {@code AuthorizationManager}
     *
     * @param securityDomain name of the security domain
     * @return an instance of {@code AuthorizationManager}
     * @throws Exception if creation fails
     */
    private AuthorizationManager createAuthorizationManager(String securityDomain) throws Exception {
        int i = authorizationManagerClassName.lastIndexOf(":");
        if (i == -1)
            throw SecurityMessages.MESSAGES.missingModuleName("authorization manager class");
        String moduleSpec = authorizationManagerClassName.substring(0, i);
        String className = authorizationManagerClassName.substring(i + 1);
        Class<?> clazz = SecurityActions.getModuleClassLoader(moduleSpec).loadClass(className);
        Constructor<?> ctr = clazz.getConstructor(new Class[] { String.class });
        return (AuthorizationManager) ctr.newInstance(new Object[] { securityDomain });
    }

    /**
     * Creates an {@code AuditManager}
     *
     * @param securityDomain name of the security domain
     * @return an instance of {@code AuditManager}
     * @throws Exception if creation fails
     */
    private AuditManager createAuditManager(String securityDomain) throws Exception {
        int i = auditManagerClassName.lastIndexOf(":");
        if (i == -1)
            throw SecurityMessages.MESSAGES.missingModuleName("audit manager class");
        String moduleSpec = auditManagerClassName.substring(0, i);
        String className = auditManagerClassName.substring(i + 1);
        Class<?> clazz = SecurityActions.getModuleClassLoader(moduleSpec).loadClass(className);
        Constructor<?> ctr = clazz.getConstructor(new Class[] { String.class });
        return (AuditManager) ctr.newInstance(new Object[] { securityDomain });
    }

    /**
     * Creates an {@code IdentityTrustManager}
     *
     * @param securityDomain name of the security domain
     * @return an instance of {@code IdentityTrustManager}
     * @throws Exception if creation fails
     */
    private IdentityTrustManager createIdentityTrustManager(String securityDomain) throws Exception {
        int i = identityTrustManagerClassName.lastIndexOf(":");
        if (i == -1)
            throw SecurityMessages.MESSAGES.missingModuleName("identity trust manager class");
        String moduleSpec = identityTrustManagerClassName.substring(0, i);
        String className = identityTrustManagerClassName.substring(i + 1);
        Class<?> clazz = SecurityActions.getModuleClassLoader(moduleSpec).loadClass(className);
        Constructor<?> ctr = clazz.getConstructor(new Class[] { String.class });
        return (IdentityTrustManager) ctr.newInstance(new Object[] { securityDomain });
    }

    /**
     * Creates an {@code MappingManager}
     *
     * @param securityDomain name of the security domain
     * @return an instance of {@code MappingManager}
     * @throws Exception if creation fails
     */
    private MappingManager createMappingManager(String securityDomain) throws Exception {
        int i = mappingManagerClassName.lastIndexOf(":");
        if (i == -1)
            throw SecurityMessages.MESSAGES.missingModuleName("mapping manager class");
        String moduleSpec = mappingManagerClassName.substring(0, i);
        String className = mappingManagerClassName.substring(i + 1);
        Class<?> clazz = SecurityActions.getModuleClassLoader(moduleSpec).loadClass(className);
        Constructor<?> ctr = clazz.getConstructor(new Class[] { String.class });
        return (MappingManager) ctr.newInstance(new Object[] { securityDomain });
    }

    /**
     * Use reflection to attempt to set the deep copy subject mode on the {@code AuthenticationManager}
     *
     * @param authenticationManager the {@code AuthenticationManager}
     */
    private static void setDeepCopySubjectMode(AuthenticationManager authenticationManager) {
        try {
            Class<?>[] argsType = { Boolean.class };
            Method m = authenticationManager.getClass().getMethod("setDeepCopySubjectOption", argsType);
            Object[] deepCopyArgs = { Boolean.TRUE };
            m.invoke(authenticationManager, deepCopyArgs);
        } catch (Exception e) {
            log.tracef("Optional setDeepCopySubjectMode failed: " + e.getLocalizedMessage());
        }
    }

}
