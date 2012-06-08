package com.bt.pi.app.common.entities.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.util.ResourceRange;
import com.bt.pi.core.parser.KoalaJsonParser;

public class ResourceRangeTest {
    private ResourceRange resourceRange;

    @Before
    public void before() {
        resourceRange = new ResourceRange(0L, 0L);
    }

    @Test
    public void shouldIterateOverOneItem() {
        // act
        Iterator<Long> iter = resourceRange.iterator();

        // assert
        assertTrue(iter.hasNext());
        assertEquals(new Long(0), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void shouldIterateOverARange() {
        // setup
        resourceRange = new ResourceRange(5L, 7L);

        // act
        Iterator<Long> iter = resourceRange.iterator();

        // assert
        assertTrue(iter.hasNext());
        assertEquals(new Long(5), iter.next());
        assertEquals(new Long(6), iter.next());
        assertEquals(new Long(7), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void shouldIterateOverOneRangeItemWithStepSize() {
        // setup
        resourceRange = new ResourceRange(5L, 7L, 3);

        // act
        Iterator<Long> iter = resourceRange.iterator();

        // assert
        assertTrue(iter.hasNext());
        assertEquals(new Long(5), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void shouldNotIterateOverRangeWhenStepSizeNotSatisfied() {
        // setup
        resourceRange = new ResourceRange(5L, 6L, 3);

        // act
        Iterator<Long> iter = resourceRange.iterator();

        // assert
        assertFalse(iter.hasNext());
    }

    @Test
    public void shouldIterateOverARangeWithStepSize() {
        // setup
        resourceRange = new ResourceRange(5L, 12L, 3);

        // act
        Iterator<Long> iter = resourceRange.iterator();

        // assert
        assertTrue(iter.hasNext());
        assertEquals(new Long(5), iter.next());
        assertEquals(new Long(8), iter.next());
        assertEquals(new Long(11), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void shouldIterateOverInvalidRange() {
        // setup
        resourceRange = new ResourceRange(7L, 3L, 2);

        // act
        Iterator<Long> iter = resourceRange.iterator();

        // assert
        assertFalse(iter.hasNext());
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJson() {
        // setup
        KoalaJsonParser koalaJsonParser = new KoalaJsonParser();

        // act
        String json = koalaJsonParser.getJson(resourceRange);
        ResourceRange reverse = (ResourceRange) koalaJsonParser.getObject(json, ResourceRange.class);

        // assert
        assertEquals(resourceRange, reverse);
        assertEquals(resourceRange.hashCode(), reverse.hashCode());
        assertEquals(resourceRange.getMin(), reverse.getMin());
        assertEquals(resourceRange.getMax(), reverse.getMax());
    }
}
