/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.driver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.debezium.annotation.Immutable;
import org.debezium.util.Collect;
import org.debezium.util.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An immutable representation of a Debezium configuration. A {@link Configuration} instance can be obtained
 * {@link #from(Properties) from Properties} or loaded from a {@link #load(File) file}, {@link #load(InputStream) stream},
 * {@link #load(Reader) reader}, {@link #load(URL) URL}, or from a {@link #load(String, ClassLoader) resource on the classpath}.
 * <p>
 * A Configuration object is basically a decorator around a {@link Properties} object. It has methods to get and convert
 * individual property values to numeric, boolean and String types, optionally using a default value if the given property value
 * does not exist. However, it is {@link Immutable immutable}, so it does not have any methods to set property values, allowing
 * it to be passed around and reused without concern that other components might change the underlying property values.
 * 
 * @author Randall Hauch
 */
@Immutable
public interface Configuration {

    /**
     * Obtain an empty configuration.
     * 
     * @return an empty configuration; never null
     */
    public static Configuration empty() {
        return new Configuration() {
            @Override
            public Set<String> keys() {
                return Collections.emptySet();
            }

            @Override
            public String getString(String key) {
                return null;
            }
        };
    }

    /**
     * Obtain a configuration instance by copying the supplied Properties object. The supplied {@link Properties} object is
     * copied so that the resulting Configuration cannot be modified.
     * 
     * @param properties the properties; may be null or empty
     * @return the configuration; never null
     */
    public static Configuration from(Properties properties) {
        Properties props = new Properties();
        if (properties != null) props.putAll(properties);
        return new Configuration() {
            @Override
            public String getString(String key) {
                return properties.getProperty(key);
            }

            @Override
            public Set<String> keys() {
                return properties.stringPropertyNames();
            }
        };
    }

    /**
     * Obtain a configuration instance by loading the Properties from the supplied URL.
     * 
     * @param url the URL to the stream containing the configuration properties; may not be null
     * @return the configuration; never null
     * @throws IOException if there is an error reading the stream
     */
    public static Configuration load(URL url) throws IOException {
        try (InputStream stream = url.openStream()) {
            return load(stream);
        }
    }

    /**
     * Obtain a configuration instance by loading the Properties from the supplied file.
     * 
     * @param file the file containing the configuration properties; may not be null
     * @return the configuration; never null
     * @throws IOException if there is an error reading the stream
     */
    public static Configuration load(File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            return load(stream);
        }
    }

    /**
     * Obtain a configuration instance by loading the Properties from the supplied stream.
     * 
     * @param stream the stream containing the properties; may not be null
     * @return the configuration; never null
     * @throws IOException if there is an error reading the stream
     */
    public static Configuration load(InputStream stream) throws IOException {
        try {
            Properties properties = new Properties();
            properties.load(stream);
            return from(properties);
        } finally {
            stream.close();
        }
    }

    /**
     * Obtain a configuration instance by loading the Properties from the supplied reader.
     * 
     * @param reader the reader containing the properties; may not be null
     * @return the configuration; never null
     * @throws IOException if there is an error reading the stream
     */
    public static Configuration load(Reader reader) throws IOException {
        try {
            Properties properties = new Properties();
            properties.load(reader);
            return from(properties);
        } finally {
            reader.close();
        }
    }

    /**
     * Obtain a configuration instance by loading the Properties from a file on the file system or classpath given by the supplied
     * path.
     * 
     * @param path the path to the file containing the configuration properties; may not be null
     * @param classLoader the class loader to use; may be null
     * @return the configuration; never null but possibly empty
     * @throws IOException if there is an error reading the stream
     */
    public static Configuration load(String path, ClassLoader classLoader) throws IOException {
        Logger logger = LoggerFactory.getLogger(Configuration.class);
        return load(path, classLoader, logger::debug);
    }

    /**
     * Obtain a configuration instance by loading the Properties from a file on the file system or classpath given by the supplied
     * path.
     * 
     * @param path the path to the file containing the configuration properties; may not be null
     * @param classLoader the class loader to use; may be null
     * @param logger the function that will be called with status updates; may be null
     * @return the configuration; never null but possibly empty
     * @throws IOException if there is an error reading the stream
     */
    public static Configuration load(String path, ClassLoader classLoader, Consumer<String> logger) throws IOException {
        try (InputStream stream = IoUtil.getResourceAsStream(path, classLoader, null, null, logger)) {
            Properties props = new Properties();
            if (stream != null) {
                props.load(stream);
            }
            return from(props);
        }
    }

    /**
     * Determine whether this configuration contains a key-value pair with the given key and the value is non-null
     * 
     * @param key the key
     * @return true if the configuration contains the key, or false otherwise
     */
    default public boolean hasKey(String key) {
        return getString(key) != null;
    }

    /**
     * Get the set of keys in this configuration.
     * 
     * @return the set of keys; never null but possibly empty
     */
    public Set<String> keys();

    /**
     * Get the string value associated with the given key.
     * 
     * @param key the key for the configuration property
     * @return the value, or null if the key is null or there is no such key-value pair in the configuration
     */
    public String getString(String key);

    /**
     * Get the string value associated with the given key, returning the default value if there is no such key-value pair.
     * 
     * @param key the key for the configuration property
     * @param defaultValue the value that should be returned by default if there is no such key-value pair in the configuration;
     *            may be null
     * @return the configuration value, or the {@code defaultValue} if there is no such key-value pair in the configuration
     */
    default public String getString(String key, String defaultValue) {
        return getString(key, () -> defaultValue);
    }

    /**
     * Get the string value associated with the given key, returning the default value if there is no such key-value pair.
     * 
     * @param key the key for the configuration property
     * @param defaultValueSupplier the supplier of value that should be returned by default if there is no such key-value pair in
     *            the configuration; may be null and may return null
     * @return the configuration value, or the {@code defaultValue} if there is no such key-value pair in the configuration
     */
    default public String getString(String key, Supplier<String> defaultValueSupplier) {
        String value = getString(key);
        return value != null ? value : (defaultValueSupplier != null ? defaultValueSupplier.get() : null);
    }

    /**
     * Get the string value(s) associated with the given key, where the supplied regular expression is used to parse the single
     * string value into multiple values.
     * 
     * @param key the key for the configuration property
     * @param regex the delimiting regular expression
     * @return the list of string values; null only if there is no such key-value pair in the configuration
     * @see String#split(String)
     */
    default public List<String> getStrings(String key, String regex) {
        String value = getString(key);
        if (value == null) return null;
        return Collect.arrayListOf(value.split(regex));
    }

    /**
     * Get the integer value associated with the given key.
     * 
     * @param key the key for the configuration property
     * @return the integer value, or null if the key is null, there is no such key-value pair in the configuration, or the value
     *         could not be parsed as an integer
     */
    default public Integer getInteger(String key) {
        return getInteger(key, null);
    }

    /**
     * Get the long value associated with the given key.
     * 
     * @param key the key for the configuration property
     * @return the integer value, or null if the key is null, there is no such key-value pair in the configuration, or the value
     *         could not be parsed as an integer
     */
    default public Long getLong(String key) {
        return getLong(key, null);
    }

    /**
     * Get the boolean value associated with the given key.
     * 
     * @param key the key for the configuration property
     * @return the boolean value, or null if the key is null, there is no such key-value pair in the configuration, or the value
     *         could not be parsed as a boolean value
     */
    default public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    /**
     * Get the integer value associated with the given key, returning the default value if there is no such key-value pair or
     * if the value could not be {@link Integer#parseInt(String) parsed} as an integer.
     * 
     * @param key the key for the configuration property
     * @param defaultValue the default value
     * @return the integer value, or null if the key is null, there is no such key-value pair in the configuration, or the value
     *         could not be parsed as an integer
     */
    default public int getInteger(String key, int defaultValue) {
        return getInteger(key, () -> defaultValue).intValue();
    }

    /**
     * Get the long value associated with the given key, returning the default value if there is no such key-value pair or
     * if the value could not be {@link Long#parseLong(String) parsed} as a long.
     * 
     * @param key the key for the configuration property
     * @param defaultValue the default value
     * @return the long value, or null if the key is null, there is no such key-value pair in the configuration, or the value
     *         could not be parsed as a long
     */
    default public long getLong(String key, long defaultValue) {
        return getLong(key, () -> defaultValue).longValue();
    }

    /**
     * Get the boolean value associated with the given key, returning the default value if there is no such key-value pair or
     * if the value could not be {@link Boolean#parseBoolean(String) parsed} as a boolean value.
     * 
     * @param key the key for the configuration property
     * @param defaultValue the default value
     * @return the boolean value, or null if the key is null, there is no such key-value pair in the configuration, or the value
     *         could not be parsed as a boolean value
     */
    default public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key, () -> defaultValue).booleanValue();
    }

    /**
     * Get the integer value associated with the given key, using the given supplier to obtain a default value if there is no such
     * key-value pair.
     * 
     * @param key the key for the configuration property
     * @param defaultValueSupplier the supplier for the default value; may be null
     * @return the integer value, or null if the key is null, there is no such key-value pair in the configuration, the
     *         {@code defaultValueSupplier} reference is null, or there is a key-value pair in the configuration but the value
     *         could not be parsed as an integer
     */
    default public Integer getInteger(String key, IntSupplier defaultValueSupplier) {
        String value = getString(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
            }
        }
        return defaultValueSupplier != null ? defaultValueSupplier.getAsInt() : null;
    }

    /**
     * Get the long value associated with the given key, using the given supplier to obtain a default value if there is no such
     * key-value pair.
     * 
     * @param key the key for the configuration property
     * @param defaultValueSupplier the supplier for the default value; may be null
     * @return the long value, or null if the key is null, there is no such key-value pair in the configuration, the
     *         {@code defaultValueSupplier} reference is null, or there is a key-value pair in the configuration but the value
     *         could not be parsed as a long
     */
    default public Long getLong(String key, LongSupplier defaultValueSupplier) {
        String value = getString(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
            }
        }
        return defaultValueSupplier != null ? defaultValueSupplier.getAsLong() : null;
    }

    /**
     * Get the boolean value associated with the given key, using the given supplier to obtain a default value if there is no such
     * key-value pair.
     * 
     * @param key the key for the configuration property
     * @param defaultValueSupplier the supplier for the default value; may be null
     * @return the boolean value, or null if the key is null, there is no such key-value pair in the configuration, the
     *         {@code defaultValueSupplier} reference is null, or there is a key-value pair in the configuration but the value
     *         could not be parsed as a boolean value
     */
    default public Boolean getBoolean(String key, BooleanSupplier defaultValueSupplier) {
        String value = getString(key);
        if (value != null) {
            value = value.trim().toLowerCase();
            if (Boolean.valueOf(value)) return Boolean.TRUE;
            if (value.equals("false")) return false;
        }
        return defaultValueSupplier != null ? defaultValueSupplier.getAsBoolean() : null;
    }

    /**
     * Get an instance of the class given by the value in the configuration associated with the given key.
     * 
     * @param key the key for the configuration property
     * @param clazz the Class of which the resulting object is expected to be an instance of; may not be null
     * @return the new instance, or null if there is no such key-value pair in the configuration or if there is a key-value
     *         configuration but the value could not be converted to an existing class with a zero-argument constructor
     */
    default public <T> T getInstance(String key, Class<T> clazz) {
        return getInstance(key, clazz, () -> getClass().getClassLoader());
    }

    /**
     * Get an instance of the class given by the value in the configuration associated with the given key.
     * 
     * @param key the key for the configuration property
     * @param clazz the Class of which the resulting object is expected to be an instance of; may not be null
     * @param classloaderSupplier the supplier of the ClassLoader to be used to load the resulting class; may be null if this
     *            class' ClassLoader should be used
     * @return the new instance, or null if there is no such key-value pair in the configuration or if there is a key-value
     *         configuration but the value could not be converted to an existing class with a zero-argument constructor
     */
    @SuppressWarnings("unchecked")
    default public <T> T getInstance(String key, Class<T> clazz, Supplier<ClassLoader> classloaderSupplier) {
        String className = getString(key);
        if (className != null) {
            ClassLoader classloader = classloaderSupplier != null ? classloaderSupplier.get() : getClass().getClassLoader();
            try {
                return (T) classloader.loadClass(className);
            } catch (ClassNotFoundException e) {
            }
        }
        return null;
    }

    /**
     * Return a new {@link Configuration} that contains only the subset of keys that match the given prefix.
     * If desired, the keys in the resulting Configuration will have the prefix (plus any terminating "{@code .}" character if
     * needed) removed.
     * <p>
     * This method returns this Configuration instance if the supplied {@code prefix} is null or empty.
     * 
     * @param prefix the prefix
     * @param removePrefix true if the prefix (and any subsequent "{@code .}" character) should be removed from the keys in the
     *            resulting Configuration, or false if the keys in this Configuration should be used as-is in the resulting
     *            Configuration
     * @return the subset of this Configuration; never null
     */
    default public Configuration subset(String prefix, boolean removePrefix) {
        if (prefix == null) return this;
        prefix = prefix.trim();
        if (prefix.isEmpty()) return this;
        String prefixWithSeparator = prefix.endsWith(".") ? prefix : prefix + ".";
        int minLength = prefixWithSeparator.length();
        Predicate<String> matcher = key -> key.startsWith(prefixWithSeparator);
        Function<String, String> prefixRemover = removePrefix ? key -> key.substring(minLength) : key -> key;
        return subset(matcher, prefixRemover);
    }

    /**
     * Return a new {@link Configuration} that contains only the subset of keys that match the given prefix.
     * If desired, the keys in the resulting Configuration will have the prefix (plus any terminating "{@code .}" character if
     * needed) removed.
     * 
     * @param matcher the function that determines whether a key should be included in the subset
     * @param mapper that function that maps an existing key in this Configuration into an alternative key in the resulting
     *            Configuration
     * @return the subset Configuration; never null
     */
    default public Configuration subset(Predicate<? super String> matcher, Function<String, String> mapper) {
        Function<String, String> prefixRemover = mapper != null ? mapper : key -> key;
        Set<String> keys = Collections.unmodifiableSet(
                                      keys().stream()
                                            .filter(key -> key != null) // may not be null
                                            .filter(matcher) // only keys that match the predicate
                                            .map(prefixRemover) // remove prefix if desired
                                            .collect(Collectors.toSet()));
        Configuration delegate = this;
        return new Configuration() {
            @Override
            public Set<String> keys() {
                return keys;
            }

            @Override
            public String getString(String key) {
                return keys.contains(prefixRemover.apply(key)) ? delegate.getString(key) : null;
            }
        };
    }

    /**
     * Determine if this configuration is empty and has no properties.
     * 
     * @return {@code true} if empty, or {@code false} otherwise
     */
    default public boolean isEmpty() {
        return keys().isEmpty();
    }

    /**
     * Get a copy of these configuration properties as a Properties object.
     * 
     * @return the properties object; never null
     */
    default public Properties asProperties() {
        Properties props = new Properties();
        keys().forEach(key -> props.setProperty(key, getString(key)));
        return props;
    }

    /**
     * Return a copy of this configuration except where acceptable system properties are used to overwrite properties copied from
     * this configuration. All system properties whose name has the given prefix are added, where the prefix is removed from the
     * system property name, it is converted to lower case, and each underscore character ('{@code _}') are replaced with a
     * period ('{@code .}').
     * 
     * @param prefix the required prefix for the system properties
     * @return the resulting properties converted from the system properties; never null, but possibly empty
     */
    default public Configuration withSystemProperties(String prefix) {
        int prefixLength = prefix.length();
        return withSystemProperties(input -> {
            if (input.startsWith(prefix)) {
                String withoutPrefix = input.substring(prefixLength).trim();
                if (withoutPrefix.length() > 0) {
                    // Convert to a properties format ...
                    return withoutPrefix.toLowerCase().replaceAll("[_]", ".");
                }
            }
            return null;
        });
    }

    /**
     * Return a copy of this configuration except where acceptable system properties are used to overwrite properties copied from
     * this configuration. Each of the system properties is examined and passed to the supplied function; if the result of the
     * function is a non-null string, then a property with that string as the name and the system property value are added to the
     * returned configuration.
     * 
     * @param propertyNameConverter the function that will convert the name of each system property to an applicable property name
     *            (or null if the system property name does not apply); may not be null
     * @return the resulting properties filtered from the input properties; never null, but possibly empty
     */
    default public Configuration withSystemProperties(Function<String, String> propertyNameConverter) {
        Properties props = asProperties();
        Properties systemProperties = System.getProperties();
        for (String key : systemProperties.stringPropertyNames()) {
            String propName = propertyNameConverter.apply(key);
            if (propName != null && propName.length() > 0) {
                String value = systemProperties.getProperty(key);
                props.setProperty(propName, value);
            }
        }
        return from(props);
    }
}
