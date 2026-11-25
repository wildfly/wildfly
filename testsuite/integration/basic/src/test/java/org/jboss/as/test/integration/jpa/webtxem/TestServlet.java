/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.webtxem;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.jboss.as.test.integration.jpa.webtxem.entity.WebJPAEntity;

/**
 * Test servlet used by {@link WebJPATestCase}. Entity {@link WebJPAEntity} is read or written, type of operation depends on
 * parameter: mode=write or mode=read.
 *
 * @author Zbyněk Roubalík
 */

@WebServlet(name = "TestServlet", urlPatterns = {"/test"})
public class TestServlet extends HttpServlet {

    @PersistenceContext(unitName = "web_jpa_pc")
    EntityManager em;
    Long databaseGeneratedID = null;
    @Resource
    private UserTransaction tx;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {

            tx.begin();

            try {
                String mode = req.getParameter("mode");

                if (mode.equals("write")) {
                    WebJPAEntity f = new WebJPAEntity();
                    f.setName("WebJPAEntity One");
                    f = em.merge(f);
                    // Since we are using a @GeneratedValue id, we want ORM to create it for us on persist operation
                    // and then we'll store it in the test for further entity manipulations.
                    //
                    // While previous versions of ORM may have allowed users to set the value of a generated id, now the rules are stricter.
                    // Pasting from https://docs.hibernate.org/orm/6.6/migration-guide/:
                    // "
                    // Merge versioned entity when row is deleted
                    //
                    // Previously, merging a detached entity resulted in a SQL insert whenever there was no matching row in the database (for example, if the object had been deleted in another transaction). This behavior was unexpected and violated the rules of optimistic locking.
                    //
                    // An OptimisticLockException is now thrown when it is possible to determine that an entity is definitely detached, but there is no matching row. For this determination to be possible, the entity must have either:
                    //
                    // *  a generated @Id field, or
                    // *  a non-primitive @Version field.
                    //
                    // For entities which have neither, it’s impossible to distinguish a new instance from a deleted detached instance, and there is no change from the previous behavior.
                    // "
                    //
                    // Previously, we specified the WebJPAEntity id value but as mentioned ^ because the @Id field is
                    // also annotated with @GeneratedValue we can no longer do that,
                    // i.e. trying to set an id explicitly: `f.setId(1L);` will lead to an exception in this case.
                    databaseGeneratedID = f.getId();

                } else if (mode.equals("read")) {

                    WebJPAEntity f = em.find(WebJPAEntity.class, databaseGeneratedID);
                    resp.setContentType("text/plain");
                    PrintWriter out = resp.getWriter();
                    out.print(f.getName());
                    out.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
                tx.setRollbackOnly();
                throw e;
            } finally {
                if (tx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                    tx.rollback();
                } else {
                    tx.commit();
                }
            }

        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

}

