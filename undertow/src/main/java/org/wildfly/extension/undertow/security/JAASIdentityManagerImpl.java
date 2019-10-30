/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.security;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;

import org.wildfly.extension.undertow.security.digest.DigestCredential;

import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.idm.X509CertificateCredential;

import java.security.Principal;
import java.security.acl.Group;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.security.AuthenticationManager;
import org.jboss.security.AuthorizationManager;
import org.jboss.security.SecurityConstants;
import org.jboss.security.SecurityContext;
import org.jboss.security.auth.callback.CallbackHandlerPolicyContextHandler;
import org.jboss.security.auth.callback.DigestCallbackHandler;
import org.jboss.security.callbacks.SecurityContextCallbackHandler;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class JAASIdentityManagerImpl implements IdentityManager {

    private final SecurityDomainContext securityDomainContext;

    public JAASIdentityManagerImpl(final SecurityDomainContext securityDomainContext) {
        this.securityDomainContext = securityDomainContext;
    }

    @Override
    public Account verify(Account account) {
        // This method is called for previously verfified accounts so just accept it for the moment.
        if (!(account instanceof AccountImpl)) {
            UndertowLogger.ROOT_LOGGER.tracef("Account is not an AccountImpl", account);
            return null;
        }
        AccountImpl accountImpl = (AccountImpl) account;
        return verifyCredential(accountImpl, accountImpl.getCredential());
    }

    @Override
    public Account verify(String id, Credential credential) {
        AccountImpl account = getAccount(id);
        if (credential instanceof DigestCredential) {
            DigestCredential digestCredential = (DigestCredential) credential;
            DigestCallbackHandler handler = new DigestCallbackHandler(id, digestCredential.getNonce(), digestCredential.getNonceCount(),
                    digestCredential.getClientNonce(), digestCredential.getQop(), digestCredential.getRealm(),
                    digestCredential.getHA2());
            CallbackHandlerPolicyContextHandler.setCallbackHandler(handler);

            return verifyCredential(account, digestCredential.getClientDigest());
        } else if(credential instanceof PasswordCredential) {
            final char[] password = ((PasswordCredential) credential).getPassword();
            // The original array may be cleared, this integration relies on it being cached for use later.
            final char[] duplicate = Arrays.copyOf(password, password.length);
            return verifyCredential(account, duplicate);
        } else {
            return verifyCredential(account, credential);
        }
    }

    @Override
    public Account verify(Credential credential) {
        if (credential instanceof X509CertificateCredential) {
            X509CertificateCredential certCredential = (X509CertificateCredential) credential;
            X509Certificate certificate = certCredential.getCertificate();
            AccountImpl account = getAccount(certificate.getSubjectDN().getName());

            return verifyCredential(account, certificate);
        }
        throw new IllegalArgumentException("Parameter must be a X509CertificateCredential");
    }

    private AccountImpl getAccount(final String id) {
        return new AccountImpl(id);
    }

    private Account verifyCredential(final AccountImpl account, final Object credential) {
        final AuthenticationManager authenticationManager = securityDomainContext.getAuthenticationManager();
        final AuthorizationManager authorizationManager = securityDomainContext.getAuthorizationManager();
        final SecurityContext sc = SecurityActions.getSecurityContext();
        Principal incomingPrincipal = account.getOriginalPrincipal();
        Subject subject = new Subject();
        try {
            boolean isValid = authenticationManager.isValid(incomingPrincipal, credential, subject);
            if (isValid) {
                UndertowLogger.ROOT_LOGGER.tracef("User: %s is authenticated", incomingPrincipal);
                if (sc == null) {
                    throw UndertowLogger.ROOT_LOGGER.noSecurityContext();
                }
                Principal userPrincipal = getPrincipal(subject);
                sc.getUtil().createSubjectInfo(incomingPrincipal, credential, subject);
                SecurityContextCallbackHandler scb = new SecurityContextCallbackHandler(sc);
                RoleGroup roles = authorizationManager.getSubjectRoles(subject, scb);
                Set<String> roleSet = new HashSet<>();
                for (Role role : roles.getRoles()) {
                    roleSet.add(role.getRoleName());
                }
                return new AccountImpl(userPrincipal, roleSet, credential, account.getOriginalPrincipal());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                        if (g.getName().equals(SecurityConstants.CALLER_PRINCIPAL_GROUP) && callerPrincipal == null) {
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


}
