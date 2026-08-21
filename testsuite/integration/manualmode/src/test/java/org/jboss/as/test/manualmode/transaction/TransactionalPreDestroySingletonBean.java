/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.transaction;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.transaction.UserTransaction;
import javax.naming.InitialContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
public class TransactionalPreDestroySingletonBean {

    public static final String RESULT_FILE_NAME = "predestroy-result.txt";

    @PreDestroy
    void onPreDestroy() {
        List<String> output = new ArrayList<>();
        try {
            UserTransaction ut = (UserTransaction)
                    new InitialContext().lookup("java:jboss/UserTransaction");
            ut.begin();
            int status = ut.getStatus();
            ut.commit();
            output.add("SUCCESS");
            output.add("status=" + status);
        } catch (Exception e) {
            output.add("EXCEPTION");
            output.add(e + "," + Arrays.toString(e.getStackTrace()));
        }

        try {
            Path resultPath = Paths.get(System.getProperty("jboss.server.data.dir"), RESULT_FILE_NAME);
            Files.write(resultPath, output,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
        }
    }
}
