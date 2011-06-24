/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.demos.webapp.archive;

import java.io.IOException;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@WebServlet(name="SimpleServlet", urlPatterns={"/simple", "/other"})
public class SimpleServlet extends HttpServlet {

    private static final long serialVersionUID = -2579304186167063651L;

    Logger log = Logger.getLogger(SimpleServlet.class.getName());

    private static volatile boolean initialized;

    @Resource(mappedName="java:jboss/datasources/ExampleDS")
    DataSource ds;

    @Resource(mappedName="java:/ConnectionFactory")
    QueueConnectionFactory cf;

    @Resource(mappedName="queue/test")
    Queue queue;

    QueueConnection conn;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String value = req.getParameter("value");
        log.info("Received request");

        Writer writer = resp.getWriter();
        writer.write("Servlet Response\n");
        Object o = req.getAttribute("Filtered");
        writer.write("Filtered: " + (o != null) + "\n");
        writer.write("Entries in database:\n");

        Connection sqlConn = null;
        PreparedStatement stmt = null;
        try {
            log.info("Inserting '" + value + "' into database");
            sqlConn = ds.getConnection();
            stmt = sqlConn.prepareCall("INSERT INTO WebAppTestTable (value) VALUES ('" + value + "')");
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            safeClose(stmt);
            safeClose(sqlConn);
        }

        ResultSet rs = null;
        try {
            sqlConn = ds.getConnection();
            stmt = sqlConn.prepareStatement("select * from WebAppTestTable");
            rs = stmt.executeQuery();
            while (rs.next()) {
                writer.write(rs.getInt(1));
                writer.write(" - ");
                writer.write(rs.getString(2) + "\n");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            safeClose(rs);
            safeClose(stmt);
            safeClose(sqlConn);
        }

        QueueSession session = null;
        try {
            session = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueSender sender = session.createSender(queue);
            TextMessage msg = session.createTextMessage(value);
            log.info("Sending '" + value + "' via queue");
            sender.send(msg);
        } catch (JMSException e) {
            throw new ServletException(e);
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.doGet(req, resp);
    }

    @Override
    public void init() throws ServletException {
        if (!initialized) {
            synchronized (this) {
                if (initialized)
                    return;

                initialized = true;

                //TODO Remove this once injection is working
                try {
                    InitialContext context = new InitialContext();
                    ds = (DataSource)context.lookup("java:jboss/datasources/ExampleDS");
                    cf = (QueueConnectionFactory)context.lookup("java:/ConnectionFactory");
                    queue = (Queue)context.lookup("queue/test");
                    conn = cf.createQueueConnection();
                } catch (Exception e) {
                    throw new ServletException(e);
                }

                //Create our table
                Connection sqlConn = null;
                CallableStatement stmt = null;
                try {
                    sqlConn = ds.getConnection();
                    stmt = sqlConn.prepareCall("CREATE TABLE WebAppTestTable (id INTEGER IDENTITY, value VARCHAR(255))");
                    stmt.execute();
                } catch (Exception e) {
                    throw new ServletException(e);
                } finally {
                    safeClose(stmt);
                    safeClose(sqlConn);
                }
                log.info("Created table");
            }
        }
    }

    @Override
    public void destroy() {
        //Drop our table
        Connection sqlConn = null;
        CallableStatement stmt = null;
        try {
            sqlConn = ds.getConnection();
            log.info("Dropping table");
            stmt = sqlConn.prepareCall("DROP TABLE WebAppTestTable");
            stmt.execute();
        } catch (SQLException e) {
            // AutoGenerated
            e.printStackTrace();
        } finally {
            safeClose(stmt);
            safeClose(sqlConn);
        }

        try {
            conn.stop();
            conn.close();
        } catch (JMSException ignore) {
        }
    }

    private void safeClose(Statement c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Exception ignore) {
        }
    }

    private void safeClose(Connection c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Exception ignore) {
        }
    }

    private void safeClose(ResultSet c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Exception ignore) {
        }
    }
}
