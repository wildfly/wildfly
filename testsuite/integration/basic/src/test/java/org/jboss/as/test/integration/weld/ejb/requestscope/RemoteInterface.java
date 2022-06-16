package org.jboss.as.test.integration.weld.ejb.requestscope;

import jakarta.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface RemoteInterface {

    String getMessage();

}
