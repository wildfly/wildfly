package org.jboss.as.test.integration.ejb.transaction.descriptor;

import javax.ejb.Local;

/**
 * @author Stuart Douglas
 */
@Local
public interface TransactionLocal {

    int transactionStatus();


    int transactionStatus2();

}
