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
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.logging.Logger;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.SecurityConstants;
import org.jboss.security.audit.AuditManager;
import org.jboss.security.identitytrust.IdentityTrustManager;
import org.jboss.security.mapping.MappingManager;

/**
 * JNDI based implementation of {@code ISecurityManagement}
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class JNDIBasedSecurityManagement implements ISecurityManagement {

    private static final long serialVersionUID = 1924631329555621041L;

    protected static Logger log = Logger.getLogger("org.jboss.as.security");

    private static JNDIBasedSecurityManagement INSTANCE = new JNDIBasedSecurityManagement();

    private transient ConcurrentHashMap<String, SecurityDomainContext> securityMgrMap = new ConcurrentHashMap<String, SecurityDomainContext>();
    private transient ConcurrentHashMap<String, AuthenticationManager> authMgrMap = new ConcurrentHashMap<String, AuthenticationManager>();
    private transient ConcurrentHashMap<String, AuthorizationManager> authzMgrMap = new ConcurrentHashMap<String, AuthorizationManager>();

    private String authenticationManagerClassName;
    private boolean deepCopySubjectMode;
    private String callbackHandlerClassName;
    private String authorizationManagerClassName;

    // creating a singleton
    private JNDIBasedSecurityManagement() {
    }

    public static JNDIBasedSecurityManagement getInstance() {
        return INSTANCE;
    }

    public ConcurrentHashMap<String, SecurityDomainContext> getSecurityManagerMap() {
        return securityMgrMap;
    }

    /** {@inheritDoc} */
    public AuditManager getAuditManager(String securityDomain) {
        // TODO Auto-generated method stub
        return null;
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
            log.trace("Exception getting AuthenticationManager for domain=" + securityDomain, e);
        }
        return am;
    }

    /** {@inheritDoc} */
    public AuthorizationManager getAuthorizationManager(String securityDomain) {
        AuthorizationManager am = null;
        try {
            am = this.authzMgrMap.get(securityDomain);
            if (am == null) {
                am = (AuthorizationManager) lookUpJNDI(securityDomain + "/authorizationMgr");
                this.authzMgrMap.put(securityDomain, am);
            }
        } catch (Exception e) {
            log.trace("Exception getting AuthorizationManager for domain=", e);
        }
        return am;
    }

    /** {@inheritDoc} */
    public IdentityTrustManager getIdentityTrustManager(String securityDomain) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    public MappingManager getMappingManager(String securityDomain) {
        // TODO Auto-generated method stub
        return null;
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
                result = ctx.lookup(SecurityConstants.JAAS_CONTEXT_ROOT + "/" + contextName);
        } catch (Exception e) {
            log.trace("Look up of JNDI for " + contextName + " failed with " + e.getLocalizedMessage());
            return null;
        }
        return result;
    }

    /**
     * Creates a {@code SecurityDomainContext}
     *
     * @param securityDomain name of the security domain
     * @return an instance of {@code SecurityDomainContext}
     * @throws Exception if an error occurs during creation
     */
    public SecurityDomainContext createSecurityDomainContext(String securityDomain) throws Exception {
        log.debug("Creating SDC for domain=" + securityDomain);
        AuthenticationManager am = createAuthenticationManager(securityDomain);
        // TODO create auth cache and set it in am

        // set DeepCopySubject option if supported
        if (deepCopySubjectMode) {
            setDeepCopySubjectMode(am);
        }

        // TODO set auth cache
        SecurityDomainContext securityDomainContext = new SecurityDomainContext(am, null);

        securityDomainContext.setAuthorizationManager(createAuthorizationManager(securityDomain));
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
        Class<?> callbackHandlerClazz = SecurityActions.getContextClassLoader().loadClass(callbackHandlerClassName);
        CallbackHandler ch = (CallbackHandler) callbackHandlerClazz.newInstance();

        Class<?> clazz = SecurityActions.getContextClassLoader().loadClass(authenticationManagerClassName);
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
        Class<?> clazz = SecurityActions.getContextClassLoader().loadClass(authorizationManagerClassName);
        Constructor<?> ctr = clazz.getConstructor(new Class[] { String.class });
        return (AuthorizationManager) ctr.newInstance(new Object[] { securityDomain });
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
            if (log.isTraceEnabled())
                log.trace("Optional setDeepCopySubjectMode failed: " + e.getLocalizedMessage());
        }
    }

}
