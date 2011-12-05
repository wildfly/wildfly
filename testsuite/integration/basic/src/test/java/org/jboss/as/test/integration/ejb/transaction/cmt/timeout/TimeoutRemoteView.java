package org.jboss.as.test.integration.ejb.transaction.cmt.timeout;

import java.util.concurrent.TimeUnit;
import javax.ejb.Remote;
import org.jboss.ejb3.annotation.TransactionTimeout;

@Remote
public interface TimeoutRemoteView {

    public int getBeanTimeout();

    public int getBeanMethodTimeout();

    @TransactionTimeout(value=7, unit=TimeUnit.SECONDS)
    public int getRemoteMethodTimeout();
}
