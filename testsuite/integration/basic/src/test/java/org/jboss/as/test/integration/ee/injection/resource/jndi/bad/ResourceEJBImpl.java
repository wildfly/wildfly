package org.jboss.as.test.integration.ee.injection.resource.jndi.bad;

import jakarta.ejb.Stateless;

@Stateless
public class ResourceEJBImpl implements ResourceEJB {

    @Override
    public String echo(String param) {
        return param + "......" + param + "..." + param;

    }
}

