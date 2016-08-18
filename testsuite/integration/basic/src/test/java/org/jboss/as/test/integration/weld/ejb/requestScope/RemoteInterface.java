package org.jboss.as.test.integration.weld.ejb.requestScope;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface RemoteInterface {

    String getMessage();

}
