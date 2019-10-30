package org.jboss.as.test.manualmode.ejb.client.outbound.connection.security;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2017 Red Hat, Inc.
 */
@Stateless
public class IntermediateWhoAmI implements WhoAmI {

    @EJB(lookup = "ejb:/inbound-module/ServerWhoAmI!org.jboss.as.test.manualmode.ejb.client.outbound.connection.security.WhoAmI")
    private WhoAmI whoAmIBean;

    public String whoAmI() {
        return whoAmIBean.whoAmI();
    }

}
