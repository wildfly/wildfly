package org.jboss.as.test.integration.ee.injection.resource.jndi.bad;

import jakarta.annotation.Resource;
import jakarta.annotation.Resources;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

@Resources({@Resource(
        name = Constants.JNDI_NAME_BAD,
        type = org.jboss.as.test.integration.ee.injection.resource.jndi.bad.ResourceEJB.class, lookup = Constants.JNDI_NAME_GLOBAL)})
@Stateless
@Remote(SampleEJB.class)
public class SampleEJBImpl implements SampleEJB {

    @Resource(lookup = Constants.JNDI_NAME_GLOBAL)
    ResourceEJB resEJB;

    @Override
    public String sayHello() throws Exception {
        return null;
    }
}
