package com.quantum.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class QuantumSimulatorTest {
    private final QuantumSimulator sim = new QuantumSimulator(new Random(42));

    private Map<String, Double> run(QuantumCircuit c) { return sim.probabilities(sim.run(c), c.getQubits()); }

    @Test void startsInZeroState() {
        assertEquals(1.0, run(new QuantumCircuit(2)).get("00"), 1e-9);
    }
    @Test void hadamardGivesFiftyFifty() {
        var c = new QuantumCircuit(1); c.addGate(QuantumGate.H, 0);
        assertEquals(0.5, run(c).get("0"), 1e-9);
    }
    @Test void xFlipsTheQubit() {
        var c = new QuantumCircuit(1); c.addGate(QuantumGate.X, 0);
        assertEquals(1.0, run(c).get("1"), 1e-9);
    }
    @Test void hTwiceIsIdentity() {
        var c = new QuantumCircuit(1); c.addGate(QuantumGate.H, 0); c.addGate(QuantumGate.H, 0);
        assertEquals(1.0, run(c).get("0"), 1e-9);
    }
    @Test void bellStateIsEntangled() {
        var c = new QuantumCircuit(2); c.addGate(QuantumGate.H, 0); c.addCNOT(0, 1);
        var p = run(c);
        assertEquals(0.5, p.get("00"), 1e-9);
        assertEquals(0.5, p.get("11"), 1e-9);
        assertEquals(0.0, p.get("01"), 1e-9);
        double[] b = sim.bloch(sim.run(c));
        assertEquals(0.0, Math.sqrt(b[0] * b[0] + b[1] * b[1] + b[2] * b[2]), 1e-9);
    }
    @Test void measurementAlwaysReturnsPossibleOutcome() {
        var c = new QuantumCircuit(2); c.addGate(QuantumGate.H, 0); c.addCNOT(0, 1);
        for (int i = 0; i < 50; i++) {
            String m = sim.measure(sim.run(c), 2);
            assertEquals(true, m.equals("00") || m.equals("11"));
        }
    }
    @Test void rejectsBadQubitIndex() {
        assertThrows(IllegalArgumentException.class, () -> new QuantumCircuit(2).addGate(QuantumGate.H, 5));
    }
}
