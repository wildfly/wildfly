/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
