/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.base.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.apache.ofbiz.base.util.collections.MapComparator;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.ModelService;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * UtilMisc - Misc Utility Functions
 */
public final class UtilMisc {

    private static final String MODULE = UtilMisc.class.getName();

    private static final BigDecimal ZERO_BD = BigDecimal.ZERO;

    private UtilMisc() { }

    public static <T extends Throwable> T initCause(final T throwable, final Throwable cause) {
        throwable.initCause(cause);
        return throwable;
    }

    public static <T> int compare(final Comparable<T> obj1, final T obj2) {
        if (obj1 == null) {
            if (obj2 == null) {
                return 0;
            }
            return 1;
        }
        return obj1.compareTo(obj2);
    }

    public static <E> int compare(final List<E> obj1, final List<E> obj2) {
        if (obj1 == obj2) {
            return 0;
        }
        try {
            if (obj1.size() == obj2.size() && obj1.containsAll(obj2) && obj2.containsAll(obj1)) {
                return 0;
            }

        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            Debug.log(e, MODULE);
        }
        return 1;
    }

    /**
     * Get an iterator from a collection, returning null if collection is null
     * @param col The collection to be turned in to an iterator
     * @return The resulting Iterator
     */
    public static <T> Iterator<T> toIterator(final Collection<T> col) {
        if (col == null) {
            return null;
        }
        return col.iterator();
    }

    /**
     * Creates a pseudo-literal map corresponding to key-values.
     * @param kvs the key-value pairs
     * @return the corresponding map.
     * @throws IllegalArgumentException when the key-value list is not even.
     */
    public static <K, V> Map<K, V> toMap(final Object... kvs) {
        return toMap(HashMap::new, kvs);
    }

    /**
     * Creates a pseudo-literal map corresponding to key-values.
     * @param constructor the constructor used to instantiate the map
     * @param kvs         the key-value pairs
     * @return the corresponding map.
     * @throws IllegalArgumentException when the key-value list is not even.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toMap(final Supplier<Map<K, V>> constructor, final Object... kvs) {
        if (kvs.length == 1 && kvs[0] instanceof Map) {
            return UtilGenerics.cast(kvs[0]);
        }
        if (kvs.length % 2 == 1) {
            final IllegalArgumentException e = new IllegalArgumentException(
                    "You must pass an even sized array to the toMap method (size = " + kvs.length + ")");
            Debug.logInfo(e, MODULE);
            throw e;
        }
        final Map<K, V> map = constructor.get();
        for (int i = 0; i < kvs.length;) {
            map.put((K) kvs[i++], (V) kvs[i++]);
        }
        return map;
    }

    public static <K, V> String printMap(final Map<? extends K, ? extends V> theMap) {
        final StringBuilder theBuf = new StringBuilder();
        for (final Map.Entry<? extends K, ? extends V> entry : theMap.entrySet()) {
            theBuf.append(entry.getKey());
            theBuf.append(" --> ");
            theBuf.append(entry.getValue());
            theBuf.append(System.getProperty("line.separator"));
        }
        return theBuf.toString();
    }

    public static <T> List<T> makeListWritable(final Collection<? extends T> col) {
        final List<T> result = new LinkedList<>();
        if (col != null) {
            result.addAll(col);
        }
        return result;
    }

    public static <K, V> Map<K, V> makeMapWritable(final Map<K, ? extends V> map) {
        if (map == null) {
            return new HashMap<>();
        }
        final Map<K, V> result = new HashMap<>(map.size());
        result.putAll(map);
        return result;
    }

    /**
     * This change a Map to be Serializable by removing all entries with values that are not Serializable.
     * @param <V>
     * @param map
     */
    public static <V> void makeMapSerializable(final Map<String, V> map) {
        // now filter out all non-serializable values
        final Set<String> keysToRemove = new LinkedHashSet<>();
        for (final Map.Entry<String, V> mapEntry : map.entrySet()) {
            final Object entryValue = mapEntry.getValue();
            if (entryValue != null && !(entryValue instanceof Serializable)) {
                keysToRemove.add(mapEntry.getKey());
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Found Map value that is not Serializable: " + mapEntry.getKey() + "=" + mapEntry.getValue(), MODULE);
                }
            }
        }
        for (final String keyToRemove : keysToRemove) {
            map.remove(keyToRemove);
        }
    }

    /**
     * This change an ArrayList to be Serializable by removing all entries that are not Serializable.
     * @param arrayList
     */
    public static <V> void makeArrayListSerializable(final ArrayList<Object> arrayList) {
        // now filter out all non-serializable values
        final Iterator<Object> itr = arrayList.iterator();
        while (itr.hasNext()) {
            final Object obj = itr.next();
            if (!(obj instanceof Serializable)) {
                itr.remove();
            }
        }
    }

    /**
     * Sort a List of Maps by specified consistent keys.
     * @param listOfMaps List of Map objects to sort.
     * @param sortKeys List of Map keys to sort by.
     * @return a new List of sorted Maps.
     */
    public static List<Map<Object, Object>> sortMaps(final List<Map<Object, Object>> listOfMaps, final List<? extends String> sortKeys) {
        if (listOfMaps == null || sortKeys == null) {
            return null;
        }
        final List<Map<Object, Object>> toSort = new ArrayList<>(listOfMaps.size());
        toSort.addAll(listOfMaps);
        try {
            final MapComparator mc = new MapComparator(sortKeys);
            toSort.sort(mc);
        } catch (final Exception e) {
            Debug.logError(e, "Problems sorting list of maps; returning null.", MODULE);
            return null;
        }
        return toSort;
    }

    /**
     * Assuming outerMap not null; if null will throw a NullPointerException
     */
    public static <K, IK, V> Map<IK, V> getMapFromMap(final Map<K, Object> outerMap, final K key) {
        Map<IK, V> innerMap = UtilGenerics.cast(outerMap.get(key));
        if (innerMap == null) {
            innerMap = new HashMap<>();
            outerMap.put(key, innerMap);
        }
        return innerMap;
    }

    /**
     * Assuming outerMap not null; if null will throw a NullPointerException
     */
    public static <K, V> List<V> getListFromMap(final Map<K, Object> outerMap, final K key) {
        List<V> innerList = UtilGenerics.cast(outerMap.get(key));
        if (innerList == null) {
            innerList = new LinkedList<>();
            outerMap.put(key, innerList);
        }
        return innerList;
    }

    /**
     * Assuming theMap not null; if null will throw a NullPointerException
     */
    public static <K> BigDecimal addToBigDecimalInMap(final Map<K, Object> theMap, final K mapKey, final BigDecimal addNumber) {
        final Object currentNumberObj = theMap.get(mapKey);
        BigDecimal currentNumber = null;
        if (currentNumberObj == null) {
            currentNumber = ZERO_BD;
        } else if (currentNumberObj instanceof BigDecimal) {
            currentNumber = (BigDecimal) currentNumberObj;
        } else if (currentNumberObj instanceof Double) {
            currentNumber = new BigDecimal((Double) currentNumberObj);
        } else if (currentNumberObj instanceof Long) {
            currentNumber = new BigDecimal((Long) currentNumberObj);
        } else {
            throw new IllegalArgumentException("In addToBigDecimalInMap found a Map value of a type not supported: "
                    + currentNumberObj.getClass().getName());
        }

        if (addNumber == null || ZERO_BD.compareTo(addNumber) == 0) {
            return currentNumber;
        }
        currentNumber = currentNumber.add(addNumber);
        theMap.put(mapKey, currentNumber);
        return currentNumber;
    }

    public static <T> T removeFirst(final List<T> lst) {
        return lst.remove(0);
    }

    public static <T> Set<T> collectionToSet(final Collection<T> c) {
        if (c == null) {
            return null;
        }
        Set<T> theSet = null;
        if (c instanceof Set<?>) {
            theSet = (Set<T>) c;
        } else {
            theSet = new LinkedHashSet<>();
            c.remove(null);
            theSet.addAll(c);
        }
        return theSet;
    }

    /**
     * Generates a String from given values delimited by delimiter.
     * @param values
     * @param delimiter
     * @return String
     */
    public static String collectionToString(final Collection<? extends Object> values, String delimiter) {
        if (UtilValidate.isEmpty(values)) {
            return null;
        }
        if (delimiter == null) {
            delimiter = "";
        }
        final StringBuilder out = new StringBuilder();

        for (final Object val : values) {
            out.append(UtilFormatOut.safeToString(val)).append(delimiter);
        }
        return out.toString();
    }

    /**
     * Create a set from the passed objects.
     * @param data
     * @return theSet
     */
    @SafeVarargs
    public static <T> Set<T> toSet(final T... data) {
        if (data == null) {
            return null;
        }
        final Set<T> theSet = new LinkedHashSet<>();
        for (final T elem : data) {
            theSet.add(elem);
        }
        return theSet;
    }

    public static <T> Set<T> toSet(final Collection<T> collection) {
        if (collection == null) {
            return null;
        }
        if (collection instanceof Set<?>) {
            return (Set<T>) collection;
        }
        final Set<T> theSet = new LinkedHashSet<>();
        theSet.addAll(collection);
        return theSet;
    }

    public static <T> Set<T> toSetArray(final T[] data) {
        if (data == null) {
            return null;
        }
        final Set<T> set = new LinkedHashSet<>();
        for (final T value : data) {
            set.add(value);
        }
        return set;
    }

    /**
     * Creates a list from passed objects.
     * @param data
     * @return list
     */
    @SafeVarargs
    public static <T> List<T> toList(final T... data) {
        if (data == null) {
            return null;
        }

        final List<T> list = new LinkedList<>();

        for (final T t : data) {
            list.add(t);
        }

        return list;
    }

    public static <T> List<T> toListArray(final T[] data) {
        if (data == null) {
            return null;
        }
        final List<T> list = new LinkedList<>();
        for (final T value : data) {
            list.add(value);
        }
        return list;
    }

    public static <K, V> void addToListInMap(final V element, final Map<K, Object> theMap, final K listKey) {
        List<V> theList = UtilGenerics.cast(theMap.get(listKey));
        if (theList == null) {
            theList = new LinkedList<>();
            theMap.put(listKey, theList);
        }
        theList.add(element);
    }

    public static <K, V> void addToSetInMap(final V element, final Map<K, Set<V>> theMap, final K setKey) {
        Set<V> theSet = UtilGenerics.cast(theMap.get(setKey));
        if (theSet == null) {
            theSet = new LinkedHashSet<>();
            theMap.put(setKey, theSet);
        }
        theSet.add(element);
    }

    public static <K, V> void addToSortedSetInMap(final V element, final Map<K, Set<V>> theMap, final K setKey) {
        Set<V> theSet = UtilGenerics.cast(theMap.get(setKey));
        if (theSet == null) {
            theSet = new TreeSet<>();
            theMap.put(setKey, theSet);
        }
        theSet.add(element);
    }

    /**
     * Converts an <code>Object</code> to a <code>double</code>. Returns
     * zero if conversion is not possible.
     * @param obj Object to convert
     * @return double value
     */
    public static double toDouble(final Object obj) {
        final Double result = toDoubleObject(obj);
        return result == null ? 0.0 : result;
    }

    /**
     * Converts an <code>Object</code> to a <code>Double</code>. Returns
     * <code>null</code> if conversion is not possible.
     * @param obj Object to convert
     * @return Double
     */
    public static Double toDoubleObject(final Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Double) {
            return (Double) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        Double result = null;
        try {
            result = Double.parseDouble(obj.toString());
        } catch (final Exception e) {
            Debug.logError(e, MODULE);
        }

        return result;
    }

    /**
     * Converts an <code>Object</code> to an <code>int</code>. Returns
     * zero if conversion is not possible.
     * @param obj Object to convert
     * @return int value
     */
    public static int toInteger(final Object obj) {
        final Integer result = toIntegerObject(obj);
        return result == null ? 0 : result;
    }

    /**
     * Converts an <code>Object</code> to an <code>Integer</code>. Returns
     * <code>null</code> if conversion is not possible.
     * @param obj Object to convert
     * @return Integer
     */
    public static Integer toIntegerObject(final Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        Integer result = null;
        try {
            result = Integer.parseInt(obj.toString());
        } catch (final Exception e) {
            Debug.logError(e, MODULE);
        }

        return result;
    }

    /**
     * Converts an <code>Object</code> to a <code>BigDecimal</code>. Returns
     * <code>BigDecimal.ZERO</code> if conversion is not possible.
     * @param obj Object to convert
     * @return BigDecimal
     */
    public static BigDecimal toBigDecimal(final String obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(obj);
        } catch (final Exception e) {
            Debug.logError(e, MODULE);
        }

        return BigDecimal.ZERO;
    }
    
    /**
     * Converts an <code>Object</code> to a <code>long</code>. Returns
     * zero if conversion is not possible.
     * @param obj Object to convert
     * @return long value
     */
    public static long toLong(final Object obj) {
        final Long result = toLongObject(obj);
        return result == null ? 0 : result;
    }

    /**
     * Converts an <code>Object</code> to a <code>Long</code>. Returns
     * <code>null</code> if conversion is not possible.
     * @param obj Object to convert
     * @return Long
     */
    public static Long toLongObject(final Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        Long result = null;
        try {
            result = Long.parseLong(obj.toString());
        } catch (final Exception e) {
            Debug.logError(e, MODULE);
        }

        return result;
    }

    /**
     * Adds value to the key entry in theMap, or creates a new one if not already there
     * @param theMap
     * @param key
     * @param value
     */
    public static <K> void addToDoubleInMap(final Map<K, Object> theMap, final K key, final Double value) {
        final Double curValue = (Double) theMap.get(key);
        if (curValue != null) {
            theMap.put(key, curValue + value);
        } else {
            theMap.put(key, value);
        }
    }

    /**
     * Parse a locale string Locale object
     * @param localeString The locale string (en_US)
     * @return Locale The new Locale object or null if no valid locale can be interpreted
     */
    public static Locale parseLocale(final String localeString) {
        if (UtilValidate.isEmpty(localeString)) {
            return null;
        }

        Locale locale = null;
        if (localeString.length() == 2) {
            // two letter language code
            locale = new Locale.Builder().setLanguage(localeString).build();
        } else if (localeString.length() == 5) {
            // positions 0-1 language, 3-4 are country
            final String language = localeString.substring(0, 2);
            final String country = localeString.substring(3, 5);
            locale = new Locale.Builder().setLanguage(language).setRegion(country).build();
        } else if (localeString.length() > 6) {
            // positions 0-1 language, 3-4 are country, 6 and on are special extensions
            final String language = localeString.substring(0, 2);
            final String country = localeString.substring(3, 5);
            final String extension = localeString.substring(6);
            locale = new Locale(language, country, extension);
        } else {
            Debug.logWarning("Do not know what to do with the localeString [" + localeString + "], should be length 2, 5, or greater than 6, "
                    + "returning null", MODULE);
        }

        return locale;
    }

    /**
     * The input can be a String, Locale, or even null and a valid Locale will always be returned; if nothing else works, returns the default locale.
     * @param localeObject An Object representing the locale
     */
    public static Locale ensureLocale(final Object localeObject) {
        if (localeObject instanceof String) {
            final Locale locale = parseLocale((String) localeObject);
            if (locale != null) {
                return locale;
            }
        } else if (localeObject instanceof Locale) {
            return (Locale) localeObject;
        }
        return Locale.getDefault();
    }

    /**
     * Returns a List of available locales sorted by display name
     */
    public static List<Locale> availableLocales() {
        return LocaleHolder.AVAIL_LOCALE_LIST;
    }

    /**
     * List of domains or IP addresses to be checked to prevent Host Header Injection,
     * no spaces after commas, no wildcard, can be extended of course...
     * @return List of domains or IP addresses to be checked to prevent Host Header Injection,
     */
    public static List<String> getHostHeadersAllowed() {
        final String hostHeadersAllowedString = UtilProperties.getPropertyValue("security", "host-headers-allowed", "localhost");
        List<String> hostHeadersAllowed = null;
        if (UtilValidate.isNotEmpty(hostHeadersAllowedString)) {
            hostHeadersAllowed = StringUtil.split(hostHeadersAllowedString, ",");
            hostHeadersAllowed = Collections.unmodifiableList(hostHeadersAllowed);
        }
        return hostHeadersAllowed;
    }

    /**
     * @deprecated use Thread.sleep()
     */
    @Deprecated
    public static void staticWait(final long timeout) throws InterruptedException {
        Thread.sleep(timeout);
    }

    public static void copyFile(final File sourceLocation, final File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            throw new IOException("File is a directory, not a file, cannot copy");
        }
        try (InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);) {
            // Copy the bits from instream to outstream
            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    public static int getViewLastIndex(final int listSize, final int viewSize) {
        return (int) Math.ceil(listSize / (float) viewSize) - 1;
    }

    public static Map<String, String> splitPhoneNumber(final String phoneNumber, final Delegator delegator) {
        final Map<String, String> result = new HashMap<>();
        try {
            final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            final String defaultCountry = EntityUtilProperties.getPropertyValue("general", "country.geo.id.default", delegator);
            final GenericValue defaultGeo = EntityQuery.use(delegator).from("Geo").where("geoId", defaultCountry).cache().queryOne();
            final String defaultGeoCode = defaultGeo != null ? defaultGeo.getString("geoCode") : "US";
            final PhoneNumber phNumber = phoneUtil.parse(phoneNumber, defaultGeoCode);
            if (phoneUtil.isValidNumber(phNumber) || phoneUtil.isPossibleNumber(phNumber)) {
                final String nationalSignificantNumber = phoneUtil.getNationalSignificantNumber(phNumber);
                final int areaCodeLength = phoneUtil.getLengthOfGeographicalAreaCode(phNumber);
                result.put("countryCode", Integer.toString(phNumber.getCountryCode()));
                if (areaCodeLength > 0) {
                    result.put("areaCode", nationalSignificantNumber.substring(0, areaCodeLength));
                    result.put("contactNumber", nationalSignificantNumber.substring(areaCodeLength));
                } else {
                    result.put("areaCode", "");
                    result.put("contactNumber", nationalSignificantNumber);
                }
            } else {
                Debug.logError("Invalid phone number " + phoneNumber, MODULE);
                result.put(ModelService.ERROR_MESSAGE, "Invalid phone number");
            }
        } catch (GenericEntityException | NumberParseException ex) {
            Debug.logError(ex, MODULE);
            result.put(ModelService.ERROR_MESSAGE, ex.getMessage());
        }
        return result;
    }

    // Private lazy-initializer class
    private static final class LocaleHolder {
        private static final List<Locale> AVAIL_LOCALE_LIST = getAvailableLocaleList();

        private static List<Locale> getAvailableLocaleList() {
            final TreeMap<String, Locale> localeMap = new TreeMap<>();
            final String localesString = UtilProperties.getPropertyValue("general", "locales.available");
            if (UtilValidate.isNotEmpty(localesString)) {
                final List<String> idList = StringUtil.split(localesString, ",");
                for (final String id : idList) {
                    final Locale curLocale = parseLocale(id);
                    localeMap.put(curLocale.getDisplayName(), curLocale);
                }
            } else {
                final Locale[] locales = Locale.getAvailableLocales();
                for (int i = 0; i < locales.length && locales[i] != null; i++) {
                    final String displayName = locales[i].getDisplayName();
                    if (!displayName.isEmpty()) {
                        localeMap.put(displayName, locales[i]);
                    }
                }
            }
            return Collections.unmodifiableList(new ArrayList<>(localeMap.values()));
        }
    }

}
