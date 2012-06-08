package com.bt.pi.app.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.Region;

@RunWith(MockitoJUnitRunner.class)
public class CaseIgnoringMapTest {

    private Map<String, Region> innerMap = new HashMap<String, Region>();
    private CaseIgnoringMap<Region> caseIgnoringMap = new CaseIgnoringMap<Region>(innerMap);
    private final String key = "OneKey";
    private final String lowercaseKey = "onekey";
    private final String capitalisedKey = "ONEKEY";

    @org.junit.After
    public void after() {
        caseIgnoringMap.clear();
    }

    @Test
    public void shouldSupportCaseIndependentSearches() {
        // act
        caseIgnoringMap.put(key, new Region());
        // assert
        assertTrue(caseIgnoringMap.containsKey(key));
        assertTrue(caseIgnoringMap.containsKey(lowercaseKey));
        assertTrue(caseIgnoringMap.containsKey(capitalisedKey));
    }

    @Test
    public void shouldRemoveKeyWithDifferentCase() {
        // act
        caseIgnoringMap.put(key, new Region());
        assertEquals(1, caseIgnoringMap.size());
        caseIgnoringMap.remove(capitalisedKey);
        // assert
        assertEquals(0, caseIgnoringMap.size());
    }

    @Test(expected = NotImplementedException.class)
    public void shouldThrowExceptionWhenCallingPutAll() {
        caseIgnoringMap.putAll(new HashMap<String, Region>());
    }

    @Test
    public void shouldBeEmpty() {
        assertTrue(caseIgnoringMap.isEmpty());
    }

    @Test
    public void shouldReturnInstanceOfSet() {
        assertTrue(caseIgnoringMap.keySet() instanceof Set<?>);
    }
}
