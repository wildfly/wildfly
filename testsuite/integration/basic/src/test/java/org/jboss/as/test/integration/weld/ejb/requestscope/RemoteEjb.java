package org.jboss.as.test.integration.weld.ejb.requestscope;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Stateless
public class RemoteEjb implements RemoteInterface {

    @Inject
    private RequestScopedBean bean;

    @Override
    public String getMessage() {
        return bean.getMessage();
    }
}
