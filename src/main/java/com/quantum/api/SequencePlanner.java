package com.quantum.api;

import com.quantum.engine.Operation;
import com.quantum.engine.QuantumCircuit;
import com.quantum.engine.QuantumSimulator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a circuit into the timed sequence shown on the device: the signal passes through five layers
 * (room temperature, 4K, 1K, 100 mK, 10 mK chip), the gates run on the chip, then the result is read.
 * Same timings as the dashboard. Pure logic with no Spring or network code, so it is easy to test.
 */
public final class SequencePlanner {
    public static final int L2 = 1500, L3 = 2800, L4 = 4100, L5 = 5400;
    public static final int GATES = 6300, STEP = 1500, MEASURE = 1400, RESULT = 2400;

    private final QuantumSimulator sim;

    public SequencePlanner(QuantumSimulator sim) { this.sim = sim; }

    public List<DeviceEvent> plan(QuantumCircuit circuit, String measured) {
        int n = circuit.getQubits();
        List<Operation> ops = circuit.getOperations();
        int total = ops.size();
        Map<String, Double> zero = probs(new QuantumCircuit(n));
        List<DeviceEvent> ev = new ArrayList<>();

        ev.add(new DeviceEvent(0, 1, "computing", 0, total, -1, "", zero, null, "Room temperature: control computer sends the circuit"));
        ev.add(new DeviceEvent(L2, 2, "computing", 0, total, -1, "", zero, null, "4K stage: attenuators and filters"));
        ev.add(new DeviceEvent(L3, 3, "computing", 0, total, -1, "", zero, null, "1K stage: thermal and infrared filters"));
        ev.add(new DeviceEvent(L4, 4, "computing", 0, total, -1, "", zero, null, "100 mK stage: amplifiers and superconducting wiring"));
        ev.add(new DeviceEvent(L5, 5, "computing", 0, total, -1, "", zero, null, "10 mK core: qubits ready"));

        QuantumCircuit prefix = new QuantumCircuit(n);
        int t = GATES;
        for (int k = 0; k < total; k++) {
            Operation o = ops.get(k);
            if (o.gate().isTwoQubit()) prefix.addCNOT(o.control(), o.target());
            else prefix.addGate(o.gate(), o.target());
            ev.add(new DeviceEvent(t, 5, "computing", k + 1, total, o.target(), o.gate().name(), probs(prefix), null,
                    "Gate " + (k + 1) + " of " + total + ": " + o.gate().name()));
            t += STEP;
        }
        if (total == 0) t += 1000;

        Map<String, Double> fin = probs(circuit);
        long nonZero = fin.values().stream().filter(v -> v > 1e-9).count();
        ev.add(new DeviceEvent(t, 5, nonZero > 1 ? "superposition" : "measurement", total, total, -1, "", fin, null,
                "All gates done: measuring"));
        t += MEASURE;

        Map<String, Double> collapsed = new LinkedHashMap<>();
        fin.keySet().forEach(k -> collapsed.put(k, k.equals(measured) ? 1.0 : 0.0));
        ev.add(new DeviceEvent(t, 5, "measurement", total, total, -1, "", collapsed, measured, "Result: |" + measured + ">"));
        t += RESULT;
        ev.add(new DeviceEvent(t, 0, "idle", 0, total, -1, "", collapsed, measured, "Idle"));
        return ev;
    }

    private Map<String, Double> probs(QuantumCircuit c) {
        return sim.probabilities(sim.run(c), c.getQubits());
    }
}
