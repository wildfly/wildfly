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
                    f.setId(1L);
                    f.setName("WebJPAEntity One");
                    em.merge(f);

                } else if (mode.equals("read")) {

                    WebJPAEntity f = em.find(WebJPAEntity.class, 1L);
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

