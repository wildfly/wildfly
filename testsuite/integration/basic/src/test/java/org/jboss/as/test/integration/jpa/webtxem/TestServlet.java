/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jpa.webtxem;

import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.jboss.as.test.integration.jpa.hibernate.entity.Flight;

/**
 * Test servlet used by {@link WebJPATestCase}. Entity {@link Flight} is read or written, type of operation depends on
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
                    Flight f = new Flight();
                    f.setId(new Long(1));
                    f.setName("Flight number one");
                    em.merge(f);

                } else if (mode.equals("read")) {

                    Flight f = em.find(Flight.class, new Long(1));
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
                tx.commit();
            }

        } catch (Exception e) {
            throw new ServletException(e);
        }

    }

}

