/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.manualmode.transaction.recovery;

import org.jboss.as.test.integration.transactions.PersistentTestXAResource;
import org.jboss.as.test.integration.transactions.TestXAResource;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.TransactionManager;
import java.io.IOException;

/**
 * Servlet which receives the HTTP call, starts the transaction and process the 2PC at the commit.
 * The first enlisted XAResource kills the JVM.
 *
 * The result is a prepared record at side of the XAResource and no record in the Narayana object store.
 * The transaction recovery should be rolling back the resource later.
 */
@WebServlet(name="TransactionRecoverOperationEndPoint", urlPatterns={"/transactionCallPrepareAndCrash"})
public class TransactionRecoverOperationEndPoint extends HttpServlet {
    @EJB(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            tm.begin();
            tm.getTransaction().enlistResource( // on 2PC prepares and crashes, recovery should go with rollback
                    new PersistentTestXAResource(PersistentTestXAResource.PersistentTestAction.AFTER_PREPARE_CRASH_JVM));
            tm.getTransaction().enlistResource(new TestXAResource()); // need second resource to process with 2PC
            tm.commit();
        } catch (Exception e) {
            throw new ServletException("Cannot work with transaction or enlist TestXAResource", e);
        }
    }
}
