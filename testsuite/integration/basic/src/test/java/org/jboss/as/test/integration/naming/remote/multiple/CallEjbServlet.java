package org.jboss.as.test.integration.naming.remote.multiple;

import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.network.NetworkUtils;

@WebServlet(name = "CallEjbServlet", urlPatterns = {"/CallEjbServlet"})
public class CallEjbServlet extends HttpServlet {
    @EJB
    MyEjb ejb;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Context ctx = null;
        try {
            Properties env = new Properties();
            String address = System.getProperty("node0", "localhost");
            // format possible IPv6 address
            address = NetworkUtils.formatPossibleIpv6Address(address);
            env.put(Context.PROVIDER_URL, "remote+http://" + address + ":8080");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            ctx = new InitialContext(env);

            // ensure it's actually connected to the server
            MyObject obj = (MyObject) ctx.lookup("loc/stub");

            // call the EJB which also does remote lookup
            Writer writer = resp.getWriter();
            writer.write(ejb.doIt());
            writer.write(obj.doIt("Hello"));
            writer.flush();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
