/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.classloader.leak;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Random;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@WebServlet(urlPatterns={"/ClassloaderServlet"}, loadOnStartup = 1)
public class ClassLoaderServlet extends HttpServlet {
            
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        // create weak references for the module and its classloader
        ClassLoader cLoader = this.getClass().getClassLoader();
        ClassLoaderRef.setClassLoaderRef(new WeakReference<ClassLoader>(cLoader));
        
        if (cLoader instanceof ModuleClassLoader) {
            ModuleClassLoader mLoader = (ModuleClassLoader) cLoader;
            Module module = mLoader.getModule();
            
            ClassLoaderRef.setModuleRef(new WeakReference<Module>(module));
            ClassLoaderRef.setModuleIdentifier(module.getIdentifier().toString());          
        }

    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // store a value to HttpSession
        HttpSession session = request.getSession(true);
        session.setAttribute("test", new Random().nextDouble());
        
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>ClassloaderServlet</title></head>");
        out.println("<body>Done</body>");
        out.println("</html>");
        out.close();                
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
}
