/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.compat.jpa.eclipselink;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;

/**
 * @author Scott Marlow
 *
 * Exercise the code paths reached when a container managed entity manager is injected into a servlet
 */

@WebServlet(name="SimpleServlet", urlPatterns={"/simple"})
public class SimpleServlet extends HttpServlet {
    @PersistenceUnit(unitName = "web_hibernate3_pc")
    EntityManagerFactory emf;   // servlet is in control of when obtained entity managers are closed

    @PersistenceContext(unitName = "web_hibernate3_pc", name="persistence/em")
    EntityManager em;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String msg = req.getParameter("input");

        Writer writer = resp.getWriter();

        // This is how a servlet could get an entity manager from an entity manager factory
        EntityManager localEntityManager = emf.createEntityManager();
        Query query = localEntityManager.createQuery("select count(*) from WebLink");
        Long count = (Long)query.getSingleResult();
        localEntityManager.close();

        // a new underlying entity manager will be obtained to handle creating query
        // the em will stay open until the container closes it.
        query = em.createQuery("select count(*) from WebLink");
        count = (Long)query.getSingleResult();

        writer.write(count.toString());
    }
}
