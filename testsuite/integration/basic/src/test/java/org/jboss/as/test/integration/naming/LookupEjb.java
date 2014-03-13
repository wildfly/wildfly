package org.jboss.as.test.integration.naming;

import com.sun.jndi.ldap.LdapCtx;

import javax.annotation.Resource;
import javax.ejb.Stateless;

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
