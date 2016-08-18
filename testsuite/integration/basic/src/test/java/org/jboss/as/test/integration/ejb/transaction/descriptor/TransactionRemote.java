package org.jboss.as.test.integration.ejb.transaction.descriptor;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface TransactionRemote {

    int transactionStatus();

    int transactionStatus2();
}
