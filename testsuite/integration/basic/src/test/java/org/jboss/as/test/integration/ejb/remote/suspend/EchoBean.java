package org.jboss.as.test.integration.ejb.remote.suspend;

import javax.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class EchoBean implements Echo {
    @Override
    public String echo(String val) {
        return val;
    }
}
