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

package org.jboss.as.test.integration.jaxb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.jboss.as.test.integration.jaxb.bindings.PurchaseOrderType;

@WebServlet (urlPatterns = JAXBUsageServlet.URL_PATTERN)
public class JAXBUsageServlet extends HttpServlet {

    public static final String URL_PATTERN = "/jaxbServlet";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            Unmarshaller unmarshaller = JAXBContext.newInstance("org.jboss.as.test.integration.jaxb.bindings").createUnmarshaller();
            String xml = "<?xml version=\"1.0\"?>\n"

                    + "<purchaseOrder orderDate=\"1999-10-20\">\n" +
                    "   <shipTo country=\"US\">\n" +
                    "      <name>Alice Smith</name>\n" +
                    "      <street>123 Maple Street</street>\n" +
                    "      <city>Mill Valley</city>\n" +
                    "      <state>CA</state>\n" +
                    "      <zip>90952</zip>\n" +
                    "   </shipTo>\n" +
                    "   <billTo country=\"US\">\n" +
                    "      <name>Robert Smith</name>\n" +
                    "      <street>8 Oak Avenue</street>\n" +
                    "      <city>Old Town</city>\n" +
                    "      <state>PA</state>\n" +
                    "      <zip>95819</zip>\n" +
                    "   </billTo>\n" +
                    "   <comment>Hurry, my lawn is going wild!</comment>\n" +
                    "   <items>\n" +
                    "      <item partNum=\"872-AA\">\n" +
                    "         <productName>Lawnmower</productName>\n" +
                    "         <quantity>1</quantity>\n" +
                    "         <USPrice>148.95</USPrice>\n" +
                    "         <comment>Confirm this is electric</comment>\n" +
                    "      </item>\n" +
                    "      <item partNum=\"926-AA\">\n" +
                    "         <productName>Baby Monitor</productName>\n" +
                    "         <quantity>1</quantity>\n" +
                    "         <USPrice>39.98</USPrice>\n" +
                    "         <shipDate>1999-05-21</shipDate>\n" +
                    "      </item>\n" +
                    "   </items>\n" +
                    "</purchaseOrder>";

            PurchaseOrderType order = ((JAXBElement<PurchaseOrderType>)unmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))).getValue();
            resp.getOutputStream().println(order.getShipTo().getCity());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

    }


}
