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

package org.jboss.as.test.integration.ee.jmx.property;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xnio.IoUtils;

/**
 * @author baranowb
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JMXPropertyEditorsTestCase {

    private static final String SAR_DEPLOMENT_NAME = "property-editors-beans";
    private static final String SAR_DEPLOMENT_FILE = SAR_DEPLOMENT_NAME + ".sar";

    @ArquillianResource
    private Deployer deployer;

    @ContainerResource
    private ManagementClient managementClient;

    private MBeanServerConnection connection;
    private JMXConnector connector;
    private static final String USER_SYS_PROP = System.getProperty("os.name","linux").contains("indows")?"USERNAME":"USER";

    @Before
    public void initialize() throws Exception {
        connection = getMBeanServerConnection();
        Assert.assertNotNull(connection);
    }

    @After
    public void closeConnection() throws Exception {
        connection = null;
        IoUtils.safeClose(connector);
    }

    private MBeanServerConnection getMBeanServerConnection() throws IOException {
        final String address = managementClient.getMgmtAddress()+":"+managementClient.getMgmtPort();
        connector = JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:http-remoting-jmx://"+address));
        return connector.getMBeanServerConnection();

    }

    private static Asset createServiceAsset(String attributeName, String attributeValue) {
        return new StringAsset(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<server xmlns=\"urn:jboss:service:7.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"urn:jboss:service:7.0 jboss-service_7_0.xsd\">"
                        + "<mbean code=\"org.jboss.as.test.integration.ee.jmx.property.WithProperties\" name=\"test:service=WithProperties"
                        + attributeName + "\">" + "<attribute name=\"" + attributeName + "\">" + attributeValue
                        + "</attribute>" + "</mbean>" + "</server>");

    }

    private static JavaArchive createArchive(String prefix) {
        // jar
        final JavaArchive jmxSAR = ShrinkWrap.create(JavaArchive.class, prefix + "-" + SAR_DEPLOMENT_FILE);
        jmxSAR.addClass(WithPropertiesMBean.class);
        jmxSAR.addClass(WithProperties.class);

        return jmxSAR;
    }

    @Deployment(name = "AtomicBoolean", managed = false)
    public static Archive<?> deploymentAtomicBoolean() {

        // jar
        final JavaArchive jmxSAR = createArchive("AtomicBoolean");

        Asset asset = createServiceAsset("AtomicBoolean", "true");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testAtomicBoolean() throws Exception {
        try {
            deployer.deploy("AtomicBoolean");
            performTest("AtomicBoolean", new AtomicBoolean(true), new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    AtomicBoolean a1 = (AtomicBoolean) o1;
                    AtomicBoolean a2 = (AtomicBoolean) o2;

                    return a1.get() == a2.get() ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("AtomicBoolean");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Deployment(name = "AtomicInteger", managed = false)
    public static Archive<?> deploymentAtomicInteger() {

        // jar
        final JavaArchive jmxSAR = createArchive("AtomicInteger");

        Asset asset = createServiceAsset("AtomicInteger", "3");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testAtomicInteger() throws Exception {
        try {
            deployer.deploy("AtomicInteger");
            performTest("AtomicInteger", new AtomicInteger(3), new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    AtomicInteger a1 = (AtomicInteger) o1;
                    AtomicInteger a2 = (AtomicInteger) o2;

                    return a1.get() == a2.get() ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("AtomicInteger");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Deployment(name = "AtomicLong", managed = false)
    public static Archive<?> deploymentAtomicLong() {

        // jar
        final JavaArchive jmxSAR = createArchive("AtomicLong");

        Asset asset = createServiceAsset("AtomicLong", "2");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testAtomicLong() throws Exception {
        try {
            deployer.deploy("AtomicLong");
            performTest("AtomicLong", new AtomicLong(2), new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    AtomicLong a1 = (AtomicLong) o1;
                    AtomicLong a2 = (AtomicLong) o2;

                    return a1.get() == a2.get() ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("AtomicLong");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Deployment(name = "BigDecimal", managed = false)
    public static Archive<?> deploymentBigDecimal() {

        // jar
        final JavaArchive jmxSAR = createArchive("BigDecimal");

        Asset asset = createServiceAsset("BigDecimal", "100000000");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testBigDecimal() throws Exception {
        try {
            deployer.deploy("BigDecimal");
            performTest("BigDecimal", new BigDecimal(100000000));
        } finally {

            try {
                deployer.undeploy("BigDecimal");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Deployment(name = "BigInteger", managed = false)
    public static Archive<?> deploymentBigIntegerl() {

        // jar
        final JavaArchive jmxSAR = createArchive("BigInteger");

        Asset asset = createServiceAsset("BigInteger", "100000000");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testBigInteger() throws Exception {
        try {
            deployer.deploy("BigInteger");
            performTest("BigInteger", new BigInteger("100000000"));
        } finally {

            try {
                deployer.undeploy("BigInteger");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Deployment(name = "Boolean", managed = false)
    public static Archive<?> deploymentBoolean() {

        // jar
        final JavaArchive jmxSAR = createArchive("Boolean");

        Asset asset = createServiceAsset("Boolean", "true");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testBoolean() throws Exception {
        try {
            deployer.deploy("Boolean");
            performTest("Boolean", new Boolean(true));
        } finally {

            try {
                deployer.undeploy("Boolean");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Deployment(name = "BooleanArray", managed = false)
    public static Archive<?> deploymentBooleanArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("BooleanArray");

        Asset asset = createServiceAsset("BooleanArray", "true,false");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testBooleanArray() throws Exception {
        try {
            deployer.deploy("BooleanArray");
            performTest("BooleanArray", new boolean[] { true, false }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    boolean[] b1 = (boolean[]) o1;
                    boolean[] b2 = (boolean[]) o2;
                    return Arrays.equals(b1, b2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("BooleanArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Deployment(name = "Byte", managed = false)
    public static Archive<?> deploymentByte() {

        // jar
        final JavaArchive jmxSAR = createArchive("Byte");

        Asset asset = createServiceAsset("Byte", "1");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testByte() throws Exception {
        try {
            deployer.deploy("Byte");
            performTest("Byte", new Byte((byte) 1));
        } finally {

            try {
                deployer.undeploy("Byte");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Deployment(name = "ByteArray", managed = false)
    public static Archive<?> deploymentByteArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("ByteArray");

        Asset asset = createServiceAsset("ByteArray", "1,2,3");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testByteArray() throws Exception {
        try {
            deployer.deploy("ByteArray");
            performTest("ByteArray", new byte[] { 1, 2, 3 }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    byte[] b1 = (byte[]) o1;
                    byte[] b2 = (byte[]) o2;
                    return Arrays.equals(b1, b2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("ByteArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Deployment(name = "Char", managed = false)
    public static Archive<?> deploymentChar() {

        // jar
        final JavaArchive jmxSAR = createArchive("Char");

        Asset asset = createServiceAsset("Char", "R");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testChar() throws Exception {
        try {
            deployer.deploy("Char");
            performTest("Char", new Character('R'));
        } finally {

            try {
                deployer.undeploy("Char");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "CharacterArray", managed = false)
    public static Archive<?> deploymentCharacterArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("CharacterArray");

        Asset asset = createServiceAsset("CharacterArray", "R,R,X");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testCharacterArray() throws Exception {
        try {
            deployer.deploy("CharacterArray");
            performTest("CharacterArray", new char[] { 'R', 'R', 'X' }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    char[] a1 = (char[]) o1;
                    char[] a2 = (char[]) o2;
                    return Arrays.equals(a1, a2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("CharacterArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Clazz", managed = false)
    public static Archive<?> deploymentClazz() {

        // jar
        final JavaArchive jmxSAR = createArchive("Clazz");

        Asset asset = createServiceAsset("Clazz", "java.lang.String");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testClazz() throws Exception {
        try {
            deployer.deploy("Clazz");
            performTest("Clazz", String.class);
        } finally {

            try {
                deployer.undeploy("Clazz");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ClassArray", managed = false)
    public static Archive<?> deploymentClassArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("ClassArray");

        Asset asset = createServiceAsset("ClassArray", "java.lang.String,java.util.List");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testClassArray() throws Exception {
        try {
            deployer.deploy("ClassArray");
            performTest("ClassArray", new Class[] { String.class, List.class }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    Class[] a1 = (Class[]) o1;
                    Class[] a2 = (Class[]) o2;
                    return Arrays.equals(a1, a2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("ClassArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Document", managed = false)
    public static Archive<?> deploymentDocument() {

        // jar
        final JavaArchive jmxSAR = createArchive("Document");

        Asset asset = createServiceAsset("Document", "<document><element/><document>");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    @Ignore("JBossServiceXmlDescriptorParser does not support XML as attribs.")
    public void testDocument() throws Exception {
        try {
            deployer.deploy("Document");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            StringReader sr = new StringReader("<document><element/><document>");
            InputSource is = new InputSource(sr);
            Document d = db.parse(is);
            performTest("Document", d);
        } finally {

            try {
                deployer.undeploy("Document");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Double", managed = false)
    public static Archive<?> deploymentDouble() {

        // jar
        final JavaArchive jmxSAR = createArchive("Double");

        Asset asset = createServiceAsset("Double", "4");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testDouble() throws Exception {
        try {
            deployer.deploy("Double");
            performTest("Double", new Double(4));
        } finally {

            try {
                deployer.undeploy("Double");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Element", managed = false)
    public static Archive<?> deploymentElement() {

        // jar
        final JavaArchive jmxSAR = createArchive("Element");

        Asset asset = createServiceAsset("Element", "<element/>");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    @Ignore("JBossServiceXmlDescriptorParser does not support XML as attribs.")
    public void testElement() throws Exception {
        try {
            deployer.deploy("Element");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            StringReader sr = new StringReader("<element/>");
            InputSource is = new InputSource(sr);
            Document d = db.parse(is);
            performTest("Element", d.getDocumentElement());
        } finally {

            try {
                deployer.undeploy("Element");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "File", managed = false)
    public static Archive<?> deploymentFile() {

        // jar
        final JavaArchive jmxSAR = createArchive("File");

        Asset asset = createServiceAsset("File", "/I_DONT_EXIST/DUNNO");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testFile() throws Exception {
        try {
            deployer.deploy("File");
            performTest("File", new File("/I_DONT_EXIST/DUNNO").getAbsoluteFile());
        } finally {

            try {
                deployer.undeploy("File");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Float", managed = false)
    public static Archive<?> deploymentFloat() {

        // jar
        final JavaArchive jmxSAR = createArchive("Float");

        Asset asset = createServiceAsset("Float", "1.5");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testFloat() throws Exception {
        try {
            deployer.deploy("Float");
            performTest("Float", new Float("1.5"));
        } finally {

            try {
                deployer.undeploy("Float");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "FloatArray", managed = false)
    public static Archive<?> deploymentFloatArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("FloatArray");

        Asset asset = createServiceAsset("FloatArray", "1.5,2.5");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testFloatArray() throws Exception {
        try {
            deployer.deploy("FloatArray");
            performTest("FloatArray", new float[] { 1.5f, 2.5f }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    float[] a1 = (float[]) o1;
                    float[] a2 = (float[]) o2;
                    return Arrays.equals(a1, a2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("FloatArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "InetAddress", managed = false)
    public static Archive<?> deploymentInetAddress() {

        // jar
        final JavaArchive jmxSAR = createArchive("InetAddress");

        Asset asset = createServiceAsset("InetAddress", "10.10.10.1");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testInetAddress() throws Exception {
        try {
            deployer.deploy("InetAddress");
            performTest("InetAddress", InetAddress.getByAddress(new byte[] { 10, 10, 10, 1 }));
        } finally {

            try {
                deployer.undeploy("InetAddress");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "InetAddressArray", managed = false)
    public static Archive<?> deploymentInetAddressArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("InetAddressArray");

        Asset asset = createServiceAsset("InetAddressArray", "10.10.10.1,localhost");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testInetAddressArray() throws Exception {
        try {
            deployer.deploy("InetAddressArray");
            performTest("InetAddressArray", new InetAddress[] { InetAddress.getByAddress(new byte[] { 10, 10, 10, 1 }),
                    InetAddress.getByName("localhost") }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    InetAddress[] a1 = (InetAddress[]) o1;
                    InetAddress[] a2 = (InetAddress[]) o2;
                    return Arrays.equals(a1, a2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("InetAddressArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Integer", managed = false)
    public static Archive<?> deploymentInteger() {

        // jar
        final JavaArchive jmxSAR = createArchive("Integer");

        Asset asset = createServiceAsset("Integer", "1");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testInteger() throws Exception {
        try {
            deployer.deploy("Integer");
            performTest("Integer", new Integer("1"));
        } finally {

            try {
                deployer.undeploy("Integer");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "IntegerArray", managed = false)
    public static Archive<?> deploymentIntegerArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("IntegerArray");

        Asset asset = createServiceAsset("IntegerArray", "1,5,4");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testIntegerArray() throws Exception {
        try {
            deployer.deploy("IntegerArray");
            performTest("IntegerArray", new int[] { 1, 5, 4 }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    int[] a1 = (int[]) o1;
                    int[] a2 = (int[]) o2;
                    return Arrays.equals(a1, a2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("IntegerArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Locale", managed = false)
    public static Archive<?> deploymentLocale() {

        // jar
        final JavaArchive jmxSAR = createArchive("Locale");

        Asset asset = createServiceAsset("Locale", Locale.ENGLISH.toString());
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testLocale() throws Exception {
        try {
            deployer.deploy("Locale");
            performTest("Locale", Locale.ENGLISH);
        } finally {

            try {
                deployer.undeploy("Locale");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Long", managed = false)
    public static Archive<?> deploymentLong() {

        // jar
        final JavaArchive jmxSAR = createArchive("Long");

        Asset asset = createServiceAsset("Long", "14");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testLong() throws Exception {
        try {
            deployer.deploy("Long");
            performTest("Long", new Long(14));
        } finally {

            try {
                deployer.undeploy("Long");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "LongArray", managed = false)
    public static Archive<?> deploymentLongArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("LongArray");

        Asset asset = createServiceAsset("LongArray", "14,15");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testLongArray() throws Exception {
        try {
            deployer.deploy("LongArray");
            performTest("LongArray", new long[] { 14, 15 }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    long[] a1 = (long[]) o1;
                    long[] a2 = (long[]) o2;
                    return Arrays.equals(a1, a2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("LongArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ObjectBoolean", managed = false)
    public static Archive<?> deploymentObjectBoolean() {

        // jar
        final JavaArchive jmxSAR = createArchive("ObjectBoolean");

        Asset asset = createServiceAsset("ObjectBoolean", "true");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testObjectBoolean() throws Exception {
        try {
            deployer.deploy("ObjectBoolean");
            performTest("ObjectBoolean", new Boolean(true));
        } finally {

            try {
                deployer.undeploy("ObjectBoolean");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ObjectByte", managed = false)
    public static Archive<?> deploymentObjectByte() {

        // jar
        final JavaArchive jmxSAR = createArchive("ObjectByte");

        Asset asset = createServiceAsset("ObjectByte", "10");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testObjectByte() throws Exception {
        try {
            deployer.deploy("ObjectByte");
            performTest("ObjectByte", new Byte((byte) 10));
        } finally {

            try {
                deployer.undeploy("ObjectByte");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ObjectChar", managed = false)
    public static Archive<?> deploymentObjectCharacter() {

        // jar
        final JavaArchive jmxSAR = createArchive("ObjectChar");

        Asset asset = createServiceAsset("ObjectChar", "Z");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testObjectCharacter() throws Exception {
        try {
            deployer.deploy("ObjectChar");
            performTest("ObjectChar", new Character('Z'));
        } finally {

            try {
                deployer.undeploy("ObjectChar");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ObjectDouble", managed = false)
    public static Archive<?> deploymentObjectDouble() {

        // jar
        final JavaArchive jmxSAR = createArchive("ObjectDouble");

        Asset asset = createServiceAsset("ObjectDouble", "10");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testObjectDouble() throws Exception {
        try {
            deployer.deploy("ObjectDouble");
            performTest("ObjectDouble", new Double(10));
        } finally {

            try {
                deployer.undeploy("ObjectDouble");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ObjectFloat", managed = false)
    public static Archive<?> deploymentObjectFloat() {

        // jar
        final JavaArchive jmxSAR = createArchive("ObjectFloat");

        Asset asset = createServiceAsset("ObjectFloat", "10");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testObjectFloat() throws Exception {
        try {
            deployer.deploy("ObjectFloat");
            performTest("ObjectFloat", new Float(10));
        } finally {

            try {
                deployer.undeploy("ObjectFloat");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ObjectInteger", managed = false)
    public static Archive<?> deploymentObjectInteger() {

        // jar
        final JavaArchive jmxSAR = createArchive("ObjectInteger");

        Asset asset = createServiceAsset("ObjectInteger", "10");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testObjectInteger() throws Exception {
        try {
            deployer.deploy("ObjectInteger");
            performTest("ObjectInteger", new Integer(10));
        } finally {

            try {
                deployer.undeploy("ObjectInteger");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ObjectLong", managed = false)
    public static Archive<?> deploymentObjectLong() {

        // jar
        final JavaArchive jmxSAR = createArchive("ObjectLong");

        Asset asset = createServiceAsset("ObjectLong", "10");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testObjectLong() throws Exception {
        try {
            deployer.deploy("ObjectLong");
            performTest("ObjectLong", new Long(10));
        } finally {

            try {
                deployer.undeploy("ObjectLong");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ObjectShort", managed = false)
    public static Archive<?> deploymentObjectShort() {

        // jar
        final JavaArchive jmxSAR = createArchive("ObjectShort");

        Asset asset = createServiceAsset("ObjectShort", "10");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testObjectShort() throws Exception {
        try {
            deployer.deploy("ObjectShort");
            performTest("ObjectShort", new Short((short) 10));
        } finally {

            try {
                deployer.undeploy("ObjectShort");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Properties", managed = false)
    public static Archive<?> deploymentProperties() {

        // jar
        final JavaArchive jmxSAR = createArchive("Properties");

        Asset asset = createServiceAsset("Properties", "prop1=ugabuga\nprop2=HAHA\nenv=${env."+USER_SYS_PROP+"}");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testProperties() throws Exception {
        try {
            deployer.deploy("Properties");
            Properties props = new Properties();
            props.put("prop1", "ugabuga");
            props.put("prop2", "HAHA");
            props.put("env", System.getenv(USER_SYS_PROP));
            // props also dont override equals...
            performTest("Properties", props, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    Properties p1 = (Properties) o1;
                    Properties p2 = (Properties) o2;
                    System.err.println("["+p1.size()+"]:"+p1+"\n["+p2.size()+"]:"+p2);
                    System.err.println("------- x");
                    if (p1.size() != p2.size()) {
                        System.err.println("------- 0");
                        return 1;
                    }
                    System.err.println("------- 1");
                    if(!p1.keySet().containsAll(p2.keySet())){
                        System.err.println("------- 2");
                        return 1;
                    }

                    Set<Object> keys1 = p1.keySet();
                    for(Object key:keys1){
                        Object v1 = p1.get(key);
                        Object v2 = p2.get(key);
                        if(!v1.equals(v2)){
                            System.err.println("------- 3: "+v1+":"+v2);
                            return 1;
                        }
                    }
                    return 0;
                }

            });
        } finally {

            try {
                deployer.undeploy("Properties");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "Short", managed = false)
    public static Archive<?> deploymentShort() {

        // jar
        final JavaArchive jmxSAR = createArchive("Short");

        Asset asset = createServiceAsset("Short", "1");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testShort() throws Exception {
        try {
            deployer.deploy("Short");
            performTest("Short", new Short((short) 1));
        } finally {

            try {
                deployer.undeploy("Short");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "ShortArray", managed = false)
    public static Archive<?> deploymentShortArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("ShortArray");

        Asset asset = createServiceAsset("ShortArray", "1,20");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testShortArray() throws Exception {
        try {
            deployer.deploy("ShortArray");
            performTest("ShortArray", new short[] { 1, 20 }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    short[] a1 = (short[]) o1;
                    short[] a2 = (short[]) o2;
                    return Arrays.equals(a1, a2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("ShortArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "StringArray", managed = false)
    public static Archive<?> deploymentStringArray() {

        // jar
        final JavaArchive jmxSAR = createArchive("StringArray");

        Asset asset = createServiceAsset("StringArray", "1,20");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testStringArray() throws Exception {
        try {
            deployer.deploy("StringArray");
            performTest("StringArray", new String[] { "1", "20" }, new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    String[] a1 = (String[]) o1;
                    String[] a2 = (String[]) o2;
                    return Arrays.equals(a1, a2) ? 0 : 1;
                }
            });
        } finally {

            try {
                deployer.undeploy("StringArray");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "URI", managed = false)
    public static Archive<?> deploymentURI() {

        // jar
        final JavaArchive jmxSAR = createArchive("URI");

        Asset asset = createServiceAsset("URI", "http://nowhere.com");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testURI() throws Exception {
        try {
            deployer.deploy("URI");
            performTest("URI", new URI("http://nowhere.com"));
        } finally {

            try {
                deployer.undeploy("URI");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Deployment(name = "URL", managed = false)
    public static Archive<?> deploymentURL() {

        // jar
        final JavaArchive jmxSAR = createArchive("URL");

        Asset asset = createServiceAsset("URL", "http://nowhere.com");
        jmxSAR.addAsManifestResource(asset, "jboss-service.xml");

        System.err.println(jmxSAR.toString(true));
        return jmxSAR;
    }

    @Test
    public void testURL() throws Exception {
        try {
            deployer.deploy("URL");
            performTest("URL", new URL("http://nowhere.com"));
        } finally {

            try {
                deployer.undeploy("URL");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void performTest(String attributeName, Object expectedValue) throws Exception {
        this.performTest(attributeName, expectedValue, null);
    }

    private void performTest(String attributeName, Object expectedValue, Comparator comparator) throws Exception {
        ObjectName oname = new ObjectName("test:service=WithProperties" + attributeName);
        Object attributeValue = connection.getAttribute(oname, attributeName);
        Assert.assertNotNull("Found null attribute value for '" + attributeName + "'", attributeValue);
        if (comparator == null) {
            Assert.assertEquals("Found wrong attribute value for '" + attributeName + "'", expectedValue, attributeValue);
        } else {
            boolean equal = comparator.compare(expectedValue, attributeValue) == 0;
            Assert.assertTrue("Found wrong attribute value for '" + attributeName + "', value: '"+attributeValue+"' expected: '"+expectedValue+"'", equal);
        }

    }
}
