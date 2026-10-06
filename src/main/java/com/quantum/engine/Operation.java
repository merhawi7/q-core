package com.quantum.engine;

/** One gate placed on the circuit. control is -1 for single-qubit gates. */
public record Operation(QuantumGate gate, int target, int control) {}
