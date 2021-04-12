/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.marshalling;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

/**
 * Generic tests for java.util.* classes.
 * @author Paul Ferraro
 */
public abstract class AbstractUtilTestCase {
    private static final Map<Object, Object> BASIS = Stream.of(1, 2, 3, 4, 5).collect(Collectors.toMap(i -> i, i -> Integer.toString(-i)));

    private final MarshallingTesterFactory factory;

    public AbstractUtilTestCase(MarshallingTesterFactory factory) {
        this.factory = factory;
    }

    @Test
    public void testArrayDeque() throws IOException {
        MarshallingTester<ArrayDeque<Object>> tester = this.factory.createTester();
        tester.test(new ArrayDeque<>(BASIS.keySet()), AbstractUtilTestCase::assertCollectionEquals);
    }

    @Test
    public void testArrayList() throws IOException {
        MarshallingTester<ArrayList<Object>> tester = this.factory.createTester();
        tester.test(new ArrayList<>(BASIS.keySet()), AbstractUtilTestCase::assertCollectionEquals);
    }

    @Test
    public void testBitSet() throws IOException {
        MarshallingTester<BitSet> tester = this.factory.createTester();

        tester.test(new BitSet(0));
        BitSet set = new BitSet(7);
        set.set(1);
        set.set(3);
        set.set(5);
        tester.test(set);
    }

    @Test
    public void testCalendar() throws IOException {
        MarshallingTester<Calendar> tester = this.factory.createTester();
        LocalDateTime time = LocalDateTime.now();
        // Validate default calendar w/date only
        tester.test(new Calendar.Builder().setDate(time.getYear(), time.getMonthValue(), time.getDayOfMonth()).build());
        // Validate Gregorian calendar w/locale and date + time
        tester.test(new Calendar.Builder().setLenient(false).setLocale(Locale.FRANCE).setDate(time.getYear(), time.getMonthValue() -1, time.getDayOfMonth()).setTimeOfDay(time.getHour(), time.getMinute(), time.getSecond()).build());
        // Validate Japanese Imperial calendar w/full date/time
        tester.test(new Calendar.Builder().setLocale(Locale.JAPAN).setTimeZone(TimeZone.getTimeZone("Asia/Tokyo")).setInstant(Date.from(time.toInstant(ZoneOffset.UTC))).build());
        // Validate Buddhist calendar
        tester.test(new Calendar.Builder().setLocale(Locale.forLanguageTag("th_TH")).setTimeZone(TimeZone.getTimeZone("Asia/Bangkok")).build());
    }

    @Test
    public void testCurrency() throws IOException {
        MarshallingTester<Currency> tester = this.factory.createTester();
        tester.test(Currency.getInstance(Locale.getDefault()));
        tester.test(Currency.getInstance(Locale.UK));
    }

    @Test
    public void testDate() throws IOException {
        MarshallingTester<Date> tester = this.factory.createTester();
        tester.test(Date.from(Instant.EPOCH));
        tester.test(Date.from(Instant.now()));
    }

    @Test
    public void testEnumMap() throws IOException {
        MarshallingTester<EnumMap<Thread.State, String>> tester = this.factory.createTester();
        EnumMap<Thread.State, String> map = new EnumMap<>(Thread.State.class);
        tester.test(map, AbstractUtilTestCase::assertMapEquals);
        for (Thread.State state : EnumSet.allOf(Thread.State.class)) {
            map.put(state, ((state.ordinal() % 2) == 0) ? state.name() : null);
            tester.test(map, AbstractUtilTestCase::assertMapEquals);
        }
    }

    @Test
    public void testEnumSet() throws IOException {
        MarshallingTester<EnumSet<Thread.State>> tester = this.factory.createTester();
        EnumSet<Thread.State> set = EnumSet.noneOf(Thread.State.class);
        tester.test(set, AbstractUtilTestCase::assertCollectionEquals);
        for (Thread.State state : EnumSet.allOf(Thread.State.class)) {
            set.add(state);
            tester.test(set, AbstractUtilTestCase::assertCollectionEquals);
        }
    }

    @Test
    public void testJumboEnumSet() throws IOException {
        MarshallingTester<EnumSet<Character.UnicodeScript>> tester = this.factory.createTester();
        tester.test(EnumSet.noneOf(Character.UnicodeScript.class), AbstractUtilTestCase::assertCollectionEquals);
        tester.test(EnumSet.of(Character.UnicodeScript.UNKNOWN), AbstractUtilTestCase::assertCollectionEquals);
        tester.test(EnumSet.of(Character.UnicodeScript.ARABIC, Character.UnicodeScript.ARMENIAN, Character.UnicodeScript.AVESTAN, Character.UnicodeScript.BALINESE, Character.UnicodeScript.BAMUM, Character.UnicodeScript.BATAK, Character.UnicodeScript.BENGALI, Character.UnicodeScript.BOPOMOFO, Character.UnicodeScript.BRAHMI, Character.UnicodeScript.BRAILLE, Character.UnicodeScript.BUGINESE, Character.UnicodeScript.BUHID, Character.UnicodeScript.CANADIAN_ABORIGINAL, Character.UnicodeScript.CARIAN), AbstractUtilTestCase::assertCollectionEquals);
        tester.test(EnumSet.complementOf(EnumSet.of(Character.UnicodeScript.UNKNOWN)), AbstractUtilTestCase::assertCollectionEquals);
        tester.test(EnumSet.allOf(Character.UnicodeScript.class), AbstractUtilTestCase::assertCollectionEquals);
    }

    @Test
    public void testHashMap() throws IOException {
        MarshallingTester<HashMap<Object, Object>> tester = this.factory.createTester();
        tester.test(new HashMap<>(BASIS), AbstractUtilTestCase::assertMapEquals);
    }

    @Test
    public void testHashSet() throws IOException {
        MarshallingTester<HashSet<Object>> tester = this.factory.createTester();
        tester.test(new HashSet<>(BASIS.keySet()), AbstractUtilTestCase::assertCollectionEquals);
    }

    @Test
    public void testLinkedHashMap() throws IOException {
        MarshallingTester<LinkedHashMap<Object, Object>> tester = this.factory.createTester();
        tester.test(new LinkedHashMap<>(BASIS), AbstractUtilTestCase::assertLinkedMapEquals);
        LinkedHashMap<Object, Object> accessOrderMap = new LinkedHashMap<>(5, 1, true);
        accessOrderMap.putAll(BASIS);
        tester.test(new LinkedHashMap<>(accessOrderMap), AbstractUtilTestCase::assertLinkedMapEquals);
    }

    @Test
    public void testLinkedHashSet() throws IOException {
        MarshallingTester<LinkedHashSet<Object>> tester = this.factory.createTester();
        tester.test(new LinkedHashSet<>(BASIS.keySet()), AbstractUtilTestCase::assertCollectionEquals);
    }

    @Test
    public void testLinkedList() throws IOException {
        MarshallingTester<LinkedList<Object>> tester = this.factory.createTester();
        tester.test(new LinkedList<>(BASIS.keySet()), AbstractUtilTestCase::assertCollectionEquals);
    }

    @Test
    public void testLocale() throws IOException {
        MarshallingTester<Locale> tester = this.factory.createTester();
        tester.test(Locale.getDefault());
        tester.test(Locale.ENGLISH);
        tester.test(Locale.CANADA_FRENCH);
    }

    @Test
    public void testOptional() throws IOException {
        MarshallingTester<Optional<Object>> tester = this.factory.createTester();
        tester.test(Optional.empty());
        tester.test(Optional.of("foo"));
    }

    @Test
    public void testOptionalDouble() throws IOException {
        MarshallingTester<OptionalDouble> tester = this.factory.createTester();
        tester.test(OptionalDouble.empty());
        tester.test(OptionalDouble.of(Double.MAX_VALUE));
    }

    @Test
    public void testOptionalInt() throws IOException {
        MarshallingTester<OptionalInt> tester = this.factory.createTester();
        tester.test(OptionalInt.empty());
        tester.test(OptionalInt.of(Integer.MAX_VALUE));
    }

    @Test
    public void testOptionalLong() throws IOException {
        MarshallingTester<OptionalLong> tester = this.factory.createTester();
        tester.test(OptionalLong.empty());
        tester.test(OptionalLong.of(Long.MAX_VALUE));
    }

    @Test
    public void testSimpleEntry() throws IOException {
        MarshallingTester<AbstractMap.SimpleEntry<Object, Object>> tester = this.factory.createTester();
        String key = "key";
        String value = "value";
        tester.test(new AbstractMap.SimpleEntry<>(null, null));
        tester.test(new AbstractMap.SimpleEntry<>(key, null));
        tester.test(new AbstractMap.SimpleEntry<>(key, value));
        tester.test(new AbstractMap.SimpleEntry<>(value, value));
    }

    @Test
    public void testSimpleImmutableEntry() throws IOException {
        MarshallingTester<AbstractMap.SimpleImmutableEntry<Object, Object>> tester = this.factory.createTester();
        String key = "key";
        String value = "value";
        tester.test(new AbstractMap.SimpleImmutableEntry<>(null, null));
        tester.test(new AbstractMap.SimpleImmutableEntry<>(key, null));
        tester.test(new AbstractMap.SimpleImmutableEntry<>(key, value));
        tester.test(new AbstractMap.SimpleImmutableEntry<>(value, value));
    }

    @Test
    public void testTimeZone() throws IOException {
        MarshallingTester<TimeZone> tester = this.factory.createTester();
        tester.test(TimeZone.getDefault());
        tester.test(TimeZone.getTimeZone("GMT"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTreeMap() throws IOException {
        MarshallingTester<TreeMap<Object, Object>> tester = this.factory.createTester();

        TreeMap<Object, Object> map = new TreeMap<>();
        map.putAll(BASIS);
        tester.test(map, AbstractUtilTestCase::assertMapEquals);

        map = new TreeMap<>((Comparator<Object>) (Comparator<?>) Comparator.reverseOrder());
        map.putAll(BASIS);
        tester.test(map, AbstractUtilTestCase::assertMapEquals);

        map = new TreeMap<>(new TestComparator<>());
        map.putAll(BASIS);
        tester.test(map, AbstractUtilTestCase::assertMapEquals);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTreeSet() throws IOException {
        MarshallingTester<TreeSet<Object>> tester = this.factory.createTester();

        TreeSet<Object> set = new TreeSet<>();
        set.addAll(BASIS.keySet());
        tester.test(set, AbstractUtilTestCase::assertCollectionEquals);

        set = new TreeSet<>((Comparator<Object>) (Comparator<?>) Comparator.reverseOrder());
        set.addAll(BASIS.keySet());
        tester.test(set, AbstractUtilTestCase::assertCollectionEquals);

        set = new TreeSet<>(new TestComparator<>());
        set.addAll(BASIS.keySet());
        tester.test(set, AbstractUtilTestCase::assertCollectionEquals);
    }

    @Test
    public void testUUID() throws IOException {
        MarshallingTester<UUID> tester = this.factory.createTester();
        tester.test(UUID.randomUUID());
    }

    // java.util.Collections.emptyXXX() methods
    @Test
    public void testEmptyEnumeration() throws IOException {
        MarshallingTester<Enumeration<Object>> tester = this.factory.createTester();
        tester.test(Collections.emptyEnumeration(), Assert::assertSame);
    }

    @Test
    public void testEmptyIterator() throws IOException {
        MarshallingTester<Iterator<Object>> tester = this.factory.createTester();
        tester.test(Collections.emptyIterator(), Assert::assertSame);
    }

    @Test
    public void testEmptyList() throws IOException {
        MarshallingTester<List<Object>> tester = this.factory.createTester();
        tester.test(Collections.emptyList(), Assert::assertSame);
    }

    @Test
    public void testEmptyListIterator() throws IOException {
        MarshallingTester<ListIterator<Object>> tester = this.factory.createTester();
        tester.test(Collections.emptyListIterator(), Assert::assertSame);
    }

    @Test
    public void testEmptyMap() throws IOException {
        MarshallingTester<Map<Object, Object>> tester = this.factory.createTester();
        tester.test(Collections.emptyMap(), Assert::assertSame);
    }

    @Test
    public void testEmptyNavigableMap() throws IOException {
        MarshallingTester<NavigableMap<Object, Object>> tester = this.factory.createTester();
        tester.test(Collections.emptyNavigableMap(), Assert::assertSame);
    }

    @Test
    public void testEmptyNavigableSet() throws IOException {
        MarshallingTester<NavigableSet<Object>> tester = this.factory.createTester();
        tester.test(Collections.emptyNavigableSet(), Assert::assertSame);
    }

    @Test
    public void testEmptySet() throws IOException {
        MarshallingTester<Set<Object>> tester = this.factory.createTester();
        tester.test(Collections.emptySet(), Assert::assertSame);
    }

    @Test
    public void testEmptySortedMap() throws IOException {
        MarshallingTester<SortedMap<Object, Object>> tester = this.factory.createTester();
        tester.test(Collections.emptySortedMap(), Assert::assertSame);
    }

    @Test
    public void testEmptySortedSet() throws IOException {
        MarshallingTester<SortedSet<Object>> tester = this.factory.createTester();
        tester.test(Collections.emptySortedSet(), Assert::assertSame);
    }

    // java.util.Collections.singletonXXX(...) methods
    @Test
    public void testSingletonList() throws IOException {
        MarshallingTester<List<Object>> tester = this.factory.createTester();
        tester.test(Collections.singletonList(null), AbstractUtilTestCase::assertCollectionEquals);
        tester.test(Collections.singletonList("foo"), AbstractUtilTestCase::assertCollectionEquals);
    }

    @Test
    public void testSingletonMap() throws IOException {
        MarshallingTester<Map<Object, Object>> tester = this.factory.createTester();
        tester.test(Collections.singletonMap(null, null), AbstractUtilTestCase::assertMapEquals);
        tester.test(Collections.singletonMap("foo", null), AbstractUtilTestCase::assertMapEquals);
        tester.test(Collections.singletonMap("foo", "bar"), AbstractUtilTestCase::assertMapEquals);
    }

    @Test
    public void testSingletonSet() throws IOException {
        MarshallingTester<Set<Object>> tester = this.factory.createTester();
        tester.test(Collections.singleton(null), AbstractUtilTestCase::assertCollectionEquals);
        tester.test(Collections.singleton("foo"), AbstractUtilTestCase::assertCollectionEquals);
    }

    static <T extends Collection<?>> void assertCollectionEquals(T expected, T actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertTrue(expected.containsAll(actual));
    }

    static <T extends Map<?, ?>> void assertMapEquals(T expected, T actual) {
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertTrue(actual.keySet().toString(), expected.keySet().containsAll(actual.keySet()));
        for (Map.Entry<?, ?> entry : expected.entrySet()) {
            Assert.assertEquals(entry.getValue(), actual.get(entry.getKey()));
        }
    }

    static <T extends Map<?, ?>> void assertLinkedMapEquals(T expected, T actual) {
        Assert.assertEquals(expected.size(), actual.size());
        // Change access order
        expected.get(expected.keySet().iterator().next());
        actual.get(actual.keySet().iterator().next());
        @SuppressWarnings("unchecked")
        Iterator<Map.Entry<?, ?>> expectedEntries = (Iterator<Map.Entry<?, ?>>) (Iterator<?>) expected.entrySet().iterator();
        @SuppressWarnings("unchecked")
        Iterator<Map.Entry<?, ?>> actualEntries = (Iterator<Map.Entry<?, ?>>) (Iterator<?>) actual.entrySet().iterator();
        while (expectedEntries.hasNext() && actualEntries.hasNext()) {
            Map.Entry<?, ?> expectedEntry = expectedEntries.next();
            Map.Entry<?, ?> actualEntry = actualEntries.next();
            Assert.assertEquals(expectedEntry.getKey(), actualEntry.getKey());
            Assert.assertEquals(expectedEntry.getValue(), actualEntry.getValue());
        }
    }
}
