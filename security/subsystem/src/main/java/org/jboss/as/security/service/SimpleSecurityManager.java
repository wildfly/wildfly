/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

import static java.security.AccessController.doPrivileged;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.acl.Group;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.as.core.security.UniqueIdUserInfo;
import org.jboss.as.domain.management.security.PasswordCredential;
import org.jboss.as.security.SecurityMessages;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.remoting3.security.UserInfo;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.RunAs;
import org.jboss.security.RunAsIdentity;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;
import org.jboss.security.SecurityContextUtil;
import org.jboss.security.SecurityRolesAssociation;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.SubjectInfo;
import org.jboss.security.audit.AuditEvent;
import org.jboss.security.audit.AuditLevel;
import org.jboss.security.audit.AuditManager;
import org.jboss.security.authorization.resources.EJBResource;
import org.jboss.security.identity.Identity;
import org.jboss.security.identity.plugins.SimpleIdentity;
import org.jboss.security.identity.plugins.SimpleRoleGroup;
import org.jboss.security.javaee.AbstractEJBAuthorizationHelper;
import org.jboss.security.javaee.SecurityHelperFactory;
import org.jboss.security.javaee.SecurityRoleRef;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SimpleSecurityManager implements ServerSecurityManager {
    private ThreadLocalStack<SecurityContext> contexts = new ThreadLocalStack<SecurityContext>();
    /**
     * Indicates if we propagate previous SecurityContext informations to the current one or not.
     * This was introduced for WFLY-981 where we wanted to have a clean SecurityContext using only the Singleton EJB security
     * configuration and not what it might have 'inherited' in the execution stack during a Postconstruct method call.
     * @see org.jboss.as.ejb3.security.NonPropagatingSecurityContextInterceptorFactory
     */
    private boolean propagate = true;

    public SimpleSecurityManager() {
    }

    public SimpleSecurityManager(SimpleSecurityManager delegate) {
        this.securityManagement = delegate.securityManagement;
        this.propagate = false;
    }

    private ISecurityManagement securityManagement = null;

    private PrivilegedAction<SecurityContext> securityContext() {
        return new PrivilegedAction<SecurityContext>() {
            public SecurityContext run() {
                return SecurityContextAssociation.getSecurityContext();
            }
        };
    }

    private SecurityContext establishSecurityContext(final String securityDomain) {
        // Do not use SecurityFactory.establishSecurityContext, its static init is broken.
        try {
            final SecurityContext securityContext = SecurityContextFactory.createSecurityContext(securityDomain);
            if(securityManagement == null)
                throw SecurityMessages.MESSAGES.securityManagementNotInjected();
            securityContext.setSecurityManagement(securityManagement);
            SecurityContextAssociation.setSecurityContext(securityContext);
            return securityContext;
        } catch (Exception e) {
            throw SecurityMessages.MESSAGES.securityException(e);
        }
    }

    public void setSecurityManagement(ISecurityManagement iSecurityManagement){
        securityManagement = iSecurityManagement;
    }

    public Principal getCallerPrincipal() {
        final SecurityContext securityContext = doPrivileged(securityContext());
        if (securityContext == null) {
            return getUnauthenticatedIdentity().asPrincipal();
        }
        /*
         * final Principal principal = getPrincipal(securityContext.getUtil().getSubject());
         */
        Principal principal = securityContext.getIncomingRunAs();
        if (principal == null)
            principal = getPrincipal(securityContext.getSubjectInfo().getAuthenticatedSubject());
        if (principal == null)
            return getUnauthenticatedIdentity().asPrincipal();
        return principal;
    }

    public Subject getSubject() {
        final SecurityContext securityContext = doPrivileged(securityContext());
        if (securityContext != null) {
            return securityContext.getSubjectInfo().getAuthenticatedSubject();
        }
        return null;
    }

    /**
     * Get the Principal given the authenticated Subject. Currently the first principal that is not of type {@code Group} is
     * considered or the single principal inside the CallerPrincipal group.
     *
     * @param subject
     * @return the authenticated principal
     */
    private Principal getPrincipal(Subject subject) {
        Principal principal = null;
        Principal callerPrincipal = null;
        if (subject != null) {
            Set<Principal> principals = subject.getPrincipals();
            if (principals != null && !principals.isEmpty()) {
                for (Principal p : principals) {
                    if (!(p instanceof Group) && principal == null) {
                        principal = p;
                    }
                    if (p instanceof Group) {
                        Group g = Group.class.cast(p);
                        if (g.getName().equals("CallerPrincipal") && callerPrincipal == null) {
                            Enumeration<? extends Principal> e = g.members();
                            if (e.hasMoreElements())
                                callerPrincipal = e.nextElement();
                        }
                    }
                }
            }
        }
        return callerPrincipal == null ? principal : callerPrincipal;
    }

    @Override
    public boolean isCallerInRole(String ejbName, Object mappedRoles, Map<String, Collection<String>> roleLinks, String... roleNames) {
        return isCallerInRole(ejbName, PolicyContext.getContextID(), mappedRoles, roleLinks, roleNames);
    }


    /**
     * @param ejbName The name of the EJB component where isCallerInRole was invoked.
     * @param incommingMappedRoles The principal vs roles mapping (if any). Can be null.
     * @param roleLinks The role link map where the key is a alias role name and the value is the collection of
     *                  role names, that alias represents. Can be null.
     * @param roleNames The role names for which the caller is being checked for
     * @return true if the user is in <b>any</b> one of the <code>roleNames</code>. Else returns false
     */
    public boolean isCallerInRole(final String ejbName, final String policyContextID, final Object incommingMappedRoles,
                                  final Map<String, Collection<String>> roleLinks, final String... roleNames) {

        final SecurityContext securityContext = doPrivileged(securityContext());
        if (securityContext == null) {
            return false;
        }

        final EJBResource resource = new EJBResource(new HashMap<String, Object>());
        resource.setEjbName(ejbName);
        resource.setPolicyContextID(policyContextID);
        resource.setCallerRunAsIdentity(securityContext.getIncomingRunAs());
        resource.setCallerSubject(securityContext.getUtil().getSubject());
        Principal userPrincipal = securityContext.getUtil().getUserPrincipal();
        resource.setPrincipal(userPrincipal);
        if (roleLinks != null) {
            final Set<SecurityRoleRef> roleRefs = new HashSet<SecurityRoleRef>();
            for (String key : roleLinks.keySet()) {
                Collection<String> values = roleLinks.get(key);
                if (values != null) {
                    for (String value : values)
                        roleRefs.add(new SecurityRoleRef(key, value));
                }
            }
            resource.setSecurityRoleReferences(roleRefs);
        }

        Map<String, Set<String>> previousRolesAssociationMap = null;
        try {
            // ensure the security roles association contains the incoming principal x roles map.
            if (incommingMappedRoles != null) {
                SecurityRolesMetaData rolesMetaData = (SecurityRolesMetaData) incommingMappedRoles;
                previousRolesAssociationMap = this.setSecurityRolesAssociation(rolesMetaData.getPrincipalVersusRolesMap());
            }

            AbstractEJBAuthorizationHelper helper = SecurityHelperFactory.getEJBAuthorizationHelper(securityContext);
            for (String roleName : roleNames) {
                if (helper.isCallerInRole(resource, roleName)) {
                    return true;
                }
            }
            return false;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            // reset the security roles association state.
            if (incommingMappedRoles != null) {
                this.setSecurityRolesAssociation(previousRolesAssociationMap);
            }
        }
    }

    public boolean authorize(String ejbName, CodeSource ejbCodeSource, String ejbMethodIntf, Method ejbMethod, Set<Principal> methodRoles, String contextID) {

        final SecurityContext securityContext = doPrivileged(securityContext());
        if (securityContext == null) {
            return false;
        }

        EJBResource resource = new EJBResource(new HashMap<String, Object>());
        resource.setEjbName(ejbName);
        resource.setEjbMethod(ejbMethod);
        resource.setEjbMethodInterface(ejbMethodIntf);
        resource.setEjbMethodRoles(new SimpleRoleGroup(methodRoles));
        resource.setCodeSource(ejbCodeSource);
        resource.setPolicyContextID(contextID);
        resource.setCallerRunAsIdentity(securityContext.getIncomingRunAs());
        resource.setCallerSubject(securityContext.getUtil().getSubject());
        Principal userPrincipal = securityContext.getUtil().getUserPrincipal();
        resource.setPrincipal(userPrincipal);

        try {
            AbstractEJBAuthorizationHelper helper = SecurityHelperFactory.getEJBAuthorizationHelper(securityContext);
            return helper.authorize(resource);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Must be called from within a privileged action.
     *
     * @param securityDomain
     * @param runAs
     * @param runAsPrincipal
     * @param extraRoles
     */
    public void push(final String securityDomain, final String runAs, final String runAsPrincipal, final Set<String> extraRoles) {
        boolean contextPushed = false;
        boolean securityContextEstablished = false;

        // TODO - Handle a null securityDomain here? Yes I think so.
        final SecurityContext previous = SecurityContextAssociation.getSecurityContext();
        try {
            contexts.push(previous);
            contextPushed = true;

            SecurityContext current = establishSecurityContext(securityDomain);
            securityContextEstablished = true;

            if (propagate && previous != null) {
                current.setSubjectInfo(previous.getSubjectInfo());
                current.setIncomingRunAs(previous.getOutgoingRunAs());
            }

            RunAs currentRunAs = current.getIncomingRunAs();
            boolean trusted = currentRunAs != null && currentRunAs instanceof RunAsIdentity;

            if (trusted == false) {
                /*
                 * We should only be switching to a context based on an identity from the Remoting connection
                 * if we don't already have a trusted identity - this allows for beans to reauthenticate as a
                 * different identity.
                 */
                boolean authenticated = false;
                if (SecurityActions.remotingContextIsSet()) {
                    // In this case the principal and credential will not have been set to set some random values.
                    SecurityContextUtil util = current.getUtil();

                    UserInfo userInfo = SecurityActions.remotingContextGetConnection().getUserInfo();
                    Principal p = null;
                    String credential = null;
                    Subject subject = null;
                    if (userInfo instanceof SubjectUserInfo) {
                        SubjectUserInfo sinfo = (SubjectUserInfo) userInfo;
                        subject = sinfo.getSubject();

                        Set<PasswordCredential> pcSet = subject.getPrivateCredentials(PasswordCredential.class);
                        if (pcSet.size() > 0) {
                            PasswordCredential pc = pcSet.iterator().next();
                            p = new SimplePrincipal(pc.getUserName());
                            credential = new String(pc.getCredential());
                            SecurityActions.remotingContextClear(); // Now that it has been used clear it.
                        }
                        if ((p == null || credential == null) && userInfo instanceof UniqueIdUserInfo) {
                            UniqueIdUserInfo uinfo = (UniqueIdUserInfo) userInfo;
                            p = new SimplePrincipal(sinfo.getUserName());
                            credential = uinfo.getId();
                            // In this case we do not clear the RemotingContext as it is still to be used
                            // here extracting the ID just ensures we are not continually calling the modules
                            // for each invocation.
                        }
                    }

                    if (p == null || credential == null) {
                        p = new SimplePrincipal(UUID.randomUUID().toString());
                        credential = UUID.randomUUID().toString();
                    }

                    util.createSubjectInfo(p, credential, subject);
                }

                // If we have a trusted identity no need for a re-auth.
                if (authenticated == false) {
                    authenticated = authenticate(current, null);
                }
                if (authenticated == false) {
                    // TODO - Better type needed.
                    throw SecurityMessages.MESSAGES.invalidUserException();
                }
            }

            if (runAs != null) {
                RunAs runAsIdentity = new RunAsIdentity(runAs, runAsPrincipal, extraRoles);
                current.setOutgoingRunAs(runAsIdentity);
            } else if (propagate &&  previous != null && previous.getOutgoingRunAs() != null) {
                // Ensure the propagation continues.
                current.setOutgoingRunAs(previous.getOutgoingRunAs());
            }
        } catch (Throwable t) {
            // cleanup whatever we had pushed or set in threadlocal
            if (securityContextEstablished) {
                SecurityContextAssociation.setSecurityContext(previous);
            }
            if (contextPushed) {
                contexts.pop();
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);

        }
    }

    public void push(final String securityDomain, String userName, char[] password, final Subject subject) {
        boolean contextPushed = false;
        boolean securityContextEstablished = false;
        final SecurityContext previous = SecurityContextAssociation.getSecurityContext();
        try {
            contexts.push(previous);
            contextPushed = true;

            SecurityContext current = establishSecurityContext(securityDomain);
            securityContextEstablished = true;

            if (propagate && previous != null) {
                current.setSubjectInfo(previous.getSubjectInfo());
                current.setIncomingRunAs(previous.getOutgoingRunAs());
            }

            RunAs currentRunAs = current.getIncomingRunAs();
            boolean trusted = currentRunAs != null && currentRunAs instanceof RunAsIdentity;

            if (trusted == false) {
                SecurityContextUtil util = current.getUtil();
                util.createSubjectInfo(new SimplePrincipal(userName), new String(password), subject);
                if (authenticate(current, subject) == false) {
                    throw SecurityMessages.MESSAGES.invalidUserException();
                }
            }

            if (propagate && previous != null && previous.getOutgoingRunAs() != null) {
                // Ensure the propagation continues.
                current.setOutgoingRunAs(previous.getOutgoingRunAs());
            }
        } catch (Throwable t) {
            // cleanup whatever we had pushed or set in threadlocal
            if (securityContextEstablished) {
                SecurityContextAssociation.setSecurityContext(previous);
            }
            if (contextPushed) {
                contexts.pop();
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private boolean authenticate(SecurityContext context, Subject subject) {
        SecurityContextUtil util = context.getUtil();
        SubjectInfo subjectInfo = context.getSubjectInfo();
        if (subject == null) {
            subject = new Subject();
        }
        Principal principal = util.getUserPrincipal();
        Principal auditPrincipal = principal;
        Object credential = util.getCredential();
        Identity unauthenticatedIdentity = null;

        boolean authenticated = false;
        if (principal == null) {
            unauthenticatedIdentity = getUnauthenticatedIdentity();
            subjectInfo.addIdentity(unauthenticatedIdentity);
            auditPrincipal = unauthenticatedIdentity.asPrincipal();
            subject.getPrincipals().add(auditPrincipal);
            authenticated = true;
        }

        if (authenticated == false) {
            AuthenticationManager authenticationManager = context.getAuthenticationManager();
            authenticated = authenticationManager.isValid(principal, credential, subject);
        }
        if (authenticated == true) {
            subjectInfo.setAuthenticatedSubject(subject);
        }

        AuditManager auditManager = context.getAuditManager();
        if (auditManager != null) {
            audit(authenticated ? AuditLevel.SUCCESS : AuditLevel.FAILURE, auditManager, auditPrincipal);
        }

        return authenticated;
    }

    // TODO - Base on configuration.
    // Also the spec requires a container representation of an unauthenticated identity so providing
    // at least a default is not optional.
    private Identity getUnauthenticatedIdentity() {
        return new SimpleIdentity("anonymous");
    }

    /**
     * Must be called from within a privileged action.
     */
    public void pop() {
        final SecurityContext sc = contexts.pop();
        SecurityContextAssociation.setSecurityContext(sc);
    }

    /**
     * Sends information to the {@code AuditManager}.
     * @param level
     * @param auditManager
     * @param userPrincipal
     */
    private void audit(String level, AuditManager auditManager, Principal userPrincipal) {
        AuditEvent auditEvent = new AuditEvent(AuditLevel.SUCCESS);
        Map<String, Object> ctxMap = new HashMap<String, Object>();
        ctxMap.put("principal", userPrincipal != null ? userPrincipal : "null");
        ctxMap.put("Source", getClass().getCanonicalName());
        ctxMap.put("Action", "authentication");
        auditEvent.setContextMap(ctxMap);
        auditManager.audit(auditEvent);
    }

    private Map<String, Set<String>> setSecurityRolesAssociation(Map<String, Set<String>> mappedRoles) {
        if (System.getSecurityManager() == null) {
            Map<String, Set<String>> previousMappedRoles = SecurityRolesAssociation.getSecurityRoles();
            SecurityRolesAssociation.setSecurityRoles(mappedRoles);
            return previousMappedRoles;
        }
        else {
            SetSecurityRolesAssociationAction action = new SetSecurityRolesAssociationAction(mappedRoles);
            return AccessController.doPrivileged(action);
        }
    }

    private static class SetSecurityRolesAssociationAction implements PrivilegedAction<Map<String, Set<String>>> {

        private final Map<String, Set<String>> mappedRoles;

        SetSecurityRolesAssociationAction(Map<String, Set<String>> mappedRoles) {
            this.mappedRoles = mappedRoles;
        }

        @Override
        public Map<String, Set<String>> run() {
            Map<String, Set<String>> previousMappedRoles = SecurityRolesAssociation.getSecurityRoles();
            SecurityRolesAssociation.setSecurityRoles(this.mappedRoles);
            return previousMappedRoles;
        }
    }
}
