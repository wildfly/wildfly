package org.jboss.as.test.integration.web.valve;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

public class MyValve
    extends ValveBase {

    private String mystring = null;

    public void setMystring(String mystring) {
        this.mystring = mystring;
    }

    public void invoke(Request request, Response response)
        throws IOException, ServletException {
        getNext().invoke(request, response);
    }
}

