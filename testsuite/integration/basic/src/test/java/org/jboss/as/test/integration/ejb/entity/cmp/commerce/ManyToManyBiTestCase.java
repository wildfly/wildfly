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
package org.jboss.as.test.integration.ejb.entity.cmp.commerce;

import java.util.Iterator;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CmpTestRunner.class)
public class ManyToManyBiTestCase extends AbstractCmpTest {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "cmp-commerce.jar");
        jar.addPackage(ManyToManyBiTestCase.class.getPackage());
        jar.addAsManifestResource("ejb/entity/cmp/commerce/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/entity/cmp/commerce/jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    private ProductHome getProductHome() {
        try {
            return (ProductHome) iniCtx.lookup("java:module/ProductEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.ProductHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getProduct: " + e.getMessage());
        }
        return null;
    }

    private ProductCategoryHome getProductCategoryHome() {
        try {
            return (ProductCategoryHome)iniCtx.lookup("java:module/ProductCategoryEJB!org.jboss.as.test.integration.ejb.entity.cmp.commerce.ProductCategoryHome");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception in getProductCategory: " + e.getMessage());
        }
        return null;
    }

    private Product a1;
    private Product a2;
    private Product a3;
    private Product a4;
    private Product a5;

    private ProductCategory b1;
    private ProductCategory b2;
    private ProductCategory b3;
    private ProductCategory b4;
    private ProductCategory b5;

    public void setUpEjb() throws Exception {
        ProductHome productHome = getProductHome();
        ProductCategoryHome productCategoryHome = getProductCategoryHome();

        // clean out the db
        deleteAllProducts(productHome);
        deleteAllProductCategories(productCategoryHome);

        // setup the before change part of the test
        beforeChange(productHome, productCategoryHome);
    }

    private void beforeChange(
            ProductHome productHome,
            ProductCategoryHome productCategoryHome) throws Exception {

        // Before change:
        a1 = productHome.create();
        a2 = productHome.create();
        a3 = productHome.create();
        a4 = productHome.create();
        a5 = productHome.create();

        b1 = productCategoryHome.create();
        b2 = productCategoryHome.create();
        b3 = productCategoryHome.create();
        b4 = productCategoryHome.create();
        b5 = productCategoryHome.create();

        a1.getProductCategories().add(b1);
        a1.getProductCategories().add(b2);
        a2.getProductCategories().add(b1);
        a2.getProductCategories().add(b2);
        a2.getProductCategories().add(b3);
        a3.getProductCategories().add(b2);
        a3.getProductCategories().add(b3);
        a3.getProductCategories().add(b4);
        a4.getProductCategories().add(b3);
        a4.getProductCategories().add(b4);
        a4.getProductCategories().add(b5);
        a5.getProductCategories().add(b4);
        a5.getProductCategories().add(b5);

        assertTrue(a1.getProductCategories().contains(b1));
        assertTrue(a1.getProductCategories().contains(b2));
        assertTrue(a2.getProductCategories().contains(b1));
        assertTrue(a2.getProductCategories().contains(b2));
        assertTrue(a2.getProductCategories().contains(b3));
        assertTrue(a3.getProductCategories().contains(b2));
        assertTrue(a3.getProductCategories().contains(b3));
        assertTrue(a3.getProductCategories().contains(b4));
        assertTrue(a4.getProductCategories().contains(b3));
        assertTrue(a4.getProductCategories().contains(b4));
        assertTrue(a4.getProductCategories().contains(b5));
        assertTrue(a5.getProductCategories().contains(b4));
        assertTrue(a5.getProductCategories().contains(b5));

        assertTrue(b1.getProducts().contains(a1));
        assertTrue(b1.getProducts().contains(a2));
        assertTrue(b2.getProducts().contains(a1));
        assertTrue(b2.getProducts().contains(a2));
        assertTrue(b2.getProducts().contains(a3));
        assertTrue(b3.getProducts().contains(a2));
        assertTrue(b3.getProducts().contains(a3));
        assertTrue(b3.getProducts().contains(a4));
        assertTrue(b4.getProducts().contains(a3));
        assertTrue(b4.getProducts().contains(a4));
        assertTrue(b4.getProducts().contains(a5));
        assertTrue(b5.getProducts().contains(a4));
        assertTrue(b5.getProducts().contains(a5));
    }


    @Test // a1.setB(a3.getB());
    public void test_a1SetB_a3GetB() {
        // Change:
        a1.setProductCategories(a3.getProductCategories());

        // Expected result:
        assertTrue(!a1.getProductCategories().contains(b1));
        assertTrue(a1.getProductCategories().contains(b2));
        assertTrue(a1.getProductCategories().contains(b3));
        assertTrue(a1.getProductCategories().contains(b4));

        assertTrue(a2.getProductCategories().contains(b1));
        assertTrue(a2.getProductCategories().contains(b2));
        assertTrue(a2.getProductCategories().contains(b3));

        assertTrue(a3.getProductCategories().contains(b2));
        assertTrue(a3.getProductCategories().contains(b3));
        assertTrue(a3.getProductCategories().contains(b4));

        assertTrue(a4.getProductCategories().contains(b3));
        assertTrue(a4.getProductCategories().contains(b4));
        assertTrue(a4.getProductCategories().contains(b5));

        assertTrue(a5.getProductCategories().contains(b4));
        assertTrue(a5.getProductCategories().contains(b5));


        assertTrue(!b1.getProducts().contains(a1));
        assertTrue(b1.getProducts().contains(a2));

        assertTrue(b2.getProducts().contains(a1));
        assertTrue(b2.getProducts().contains(a2));
        assertTrue(b2.getProducts().contains(a3));

        assertTrue(b3.getProducts().contains(a1));
        assertTrue(b3.getProducts().contains(a2));
        assertTrue(b3.getProducts().contains(a3));
        assertTrue(b3.getProducts().contains(a4));

        assertTrue(b4.getProducts().contains(a1));
        assertTrue(b4.getProducts().contains(a3));
        assertTrue(b4.getProducts().contains(a4));
        assertTrue(b4.getProducts().contains(a5));

        assertTrue(b5.getProducts().contains(a4));
        assertTrue(b5.getProducts().contains(a5));
    }

    @Test // a1.getB().add(b3);
    public void test_a1GetB_addB3() {
        // Change:
        a1.getProductCategories().add(b3);

        // Expected result:
        assertTrue(a1.getProductCategories().contains(b1));
        assertTrue(a1.getProductCategories().contains(b2));
        assertTrue(a1.getProductCategories().contains(b3));

        assertTrue(a2.getProductCategories().contains(b1));
        assertTrue(a2.getProductCategories().contains(b2));
        assertTrue(a2.getProductCategories().contains(b3));

        assertTrue(a3.getProductCategories().contains(b2));
        assertTrue(a3.getProductCategories().contains(b3));
        assertTrue(a3.getProductCategories().contains(b4));

        assertTrue(a4.getProductCategories().contains(b3));
        assertTrue(a4.getProductCategories().contains(b4));
        assertTrue(a4.getProductCategories().contains(b5));

        assertTrue(a5.getProductCategories().contains(b4));
        assertTrue(a5.getProductCategories().contains(b5));


        assertTrue(b1.getProducts().contains(a1));
        assertTrue(b1.getProducts().contains(a2));

        assertTrue(b2.getProducts().contains(a1));
        assertTrue(b2.getProducts().contains(a2));
        assertTrue(b2.getProducts().contains(a3));

        assertTrue(b3.getProducts().contains(a1));
        assertTrue(b3.getProducts().contains(a2));
        assertTrue(b3.getProducts().contains(a3));
        assertTrue(b3.getProducts().contains(a4));

        assertTrue(b4.getProducts().contains(a3));
        assertTrue(b4.getProducts().contains(a4));
        assertTrue(b4.getProducts().contains(a5));

        assertTrue(b5.getProducts().contains(a4));
        assertTrue(b5.getProducts().contains(a5));
    }

    @Test // a2.getB().remove(b2);
    public void test_a2GetB_removeB2() {
        // Change:
        a2.getProductCategories().remove(b2);

        // Expected result:
        assertTrue(a1.getProductCategories().contains(b1));
        assertTrue(a1.getProductCategories().contains(b2));

        assertTrue(a2.getProductCategories().contains(b1));
        assertTrue(!a2.getProductCategories().contains(b2));
        assertTrue(a2.getProductCategories().contains(b3));

        assertTrue(a3.getProductCategories().contains(b2));
        assertTrue(a3.getProductCategories().contains(b3));
        assertTrue(a3.getProductCategories().contains(b4));

        assertTrue(a4.getProductCategories().contains(b3));
        assertTrue(a4.getProductCategories().contains(b4));
        assertTrue(a4.getProductCategories().contains(b5));

        assertTrue(a5.getProductCategories().contains(b4));
        assertTrue(a5.getProductCategories().contains(b5));


        assertTrue(b1.getProducts().contains(a1));
        assertTrue(b1.getProducts().contains(a2));

        assertTrue(b2.getProducts().contains(a1));
        assertTrue(!b2.getProducts().contains(a2));
        assertTrue(b2.getProducts().contains(a3));

        assertTrue(b3.getProducts().contains(a2));
        assertTrue(b3.getProducts().contains(a3));
        assertTrue(b3.getProducts().contains(a4));

        assertTrue(b4.getProducts().contains(a3));
        assertTrue(b4.getProducts().contains(a4));
        assertTrue(b4.getProducts().contains(a5));

        assertTrue(b5.getProducts().contains(a4));
        assertTrue(b5.getProducts().contains(a5));
    }

    public void tearDownEJB() throws Exception {
    }

    public void deleteAllProducts(ProductHome productHome) throws Exception {
        // delete all Products
        Iterator currentProducts = productHome.findAll().iterator();
        while (currentProducts.hasNext()) {
            Product p = (Product) currentProducts.next();
            p.remove();
        }
    }

    public void deleteAllProductCategories(ProductCategoryHome productCategoryHome) throws Exception {
        // delete all ProductCategories
        Iterator currentProductCategories = productCategoryHome.findAll().iterator();
        while (currentProductCategories.hasNext()) {
            ProductCategory pc = (ProductCategory) currentProductCategories.next();
            pc.remove();
        }
    }


}



