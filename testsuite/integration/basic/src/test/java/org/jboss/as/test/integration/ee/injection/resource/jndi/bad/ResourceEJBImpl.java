package org.jboss.as.test.integration.ee.injection.resource.jndi.bad;

import javax.ejb.Stateless;

@Stateless
public class ResourceEJBImpl implements ResourceEJB {

    @Override
    public String echo(String param) {
        return param + "......" + param + "..." + param;

    }
}

