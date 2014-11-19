package org.jboss.as.test.integration.ejb.transaction.descriptor;

import javax.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface TransactionRemote {

    public int transactionStatus();

    public int transactionStatus2();
}
