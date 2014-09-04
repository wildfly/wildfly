package org.jboss.as.test.integration.ejb.remote.suspend;

import javax.ejb.Stateful;

/**
 * @author Stuart Douglas
 */
@Stateful
public class EchoBean implements Echo {
    @Override
    public String echo(String val) {
        return val;
    }
}
