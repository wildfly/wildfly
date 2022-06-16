package org.jboss.as.test.integration.jpa.secondlevelcache;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class RollbackException extends RuntimeException {

    private static final long serialVersionUID = 2967914874533141967L;

}
