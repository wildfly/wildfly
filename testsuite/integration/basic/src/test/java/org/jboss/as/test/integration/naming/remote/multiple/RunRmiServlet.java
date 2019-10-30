package org.jboss.as.test.integration.naming.remote.multiple;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.network.NetworkUtils;

@WebServlet(name = "RunRmiServlet", urlPatterns = {"/RunRmiServlet"})
public class RunRmiServlet extends HttpServlet {
    private List<Context> contexts = new ArrayList<Context>();

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        MyObject stub = lookup();
        PrintWriter writer = resp.getWriter();
        try {
            writer.print(stub.doIt("Test"));
        } finally {
            writer.close();
        }
    }

    protected MyObject lookup() throws ServletException {
        try {
            Properties env = new Properties();
            String address = System.getProperty("node0", "localhost");
            // format possible IPv6 address
            address = NetworkUtils.formatPossibleIpv6Address(address);
            env.put(Context.PROVIDER_URL, "remote+http://" + address + ":8080");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
            Context ctx = new InitialContext(env);
            try {
                return (MyObject) ctx.lookup("loc/stub");
            } finally {
                //ctx.close();
                contexts.add(ctx);
            }
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }

    public void destroy() {
        for (Context c : contexts) {
            try {
                c.close();
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }
        contexts.clear();
    }
}
