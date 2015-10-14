package org.jboss.as.test.manualmode.weld.extension;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ryan Emerson
 */
public class BeforeShutdownExtension implements Extension {

    private UserTransaction userTx = null;
    private TransactionSynchronizationRegistry txSynchRegistry = null;

    void lookupBeforeShutdown(@Observes final BeforeShutdown beforeShutdown) throws Exception {
        try {
            userTx = lookup("java:jboss/UserTransaction");
            userTx.getStatus();

            txSynchRegistry = lookup("java:jboss/TransactionSynchronizationRegistry");
            txSynchRegistry.getTransactionStatus();
        } catch (Exception e) {
            writeOutput(e);
            throw e;
        }
        writeOutput(null);
    }

    private <T> T lookup(String jndiName) {
        try {
            InitialContext initialContext = new InitialContext();
            return (T) initialContext.lookup(jndiName);
        } catch (NamingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Necessary so that BeforeShutdownJNDILookupTestCase can see the outcome of the BeforeShutdown JNDI lookups.
    private void writeOutput(Exception exception) throws Exception {
        List<String> output = new ArrayList<>();
        if (exception != null) {
            output.add("Exception");
            output.add(exception + "," + Arrays.toString(exception.getStackTrace()));
        } else {
            output.add("UserTransaction");
            output.add(userTx.toString());
            output.add("TransactionSynchronizationRegistry");
            output.add(txSynchRegistry.toString());
        }
        File parent = new File(BeforeShutdownJNDILookupTestCase.TEST_URL).getParentFile();
        if (!parent.exists())
            parent.mkdirs();
        Files.write(Paths.get("", BeforeShutdownJNDILookupTestCase.TEST_URL), output, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }
}
