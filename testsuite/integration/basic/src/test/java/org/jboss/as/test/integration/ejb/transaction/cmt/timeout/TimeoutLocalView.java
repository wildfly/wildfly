package org.jboss.as.test.integration.ejb.transaction.cmt.timeout;

import java.util.concurrent.TimeUnit;
import javax.ejb.Local;
import org.jboss.ejb3.annotation.TransactionTimeout;

@Local
@TransactionTimeout(value=8000, unit=TimeUnit.MILLISECONDS)
public interface TimeoutLocalView {

    public int getLocalViewTimeout();
}
