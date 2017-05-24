package org.jboss.as.test.manualmode.ejb.client.outbound.connection.security;

import javax.ejb.Remote;

/**
 * @author <a href="mailto:mjurc@redhat.com">Michal Jurc</a> (c) 2017 Red Hat, Inc.
 */
@Remote
public interface WhoAmI {

    String whoAmI();

}
