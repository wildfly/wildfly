package org.jboss.as.test.integration.ejb.transaction.descriptor;

import jakarta.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface TransactionRemote {

    int transactionStatus();

    int transactionStatus2();
}
