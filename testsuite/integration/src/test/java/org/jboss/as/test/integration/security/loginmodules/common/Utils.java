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
package org.jboss.as.test.integration.security.loginmodules.common;

import org.jboss.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.security.MessageDigest;

/**
 * @author Jan Lanik
 *
 * Common utilities for login modules tests.
 */
public class Utils {

   public static String hash(String target, String algorithm, Coding coding) {
      MessageDigest md = null;
      try {
         md = MessageDigest.getInstance(algorithm);
      } catch (Exception e) {
         e.printStackTrace();
      }
      byte[] bytes = target.getBytes();
      byte[] byteHash = md.digest(bytes);

      String encodedHash = null;

      switch (coding) {
         case BASE_64:
            encodedHash = Base64.encodeBytes(byteHash);
            break;
         case HEX:
            encodedHash = toHex(byteHash);
            break;
         default:
            throw new IllegalArgumentException("Unsuported coding:" + coding.name());
      }

      return encodedHash;
   }


   public static String toHex(byte[] bytes)
    {
       StringBuffer sb = new StringBuffer(bytes.length * 2);
       for (int i = 0; i < bytes.length; i++)
       {
          byte b = bytes[i];
          // top 4 bits
          char c = (char)((b >> 4) & 0xf);
          if(c > 9)
             c = (char)((c - 10) + 'a');
          else
             c = (char)(c + '0');
          sb.append(c);
          // bottom 4 bits
          c = (char)(b & 0xf);
          if (c > 9)
             c = (char)((c - 10) + 'a');
          else
             c = (char)(c + '0');
          sb.append(c);
       }
       return sb.toString();
    }

   public static URL getResource(String name) {
      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      return tccl.getResource(name);
   }
}
