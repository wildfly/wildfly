package org.jboss.as.messaging;

import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import org.hornetq.core.security.CheckType;
import org.hornetq.core.security.Role;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SecurityContextFactory;
import org.jboss.security.SimplePrincipal;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

public class HornetQSecurityManagerAS7 implements org.hornetq.spi.core.security.HornetQSecurityManager {
    private SecurityDomainContext securityDomainContext;
    private String defaultUser = null;
    private String defaultPassword = null;

    public HornetQSecurityManagerAS7(SecurityDomainContext sdc) {
        securityDomainContext = sdc;
        defaultUser = HornetQDefaultCredentials.getUsername();
        defaultPassword = HornetQDefaultCredentials.getPassword();
    }

    @Override
    public boolean validateUser(String username, String password) {
        if (defaultUser.equals(username) && defaultPassword.equals(password))
            return true;

        if (securityDomainContext == null)
            throw MESSAGES.securityDomainContextNotSet();

        return securityDomainContext.getAuthenticationManager().isValid(new SimplePrincipal(username), password, new Subject());
    }

    @Override
    public boolean validateUserAndRole(String username, String password, Set<Role> roles, CheckType checkType) {
        if (defaultUser.equals(username) && defaultPassword.equals(password))
            return true;

        if (securityDomainContext == null)
            throw MESSAGES.securityDomainContextNotSet();

        Subject subject = new Subject();

        // The authentication call here changes the subject and that subject must be used later.  That is why we don't call validateUser(String, String) here.
        boolean authenticated = securityDomainContext.getAuthenticationManager().isValid(new SimplePrincipal(username), password, subject);

        if (authenticated) {
            pushSecurityContext(subject, new SimplePrincipal(username), password);
            Set<Principal> principals = new HashSet<Principal>();
            for (Role role : roles) {
                if (checkType.hasRole(role)) {
                    principals.add(new SimplePrincipal(role.getName()));
                }
            }

            authenticated = securityDomainContext.getAuthorizationManager().doesUserHaveRole(new SimplePrincipal(username), principals);

            popSecurityContext();
        }

        return authenticated;
    }

    public void pushSecurityContext(final Subject subject, final Principal principal, final Object credential) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            public Void run() {
                SecurityContext securityContext = SecurityContextAssociation.getSecurityContext();
                if (securityContext == null) {
                    securityContext = createSecurityContext(subject, principal, credential, securityDomainContext.getAuthenticationManager().getSecurityDomain());
                } else {
                    securityContext.getUtil().createSubjectInfo(principal, credential, subject);
                }
                setSecurityContextOnAssociation(securityContext);
                return null;
            }
        });
    }

    private static void setSecurityContextOnAssociation(final SecurityContext sc) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                SecurityContextAssociation.setSecurityContext(sc);
                return null;
            }
        });
    }

    private static void popSecurityContext() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {

            @Override
            public Void run() {
                SecurityContextAssociation.clearSecurityContext();
                return null;
            }
        });
    }

    private static SecurityContext createSecurityContext(final Subject subject, final Principal principal, final Object credential, final String domain) {
        return AccessController.doPrivileged(new PrivilegedAction<SecurityContext>() {

            @Override
            public SecurityContext run() {
                try {
                    return SecurityContextFactory.createSecurityContext(principal, credential, subject, domain);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void addUser(String s, String s1) {
    }

    @Override
    public void removeUser(String s) {
    }

    @Override
    public void addRole(String s, String s1) {
    }

    @Override
    public void removeRole(String s, String s1) {
    }

    @Override
    public void setDefaultUser(String s) {
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public boolean isStarted() {
        return false;
    }
}
