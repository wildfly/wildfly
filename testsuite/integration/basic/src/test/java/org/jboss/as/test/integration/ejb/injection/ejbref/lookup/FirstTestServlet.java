/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.ejb.injection.ejbref.lookup;

import java.io.IOException;
import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/test1")
public class FirstTestServlet extends HttpServlet {

    @EJB(lookup = "java:comp/env/RemoteInterfaceBean")
    RemoteInterface remoteBean;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        write(response, String.valueOf(remoteBean.ping()));
    }

    private static void write(HttpServletResponse writer, String message) {
        try {
            writer.getWriter().write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
