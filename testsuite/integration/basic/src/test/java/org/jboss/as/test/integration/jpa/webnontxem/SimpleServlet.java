/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.webnontxem;

import java.io.IOException;
import java.io.Writer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Scott Marlow (based on Carlo's webejb test)
 *         <p>
 *         Exercise the code paths reached when a container managed entity manager is injected into a servlet
 */

@WebServlet(name = "SimpleServlet", urlPatterns = {"/simple"})
public class SimpleServlet extends HttpServlet {
    @PersistenceUnit(unitName = "mypc")
    EntityManagerFactory emf;   // servlet is in control of when obtained entity managers are closed

    @PersistenceContext(unitName = "mypc", name = "persistence/em")
    EntityManager em;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String msg = req.getParameter("input");

        Writer writer = resp.getWriter();

        // This is how a servlet could get an entity manager from an entity manager factory
        EntityManager localEntityManager = emf.createEntityManager();
        Query query = localEntityManager.createQuery("select count(*) from Employee");
        Long count = (Long) query.getSingleResult();
        localEntityManager.close();

        // a new underlying entity manager will be obtained to handle creating query
        // the em will stay open until the container closes it.
        query = em.createQuery("select count(*) from Employee");
        count = (Long) query.getSingleResult();

        writer.write(count.toString());
    }
}
