package com.bt.pi.app.instancemanager.handlers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.application.MessageContextFactory;

public class TerminateBuriedInstancesContinuationTest {
    private MessageContextFactory messageContextFactory;
    private Map<String, InstanceStateTransition> instancesMap;
    private PiIdBuilder piIdBuilder;

    private TerminateBuriedInstancesContinuation terminateBuriedInstancesContinuation;

    @Before
    public void setup() {
        piIdBuilder = mock(PiIdBuilder.class);
        messageContextFactory = mock(MessageContextFactory.class);
        instancesMap = new HashMap<String, InstanceStateTransition>();

        terminateBuriedInstancesContinuation = new TerminateBuriedInstancesContinuation(piIdBuilder, messageContextFactory, instancesMap);
    }

    @Test
    public void shouldReturnNullIfInstanceIsNotBuried() throws Exception {
        // setup
        Instance instance = mock(Instance.class);
        when(instance.isBuried()).thenReturn(false);

        // act
        Instance result = terminateBuriedInstancesContinuation.update(instance, null);

        // assert
        assertNull(result);
    }

    @Test
    public void shouldTerminateIfInstanceIsBuried() throws Exception {
        // setup
        Instance instance = mock(Instance.class);
        when(instance.isBuried()).thenReturn(true);

        // act
        Instance result = terminateBuriedInstancesContinuation.update(instance, null);

        // assert
        assertThat(result, equalTo(instance));
        verify(instance).setState(InstanceState.TERMINATED);
    }

    @Test
    public void shouldReturnNullIfInstanceNotFound() {
        // act
        Instance result = terminateBuriedInstancesContinuation.update(null, null);

        // assert
        assertNull(result);
    }
}
