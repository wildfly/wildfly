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
package org.jboss.as.test.integration.ejb.entity.cmp.simple;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;

import javax.ejb.DuplicateKeyException;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.ejb.entity.cmp.AbstractCmpTest;
import org.jboss.as.test.integration.ejb.entity.cmp.CmpTestRunner;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic cmp2 tests
 *
 * @author alex@jboss.org
 * @author Scott.Stark@jboss.org
 * @version $Revision: 81036 $
 */
@RunWith(CmpTestRunner.class)
public class SimpleCMPUnitTestCase extends AbstractCmpTest {
    private static Logger log = Logger.getLogger(SimpleCMPUnitTestCase.class);

    private static final String ARCHIVE_NAME = "cmp2-simple.jar";

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME);
        jar.addPackage(SimpleCMPUnitTestCase.class.getPackage());
        jar.addAsManifestResource("ejb/entity/cmp/simple/ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource("ejb/entity/cmp/simple/jbosscmp-jdbc.xml", "jbosscmp-jdbc.xml");
        AbstractCmpTest.addDeploymentAssets(jar);
        return jar;
    }

    public SimpleCMPUnitTestCase() {
        Calendar c = Calendar.getInstance();
        c.clear();    // Must clear time components
        c.set(1981, 4, 5);
        sqlDateValue = new java.sql.Date(c.getTime().getTime());

        c = Calendar.getInstance();
        c.clear();    // Must set date components to epoch
        c.set(Calendar.HOUR_OF_DAY, 22);
        c.set(Calendar.MINUTE, 33);
        c.set(Calendar.SECOND, 44);
        // java.sql.Time does not have a millisecond component
        timeValue = new java.sql.Time(c.getTime().getTime());

        objectValue = new HashMap();
        ((HashMap) objectValue).put("boolean", booleanObject);
        ((HashMap) objectValue).put("byte", byteObject);
        ((HashMap) objectValue).put("short", shortObject);
        ((HashMap) objectValue).put("int", integerObject);
        ((HashMap) objectValue).put("long", longObject);
        ((HashMap) objectValue).put("float", floatObject);
        ((HashMap) objectValue).put("double", doubleObject);
        ((HashMap) objectValue).put("string", stringValue);
        ((HashMap) objectValue).put("utilDate", utilDateValue);
        ((HashMap) objectValue).put("sqlDate", sqlDateValue);
        ((HashMap) objectValue).put("time", timeValue);
        ((HashMap) objectValue).put("timestamp", timestampValue);
        ((HashMap) objectValue).put("bigDecimal", bigDecimalValue);
    }

    private SimpleHome getSimpleHome() {
        try {
            return (SimpleHome) iniCtx.lookup("java:module/SimpleEJB!org.jboss.as.test.integration.ejb.entity.cmp.simple.SimpleHome");
        } catch (Exception e) {
            log.error("failed", e);
            fail("Exception in getSimpleHome: " + e.getMessage());
        }
        return null;
    }

    private Simple simple;
    private final boolean booleanPrimitive = true;
    private final Boolean booleanObject = Boolean.FALSE;
    private final byte bytePrimitive = (byte) 11;
    private final Byte byteObject = new Byte((byte) 22);
    private final short shortPrimitive = (short) 33;
    private final Short shortObject = new Short((short) 44);
    private final int integerPrimitive = 55;
    private final Integer integerObject = new Integer(66);
    private final long longPrimitive = 77;
    private final Long longObject = new Long(88);
    private final float floatPrimitive = 11.11f;
    private final Float floatObject = new Float(22.22f);
    private final double doublePrimitive = 33.33;
    private final Double doubleObject = new Double(44.44);
    private final String stringValue = "test string value";
    private final java.util.Date utilDateValue = new java.util.Date(1111);
    private final java.sql.Date sqlDateValue;
    private final Time timeValue;
    private final Timestamp timestampValue = new Timestamp(4444);
    private final BigDecimal bigDecimalValue = new BigDecimal("12345678.1234");
    private final byte[] byteArrayValue = "byte array test".getBytes();
    private final Object objectValue;
    private final ValueClass valueClass = new ValueClass(111, 999);
    private final Hashtable hashtable = new Hashtable();

    @Test
    public void testBooleanPrimitive() throws Exception {
        assertEquals(booleanPrimitive, simple.getBooleanPrimitive());
    }

    @Test
    public void testBooleanObject() throws Exception {
        assertEquals(booleanObject, simple.getBooleanObject());
    }

    @Test
    public void testBytePrimitive() throws Exception {
        assertEquals(bytePrimitive, simple.getBytePrimitive());
    }

    @Test
    public void testByteObject() throws Exception {
        assertEquals(byteObject, simple.getByteObject());
    }

    @Test
    public void testShortPrimitive() throws Exception {
        assertEquals(shortPrimitive, simple.getShortPrimitive());
    }

    @Test
    public void testShortObject() throws Exception {
        assertEquals(shortObject, simple.getShortObject());
    }

    @Test
    public void testIntegerPrimitive() throws Exception {
        assertEquals(integerPrimitive, simple.getIntegerPrimitive());
    }

    @Test
    public void testIntegerObject() throws Exception {
        assertEquals(integerObject, simple.getIntegerObject());
    }

    @Test
    public void testLongPrimitive() throws Exception {
        assertEquals(longPrimitive, simple.getLongPrimitive());
    }

    @Test
    public void testLongObject() throws Exception {
        assertEquals(longObject, simple.getLongObject());
    }

    @Test
    public void testFloatPrimitive() throws Exception {
        assertEquals(floatPrimitive, simple.getFloatPrimitive(), 0);
    }

    @Test
    public void testFloatObject() throws Exception {
        assertEquals(floatObject, simple.getFloatObject());
    }

    @Test
    public void testDoublePrimitive() throws Exception {
        assertEquals(doublePrimitive, simple.getDoublePrimitive(), 0);
    }

    @Test
    public void testDoubleObject() throws Exception {
        assertEquals(doubleObject, simple.getDoubleObject());
    }

    @Test
    public void testStringValue() throws Exception {
        assertEquals(stringValue, simple.getStringValue());
    }

    @Test
    public void testUtilDateValue() throws Exception {
        assertTrue(
                "expected :<" + simple.getUtilDateValue() + "> but was <" +
                        utilDateValue + ">",
                utilDateValue.compareTo(simple.getUtilDateValue()) == 0);
    }

    @Test
    public void testSqlDateValue() throws Exception {
        assertTrue(
                "expected :<" + simple.getSqlDateValue() + "> but was <" +
                        sqlDateValue + ">",
                sqlDateValue.compareTo(simple.getSqlDateValue()) == 0);
    }

    @Test
    public void testTimeValue() throws Exception {
        assertTrue(
                "expected :<" + simple.getTimeValue() + "> but was <" +
                        timeValue + ">",
                timeValue.compareTo(simple.getTimeValue()) == 0);
    }

    @Test
    public void testTimestampValue() throws Exception {
        assertTrue(
                "expected :<" + simple.getTimestampValue() + "> but was <" +
                        timestampValue + ">",
                timestampValue.compareTo(simple.getTimestampValue()) == 0);
    }

    @Test
    public void testBigDecimalValue() throws Exception {
        assertTrue(
                "expected :<" + simple.getBigDecimalValue() + "> but was <" +
                        bigDecimalValue + ">",
                bigDecimalValue.compareTo(simple.getBigDecimalValue()) == 0);
    }

    @Test
    public void testByteArrayValue() throws Exception {
        byte[] array = simple.getByteArrayValue();
        assertEquals(byteArrayValue.length, array.length);
        for (int i = 0; i < array.length; i++) {
            assertEquals(byteArrayValue[i], array[i]);
        }
    }

    @Test
    public void testValueClass() throws Exception {
        ValueClass vc = simple.getValueClass();
        log.info("getValueClass class: " + vc.getClass().getName());
        log.info("getValueClass classloader: " + vc.getClass().getClassLoader());
        log.info("ValueClass class: " + valueClass.getClass().getName());
        log.info("ValueClass classloader: " + valueClass.getClass().getClassLoader());
        assertEquals(valueClass, vc);
    }

    @Test
    public void testObjectValue() throws Exception {
        Object v = simple.getObjectValue();
        log.info("getObjectValue class: " + v.getClass().getName());
        log.info("getObjectValue classloader: " + v.getClass().getClassLoader());
        log.info("objectValue class: " + objectValue.getClass().getName());
        log.info("objectValue classloader: " + objectValue.getClass().getClassLoader());
        assertEquals(objectValue, v);
    }


    @Test
    public void testLiteralToLiteral() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE TRUE=TRUE",
                new Object[0]);
        assertTrue(c.size() == 1);

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE 1.2=1.2",
                new Object[0]);
        assertTrue(c.size() == 1);

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE 'funk'='funk'",
                new Object[0]);
        assertTrue(c.size() == 1);
    }

    @Test
    public void testUtilDateBetween() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        java.util.Date utilDateBefore = new java.util.Date(100);
        java.util.Date utilDateAfter = new java.util.Date(2000);
        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE s.utilDateValue BETWEEN ?1 AND ?2",
                new Object[]{utilDateBefore, utilDateAfter});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testSQLDateBetween() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;
        Calendar calendar;

        calendar = Calendar.getInstance();
        calendar.clear();    // Must clear time components
        calendar.set(1981, 4, 3);
        java.sql.Date sqlDateBefore =
                new java.sql.Date(calendar.getTime().getTime());

        calendar = Calendar.getInstance();
        calendar.clear();    // Must clear time components
        calendar.set(1981, 4, 6);
        java.sql.Date sqlDateAfter =
                new java.sql.Date(calendar.getTime().getTime());

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE s.sqlDateValue BETWEEN ?1 AND ?2",
                new Object[]{sqlDateBefore, sqlDateAfter});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testTimeBetween() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;
        Calendar calendar;

        calendar = Calendar.getInstance();
        calendar.clear();    // Must set date components to epoch
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 33);
        calendar.set(Calendar.SECOND, 44);
        // java.sql.Time does not have a millisecond component
        Time timeBefore = new java.sql.Time(calendar.getTime().getTime());

        calendar = Calendar.getInstance();
        calendar.clear();    // Must set date components to epoch
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 33);
        calendar.set(Calendar.SECOND, 44);
        // java.sql.Time does not have a millisecond component
        Time timeAfter = new java.sql.Time(calendar.getTime().getTime());

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE s.timeValue BETWEEN ?1 AND ?2",
                new Object[]{timeBefore, timeAfter});
        assertTrue(c.size() == 1);

    }

    @Test
    public void testTimestampBetween() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        Timestamp timestampBefore = new Timestamp(1111);
        Timestamp timestampAfter = new Timestamp(8888);
        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE s.timestampValue BETWEEN ?1 AND ?2",
                new Object[]{timestampBefore, timestampAfter});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testTimestampComparison() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        Timestamp timestampBefore = new Timestamp(1111);
        Timestamp timestampAfter = new Timestamp(8888);
        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE s.timestampValue >= ?1 AND s.timestampValue <= ?2",
                new Object[]{timestampBefore, timestampAfter});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testTimestampIn() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        Timestamp timestampBefore = new Timestamp(1111);
        Timestamp timestampAfter = new Timestamp(8888);
        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE s.timestampValue IN(?1, ?2, ?3)",
                new Object[]{timestampBefore, timestampAfter, timestampValue});
        assertTrue(c.size() == 1);
    }


    @Test
    public void testStringBetween() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE UCASE(s.stringValue) BETWEEN UCASE(?1) AND UCASE(?2)",
                new Object[]{"aaaaa", "zzzzz"});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testStringComparison() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE UCASE(s.stringValue) >= UCASE(?1) AND " +
                        "  UCASE(s.stringValue) <= UCASE(?2)",
                new Object[]{"aaaaa", "zzzzz"});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testStringIn() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE UCASE(s.stringValue) IN(UCASE(?1), UCASE(?2), " +
                        "  UCASE('" + stringValue + "'))",
                new Object[]{"aaaaa", "zzzzz"});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testLike() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE UCASE(s.stringValue) LIKE UCASE(?1)",
                new Object[]{"% string %"});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testNumericIn() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE SQRT(s.longPrimitive) " +
                        "  IN(SQRT(?1), SQRT(?2), SQRT( " + longPrimitive + " ) )",
                new Object[]{new Long(23094), new Long(20984)});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testNumbericComparison() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE SQRT(s.longPrimitive) >= SQRT(?1) AND " +
                        "  SQRT(s.longPrimitive) <= SQRT(?2)",
                new Object[]{new Long(22), new Long(20984)});
        assertTrue(c.size() == 1);
    }

    @Test
    public void testConcatFunction() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE CONCAT(?1, ?2) = ?3",
                new Object[]{
                        "foo",
                        "bar",
                        "foobar"}
        );
        assertTrue(c.size() == 1);
    }

    @Test
    public void testLengthFunction() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE LENGTH(?1) = ?2",
                new Object[]{
                        "12345",
                        new Integer(5)}
        );
        assertTrue(c.size() == 1);
    }

    @Test
    public void testSelectValueClass() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c = simpleHome.selectValueClass();
        assertEquals(1, c.size());

        ValueClass v = (ValueClass) c.iterator().next();
        assertEquals(valueClass, v);
    }

    @Test
    public void testLocateFunction() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE LOCATE(?1, ?2, ?3) = ?4",
                new Object[]{
                        "x",
                        "1x34x67x90",
                        new Integer(3),
                        new Integer(5)}
        );
        assertTrue(c.size() == 1);

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE LOCATE(?1, ?2) = ?3",
                new Object[]{
                        "x",
                        "1x34x67x90",
                        new Integer(2)}
        );
        assertTrue(c.size() == 1);

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE LOCATE(?1, ?2, ?3) = ?4",
                new Object[]{
                        "z",
                        "1x34x67x90",
                        new Integer(3),
                        new Integer(0)}
        );
        assertTrue(c.size() == 1);
    }

    public void testSubstringFunction() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c;

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE SUBSTRING(?1, ?2, ?3) = ?4",
                new Object[]{
                        "1234587890",
                        new Integer(5),
                        new Integer(3),
                        "567"}
        );
        assertTrue(c.size() == 1);

        c = simpleHome.selectDynamic(
                "SELECT OBJECT(s) " +
                        "FROM simple s " +
                        "WHERE SUBSTRING(?1, ?2) = ?3",
                new Object[]{
                        "1234587890",
                        new Integer(5),
                        "567890"}
        );
        assertTrue(c.size() == 1);
    }

    @Test
    public void testFindWithByteArray() throws Exception {
        SimpleHome simpleHome = getSimpleHome();
        Collection c = simpleHome.findWithByteArray(byteArrayValue);
        assertEquals(1, c.size());

        Simple s = (Simple) c.iterator().next();
        assertTrue(s.isIdentical(simple));

        assertEquals(booleanPrimitive, s.getBooleanPrimitive());
        assertEquals(booleanObject, s.getBooleanObject());
        assertEquals(bytePrimitive, s.getBytePrimitive());
        assertEquals(byteObject, s.getByteObject());
        assertEquals(shortPrimitive, s.getShortPrimitive());
        assertEquals(shortObject, s.getShortObject());
        assertEquals(integerPrimitive, s.getIntegerPrimitive());
        assertEquals(integerObject, s.getIntegerObject());
        assertEquals(longPrimitive, s.getLongPrimitive());
        assertEquals(longObject, s.getLongObject());
        assertEquals(floatPrimitive, s.getFloatPrimitive(), 0);
        assertEquals(floatObject, s.getFloatObject());
        assertEquals(doublePrimitive, s.getDoublePrimitive(), 0);
        assertEquals(doubleObject, s.getDoubleObject());
        assertEquals(stringValue, s.getStringValue());
        assertTrue(
                "expected :<" + simple.getUtilDateValue() + "> but was <" +
                        utilDateValue + ">",
                utilDateValue.compareTo(simple.getUtilDateValue()) == 0);
        assertTrue(
                "expected :<" + simple.getSqlDateValue() + "> but was <" +
                        sqlDateValue + ">",
                sqlDateValue.compareTo(simple.getSqlDateValue()) == 0);
        assertTrue(
                "expected :<" + simple.getTimeValue() + "> but was <" +
                        timeValue + ">",
                timeValue.compareTo(simple.getTimeValue()) == 0);
        assertTrue(
                "expected :<" + simple.getTimestampValue() + "> but was <" +
                        timestampValue + ">",
                timestampValue.compareTo(simple.getTimestampValue()) == 0);
        assertTrue(
                "expected :<" + simple.getBigDecimalValue() + "> but was <" +
                        bigDecimalValue + ">",
                bigDecimalValue.compareTo(simple.getBigDecimalValue()) == 0);

        byte[] array = simple.getByteArrayValue();
        assertEquals(byteArrayValue.length, array.length);
        for (int i = 0; i < array.length; i++) {
            assertEquals(byteArrayValue[i], array[i]);
        }

        assertEquals(valueClass, simple.getValueClass());
        assertEquals(objectValue, simple.getObjectValue());
    }

    @Ignore
    public void testDuplicateKey() throws Exception {
        try {
            SimpleHome simpleHome = getSimpleHome();
            simpleHome.create("simple");
            fail("Did not get DuplicateKeyException");
        } catch (DuplicateKeyException e) {
            // OK
        }
    }

    @Test
    public void testHashtable() throws Exception {
        simple.addToHashtable("key1", "value1");
        simple.addToHashtable("key2", "value2");
        Hashtable result = simple.getHashtable();
        assertEquals(2, result.size());
    }

    @Test
    public void testOptionAUpdate() throws Exception {
        InitialContext ctx = new InitialContext();
        SimpleHome home = (SimpleHome) ctx.lookup("java:module/SimpleEJBWithOptionA!org.jboss.as.test.integration.ejb.entity.cmp.simple.SimpleHome");
        Simple simpleA = null;
        try {
            simpleA = home.findByPrimaryKey("simpleA");
        } catch (Exception e) {
            simpleA = home.create("simpleA");
        }

        simpleA.setIntegerPrimitive(47);
        int i = simpleA.getIntegerPrimitive();
        assertTrue("i == 47 ", i == 47);
    }

    public void setUpEjb() throws Exception {
        SimpleHome simpleHome = getSimpleHome();

        boolean wasCreated = false;
        try {
            simple = simpleHome.findByPrimaryKey("simple");
        } catch (Exception e) {
        }

        if (simple == null) {
            simple = simpleHome.create("simple");
            wasCreated = true;
        }

        simple.setBooleanPrimitive(booleanPrimitive);
        simple.setBooleanObject(booleanObject);
        simple.setBytePrimitive(bytePrimitive);
        simple.setByteObject(byteObject);
        simple.setShortPrimitive(shortPrimitive);
        simple.setShortObject(shortObject);
        simple.setIntegerPrimitive(integerPrimitive);
        simple.setIntegerObject(integerObject);
        simple.setLongPrimitive(longPrimitive);
        simple.setLongObject(longObject);
        simple.setFloatPrimitive(floatPrimitive);
        simple.setFloatObject(floatObject);
        simple.setDoublePrimitive(doublePrimitive);
        simple.setDoubleObject(doubleObject);
        simple.setStringValue(stringValue);
        simple.setUtilDateValue(utilDateValue);
        simple.setSqlDateValue(sqlDateValue);
        simple.setTimeValue(timeValue);
        simple.setTimestampValue(timestampValue);
        if (wasCreated) {
            simple.setBigDecimalValue(bigDecimalValue);
            simple.setByteArrayValue(byteArrayValue);
            simple.setObjectValue(objectValue);
            simple.setValueClass(valueClass);
            simple.setHashtable(hashtable);
        }
    }

    public void tearDownEjb() throws Exception {
        simple.remove();
    }
}
