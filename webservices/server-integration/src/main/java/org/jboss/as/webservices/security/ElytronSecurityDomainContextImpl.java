/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.security;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.security.auth.Subject;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.util.SubjectUtil;
import org.jboss.wsf.spi.security.SecurityDomainContext;
import org.wildfly.security.auth.server.RealmUnavailableException;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.auth.server.ServerAuthenticationContext;
import org.wildfly.security.evidence.PasswordGuessEvidence;

public class ElytronSecurityDomainContextImpl implements SecurityDomainContext {

    private final SecurityDomain securityDomain;
    private final ThreadLocal<SecurityIdentity> currentIdentity = new ThreadLocal<SecurityIdentity>();

    public ElytronSecurityDomainContextImpl(SecurityDomain securityDomain) {
        this.securityDomain = securityDomain;
    }

    //TODO:deprecate this after elytron
    @Override
    public boolean doesUserHaveRole(Principal principal, Set<Principal> principals) {
        return true;
    }
    //TODO:refactor/deprecate this after elytron
    @Override
    public String getSecurityDomain() {
        return this.securityDomain.toString();
    }

    @Override
    public SecurityDomain getElytronSecurityDomain() {
        return this.securityDomain;
    }

    //TODO:refactor/deprecate this after elytron?
    @Override
    public Set<Principal> getUserRoles(Principal principal) {
        return null;
    }

    @Override
    public boolean isValid(Principal principal, Object password, Subject subject) {
        if (subject == null) {
            subject = new Subject();
        }
        String username = principal.getName();
        if (!(password instanceof String)) {
            throw WSLogger.ROOT_LOGGER.onlyStringPasswordAccepted();
        }
        SecurityIdentity identity = authenticate(username, (String) password);
        if (identity == null) {
            return false;
        }
        this.currentIdentity.set(identity);
        SubjectUtil.fromSecurityIdentity(identity, subject);
        return true;
    }

    public void runAs(Callable<Void> action) throws Exception {
        final SecurityIdentity ci = currentIdentity.get();
        if (ci != null) {
            //there is no security constrains in servlet and directly with jaas
            try {
                ci.runAs(action);
            } finally {
                currentIdentity.remove();
            }
        } else {
            //undertow's ElytronRunAsHandler will propagate the SecurityIndentity to SecurityDomain and directly run this action
            action.call();
        }
    }
    @Override
    public void pushSubjectContext(Subject subject, Principal pincipal, Object credential) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                if (credential != null) {
                    subject.getPrivateCredentials().add(credential);
                }
                SecurityIdentity securityIdentity = SubjectUtil.convertToSecurityIdentity(subject, pincipal, securityDomain,
                        "ejb");
                currentIdentity.set(securityIdentity);
                return null;
            }
        });
    }

    private SecurityIdentity authenticate(final String username, final String password) {

        ServerAuthenticationContext context = this.securityDomain.createNewAuthenticationContext();
        PasswordGuessEvidence evidence = new PasswordGuessEvidence(password != null ? password.toCharArray() : null);
        try {
            context.setAuthenticationName(username);
            if (context.verifyEvidence(evidence)) {
                if (context.authorize()) {
                    context.succeed();
                    return context.getAuthorizedIdentity();
                } else {
                    context.fail();
                    WSLogger.ROOT_LOGGER.failedAuthorization(username);
                }
            } else {
                context.fail();
                WSLogger.ROOT_LOGGER.failedAuthentication(username);
            }
        } catch (IllegalArgumentException | IllegalStateException | RealmUnavailableException e) {
            context.fail();
            WSLogger.ROOT_LOGGER.failedAuthenticationWithException(e, username, e.getMessage());
        } finally {
            if (!context.isDone()) context.fail(); //prevent leaks of RealmIdentity instances
            evidence.destroy();
        }
        return null;
    }

    @Override
    public void cleanupSubjectContext() {
        currentIdentity.remove();
    }

}
