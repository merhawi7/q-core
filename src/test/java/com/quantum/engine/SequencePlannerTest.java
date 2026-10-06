package com.quantum.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.quantum.api.DeviceEvent;
import com.quantum.api.SequencePlanner;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SequencePlannerTest {
    private final QuantumSimulator sim = new QuantumSimulator(new Random(1));

    @Test void bellCircuitPassesFiveLayersThenRunsGatesOnTheChip() {
        var c = new QuantumCircuit(2); c.addGate(QuantumGate.H, 0); c.addCNOT(0, 1);
        List<DeviceEvent> ev = new SequencePlanner(sim).plan(c, "11");
        assertEquals(List.of(1, 2, 3, 4, 5, 5, 5, 5, 5, 0), ev.stream().map(DeviceEvent::stage).toList());
        assertEquals(0, ev.get(0).atMs());
        assertEquals(6300, ev.get(5).atMs());          // first gate
        assertEquals("H", ev.get(5).gate());
        assertEquals("CNOT", ev.get(6).gate());
        assertEquals(1.0, ev.get(8).p().get("11"), 1e-9);
        assertEquals("measurement", ev.get(8).mode());
    }
    @Test void emptyCircuitStillReachesTheResult() {
        var ev = new SequencePlanner(sim).plan(new QuantumCircuit(1), "0");
        assertEquals(List.of(1, 2, 3, 4, 5, 5, 5, 0), ev.stream().map(DeviceEvent::stage).toList());
    }
    @Test void eventTimesNeverGoBackwards() {
        var c = new QuantumCircuit(2); c.addGate(QuantumGate.H, 0); c.addCNOT(0, 1);
        var ev = new SequencePlanner(sim).plan(c, "00");
        for (int i = 1; i < ev.size(); i++) assertEquals(true, ev.get(i).atMs() >= ev.get(i - 1).atMs());
    }
}
