package org.jboss.as.test.integration.naming;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import javax.naming.ldap.LdapContext;

/**
 * @author Stuart Douglas
 */
@Stateless
public class LookupEjb {

    @Resource(lookup = "java:global/ldap/dc=jboss,dc=org")
    private LdapContext ldapCtx;

    public LdapContext getLdapCtx() {
        return ldapCtx;
    }
}
