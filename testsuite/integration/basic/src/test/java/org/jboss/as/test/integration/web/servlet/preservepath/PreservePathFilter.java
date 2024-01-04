/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.servlet.preservepath;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PreservePathFilter implements Filter {


   @Override
   public void doFilter(ServletRequest servletRequest,
                        ServletResponse servletResponse,
                        FilterChain filterChain) throws IOException, ServletException {

      filterChain.doFilter(servletRequest, servletResponse);

      HttpServletRequest request = (HttpServletRequest) servletRequest;

      String tmpFolder = request.getParameter("path");
      File file = new File(tmpFolder + "/output.txt");
      file.createNewFile();
      try( BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
         String text = "servletPath: " + request.getServletPath() +
                 "\nrequestUrl: " + request.getRequestURL().toString() +
                 "\nrequestUri: " + request.getRequestURI();
         bufferedWriter.write(text);
      }
   }
}
