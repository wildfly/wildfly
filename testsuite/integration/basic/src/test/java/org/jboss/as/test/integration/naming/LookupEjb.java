package org.jboss.as.test.integration.naming;

import javax.annotation.Resource;
import javax.ejb.Stateless;

import com.sun.jndi.ldap.LdapCtx;

/**
 * @author Stuart Douglas
 */
@Stateless
public class LookupEjb {

    @Resource(lookup = "java:global/ldap/dc=jboss,dc=org")
    private LdapCtx ldapCtx;

    public LdapCtx getLdapCtx() {
        return ldapCtx;
    }
}
