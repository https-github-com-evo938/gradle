/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.classpath;

import org.codehaus.groovy.runtime.callsite.AbstractCallSite;
import org.codehaus.groovy.runtime.callsite.CallSite;
import org.codehaus.groovy.runtime.callsite.CallSiteArray;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Instrumented {
    private static final Listener NO_OP = (key, value, consumer) -> {
    };
    private static final AtomicReference<Listener> LISTENER = new AtomicReference<>(NO_OP);

    public static void setListener(Listener listener) {
        LISTENER.set(listener);
    }

    public static void discardListener() {
        LISTENER.set(NO_OP);
    }

    // Called by generated code
    public static void groovyCallSites(CallSiteArray array) {
        for (CallSite callSite : array.array) {
            if (callSite.getName().equals("getProperty")) {
                array.array[callSite.getIndex()] = new SystemPropertyCallSite(callSite);
            } else if (callSite.getName().equals("properties")) {
                array.array[callSite.getIndex()] = new SystemPropertiesCallSite(callSite);
            }
        }
    }

    // Called by generated code.
    public static String systemProperty(String key, String consumer) {
        String value = System.getProperty(key);
        LISTENER.get().systemPropertyQueried(key, value, consumer);
        return value;
    }

    // Called by generated code.
    public static String systemProperty(String key, String defaultValue, String consumer) {
        String value = System.getProperty(key);
        LISTENER.get().systemPropertyQueried(key, value, consumer);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    // Called by generated code.
    public static Properties systemProperties(String consumer) {
        return new DecoratingProperties(System.getProperties(), consumer);
    }

    public interface Listener {
        /**
         * @param consumer The name of the class that is reading the property value
         */
        void systemPropertyQueried(String key, @Nullable String value, String consumer);
    }

    private static class DecoratingProperties extends Properties {
        private final String consumer;
        private final Properties delegate;

        public DecoratingProperties(Properties delegate, String consumer) {
            this.consumer = consumer;
            this.delegate = delegate;
        }

        @Override
        public Enumeration<?> propertyNames() {
            return delegate.propertyNames();
        }

        @Override
        public Set<String> stringPropertyNames() {
            return delegate.stringPropertyNames();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public Enumeration<Object> keys() {
            return delegate.keys();
        }

        @Override
        public Enumeration<Object> elements() {
            return delegate.elements();
        }

        @Override
        public Set<Object> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<Object> values() {
            return delegate.values();
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public void forEach(BiConsumer<? super Object, ? super Object> action) {
            delegate.forEach(action);
        }

        @Override
        public void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {
            delegate.replaceAll(function);
        }

        @Override
        public Object putIfAbsent(Object key, Object value) {
            return delegate.putIfAbsent(key, value);
        }

        @Override
        public boolean remove(Object key, Object value) {
            return delegate.remove(key, value);
        }

        @Override
        public boolean replace(Object key, Object oldValue, Object newValue) {
            return delegate.replace(key, oldValue, newValue);
        }

        @Override
        public Object replace(Object key, Object value) {
            return delegate.replace(key, value);
        }

        @Override
        public Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {
            return delegate.computeIfAbsent(key, mappingFunction);
        }

        @Override
        public Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            return delegate.computeIfPresent(key, remappingFunction);
        }

        @Override
        public Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            return delegate.compute(key, remappingFunction);
        }

        @Override
        public Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            return delegate.merge(key, value, remappingFunction);
        }

        @Override
        public boolean contains(Object value) {
            return delegate.contains(value);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public Object put(Object key, Object value) {
            return delegate.put(key, value);
        }

        @Override
        public Object setProperty(String key, String value) {
            return delegate.setProperty(key, value);
        }

        @Override
        public Object remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<?, ?> t) {
            delegate.putAll(t);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public String getProperty(String key) {
            String value = delegate.getProperty(key);
            LISTENER.get().systemPropertyQueried(key, value, consumer);
            return value;
        }

        @Override
        public String getProperty(String key, String defaultValue) {
            String value = delegate.getProperty(key);
            LISTENER.get().systemPropertyQueried(key, value, consumer);
            if (value == null) {
                return defaultValue;
            }
            return value;
        }

        @Override
        public Object getOrDefault(Object key, Object defaultValue) {
            return getProperty((String) key, (String) defaultValue);
        }

        @Override
        public Object get(Object key) {
            return getProperty((String) key);
        }
    }

    private static class SystemPropertyCallSite extends AbstractCallSite {
        public SystemPropertyCallSite(CallSite callSite) {
            super(callSite);
        }

        @Override
        public Object call(Object receiver, Object arg) throws Throwable {
            if (receiver.equals(System.class)) {
                return systemProperty((String) arg, array.owner.getName());
            } else {
                return super.call(receiver, arg);
            }
        }

        @Override
        public Object call(Object receiver, Object arg1, Object arg2) throws Throwable {
            if (receiver.equals(System.class)) {
                return systemProperty((String) arg1, (String) arg2, array.owner.getName());
            } else {
                return super.call(receiver, arg1, arg2);
            }
        }
    }

    private static class SystemPropertiesCallSite extends AbstractCallSite {
        public SystemPropertiesCallSite(CallSite callSite) {
            super(callSite);
        }

        @Override
        public Object callGetProperty(Object receiver) throws Throwable {
            if (receiver.equals(System.class)) {
                return systemProperties(array.owner.getName());
            } else {
                return super.callGetProperty(receiver);
            }
        }
    }
}
