package com.quantum.engine;

import java.util.ArrayList;
import java.util.List;

public class QuantumCircuit {
    public static final int MAX_QUBITS = 5;
    private final List<Operation> operations = new ArrayList<>();
    private final int qubits;

    public QuantumCircuit(int qubits) {
        if (qubits < 1 || qubits > MAX_QUBITS) throw new IllegalArgumentException("qubits must be 1.." + MAX_QUBITS);
        this.qubits = qubits;
    }

    public void addGate(QuantumGate gate, int target) {
        if (gate.isTwoQubit()) throw new IllegalArgumentException("use addCNOT for CNOT");
        check(target);
        operations.add(new Operation(gate, target, -1));
    }

    public void addCNOT(int control, int target) {
        check(control);
        check(target);
        if (control == target) throw new IllegalArgumentException("control and target must differ");
        operations.add(new Operation(QuantumGate.CNOT, target, control));
    }

    private void check(int q) {
        if (q < 0 || q >= qubits) throw new IllegalArgumentException("qubit index out of range: " + q);
    }

    public int getQubits() { return qubits; }
    public List<Operation> getOperations() { return List.copyOf(operations); }
}
