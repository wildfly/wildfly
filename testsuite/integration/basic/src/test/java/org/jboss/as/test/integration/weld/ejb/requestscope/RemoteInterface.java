package org.jboss.as.test.integration.weld.ejb.requestscope;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface RemoteInterface {

    String getMessage();

}
