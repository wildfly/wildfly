/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.security;

import java.io.Serializable;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
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

    private SecurityDomain securityDomain;
    private ThreadLocal<SecurityIdentity> currentIdentity = new ThreadLocal<SecurityIdentity>();

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
            throw new java.lang.IllegalArgumentException("only string password accepted");
        }
        SecurityIdentity identity = authenticate(username, (String) password);
        if (identity == null) {
            return false;
        }
        SubjectUtil.fromSecurityIdentity(identity, subject);
        currentIdentity.set(identity);
        return true;
    }

    public void runAs(Callable<Void> action) throws Exception {
        if (currentIdentity.get() != null) {
            //there is no security constrains in servlet and directly with jaas
            currentIdentity.get().runAs(action);
        } else {
            //undertow's ElytronRunAsHandler will propagate the SecurityIndentity to SecurityDomain and directly run this action
            action.call();
        }
    }
    @Override
    public void pushSubjectContext(Subject arg0, Principal arg1, Object arg2) {

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
            evidence.destroy();
        }
        return null;
    }
    //TODO:create a util to build subject from SecurityIdentity
    public class SimplePrincipal implements Principal, Serializable {
        private static final long serialVersionUID = -7703975471290155466L;
        private String name;

        public SimplePrincipal(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Principal name can not be null");
            }
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SimplePrincipal)) {
                return false;
            }

            return name.equals(((SimplePrincipal) obj).name);
        }

        public int hashCode() {
            return name.hashCode();
        }

        public String toString() {
            return name;
        }
    }

    public class SimpleGroup extends SimplePrincipal implements Group {
        private static final long serialVersionUID = -6684520988041121094L;
        private Set<Principal> members = new HashSet<Principal>();

        public SimpleGroup(String groupName) {
            super(groupName);
        }

        public SimpleGroup(String groupName, Principal member) {
            super(groupName);
            members.add(member);
        }

        public boolean isMember(Principal p) {
            return members.contains(p);
        }

        public boolean addMember(Principal p) {
            return members.add(p);
        }

        public Enumeration<? extends Principal> members() {

            final Iterator<Principal> it = members.iterator();

            return new Enumeration<Principal>() {

                public boolean hasMoreElements() {
                    return it.hasNext();
                }

                public Principal nextElement() {
                    return it.next();
                }

            };
        }

        public boolean removeMember(Principal p) {
            return members.remove(p);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof SimpleGroup)) {
                return false;
            }
            SimpleGroup other = (SimpleGroup) obj;
            return getName().equals(other.getName()) && members.equals(other.members);
        }

        public int hashCode() {
            return getName().hashCode() + 37 * members.hashCode();
        }
    }

}
