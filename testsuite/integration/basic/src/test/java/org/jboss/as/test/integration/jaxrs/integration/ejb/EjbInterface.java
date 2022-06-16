package org.jboss.as.test.integration.jaxrs.integration.ejb;

import jakarta.transaction.SystemException;

/**
 * @author Stuart Douglas
 */
public interface EjbInterface {
    String getMessage() throws SystemException;
}
