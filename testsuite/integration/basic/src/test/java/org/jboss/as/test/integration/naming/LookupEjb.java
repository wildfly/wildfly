package org.jboss.as.test.integration.naming;

import javax.annotation.Resource;
import javax.ejb.Stateless;
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
