package org.example.clinic.server.notification;

import org.example.clinic.server.model.Appointment;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentEventBusTest {

    @Test
    void allListenersReceiveEvent() {
        AppointmentEventBus bus = new AppointmentEventBus();
        AtomicInteger counter = new AtomicInteger();
        bus.subscribe(e -> counter.incrementAndGet());
        bus.subscribe(e -> counter.incrementAndGet());

        bus.publish(new AppointmentEvent(AppointmentEvent.Type.BOOKED, new Appointment()));
        assertEquals(2, counter.get());
    }

    @Test
    void failingListenerDoesNotBreakChain() {
        AppointmentEventBus bus = new AppointmentEventBus();
        AtomicInteger counter = new AtomicInteger();
        bus.subscribe(e -> {
            throw new IllegalStateException("boom");
        });
        bus.subscribe(e -> counter.incrementAndGet());
        bus.publish(new AppointmentEvent(AppointmentEvent.Type.CANCELLED, new Appointment()));
        assertEquals(1, counter.get());
    }

    @Test
    void unsubscribedListenerNoLongerReceives() {
        AppointmentEventBus bus = new AppointmentEventBus();
        AtomicInteger counter = new AtomicInteger();
        AppointmentEventListener listener = e -> counter.incrementAndGet();
        bus.subscribe(listener);
        bus.unsubscribe(listener);
        bus.publish(new AppointmentEvent(AppointmentEvent.Type.COMPLETED, new Appointment()));
        assertEquals(0, counter.get());
    }
}
