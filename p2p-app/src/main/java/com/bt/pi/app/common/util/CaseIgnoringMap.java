package com.bt.pi.app.common.util;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

/**
 * This class wraps an inner Map to provide case-independence.
 * 
 * @author juan
 * 
 * @param <V>
 */
public class CaseIgnoringMap<V> implements Map<String, V> {

    private final Map<String, V> innerMap;

    public CaseIgnoringMap(Map<String, V> anInnerMap) {
        this.innerMap = anInnerMap;
    }

    @Override
    public void clear() {
        innerMap.clear();

    }

    @Override
    public boolean containsKey(Object key) {
        // TODO Auto-generated method stub
        return innerMap.containsKey(((String) key).toLowerCase(Locale.getDefault()));
    }

    @Override
    public boolean containsValue(Object value) {

        return innerMap.containsValue(value);
    }

    @Override
    public Set<java.util.Map.Entry<String, V>> entrySet() {

        return innerMap.entrySet();
    }

    @Override
    public V get(Object key) {
        return innerMap.get(((String) key).toLowerCase(Locale.getDefault()));
    }

    @Override
    public boolean isEmpty() {
        return innerMap.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return innerMap.keySet();
    }

    @Override
    public V put(String key, V value) {
        return innerMap.put(key.toLowerCase(Locale.getDefault()), value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        throw new NotImplementedException();
    }

    @Override
    public V remove(Object key) {
        return innerMap.remove(((String) key).toLowerCase(Locale.getDefault()));
    }

    @Override
    public int size() {
        return innerMap.size();
    }

    @Override
    public Collection<V> values() {
        return innerMap.values();
    }

    @Override
    public boolean equals(Object obj) {

        return innerMap.equals(obj);
    }

    @Override
    public int hashCode() {

        return innerMap.hashCode();
    }

    @Override
    public String toString() {

        return innerMap.toString();
    }

}
