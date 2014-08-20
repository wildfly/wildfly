package org.jboss.as.test.integration.ejb.remote.suspend;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface Echo {

    String echo(String val);

}
