package com.bt.pi.app.imagemanager.reporting;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImageReportEntityCollectionTest {

    public void setup() {

    }

    @Test
    public void shouldHaveSimpleNameAsUriScheme() {
        // act
        ImageReportEntityCollection entity = new ImageReportEntityCollection();
        // assert

        assertEquals(ImageReportEntityCollection.class.getSimpleName(), entity.getUriScheme());
    }

    @Test
    public void shouldReturnSimpleClassNameAsType() {
        // act
        ImageReportEntityCollection entity = new ImageReportEntityCollection();
        // assert

        assertEquals(ImageReportEntityCollection.class.getSimpleName(), entity.getType());
    }

}
