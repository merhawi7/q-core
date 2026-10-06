package com.quantum.engine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * State-vector simulator. Qubit 0 is the LEFTMOST character of a basis label, so |q0 q1>
 * matches how the circuit is drawn (qubit 0 on the top row).
 */
public class QuantumSimulator {

    private final Random random;

    public QuantumSimulator() { this(new Random()); }
    public QuantumSimulator(Random random) { this.random = random; }

    public Complex[] run(QuantumCircuit circuit) {
        int n = circuit.getQubits();
        Complex[] state = new Complex[1 << n];
        java.util.Arrays.fill(state, Complex.ZERO);
        state[0] = Complex.ONE; // start in |00...0>
        for (Operation op : circuit.getOperations()) {
            state = op.gate().isTwoQubit() ? cnot(state, n, op.control(), op.target()) : single(state, n, op);
        }
        return state;
    }

    private static Complex[] single(Complex[] s, int n, Operation op) {
        Complex[][] m = op.gate().getMatrix();
        int mask = 1 << (n - 1 - op.target());
        Complex[] out = s.clone();
        for (int i = 0; i < s.length; i++) {
            if ((i & mask) != 0) continue;
            Complex a = s[i], b = s[i | mask];
            out[i] = m[0][0].times(a).plus(m[0][1].times(b));
            out[i | mask] = m[1][0].times(a).plus(m[1][1].times(b));
        }
        return out;
    }

    private static Complex[] cnot(Complex[] s, int n, int control, int target) {
        int cm = 1 << (n - 1 - control), tm = 1 << (n - 1 - target);
        Complex[] out = s.clone();
        for (int i = 0; i < s.length; i++) {
            if ((i & cm) != 0 && (i & tm) == 0) {
                out[i] = s[i | tm];
                out[i | tm] = s[i];
            }
        }
        return out;
    }

    /** Probability of each basis state, keyed by label such as "00", "01". */
    public Map<String, Double> probabilities(Complex[] state, int n) {
        Map<String, Double> p = new LinkedHashMap<>();
        for (int i = 0; i < state.length; i++) {
            String label = String.format("%" + n + "s", Integer.toBinaryString(i)).replace(' ', '0');
            p.put(label, state[i].abs2());
        }
        return p;
    }

    /** Collapses the state: samples one outcome using the probabilities. */
    public String measure(Complex[] state, int n) {
        double r = random.nextDouble(), cum = 0;
        Map<String, Double> p = probabilities(state, n);
        String last = null;
        for (var e : p.entrySet()) {
            last = e.getKey();
            cum += e.getValue();
            if (r < cum) return e.getKey();
        }
        return last;
    }

    /** Bloch vector {x, y, z} of qubit 0 (reduced state). Length < 1 means the qubit is entangled. */
    public double[] bloch(Complex[] state) {
        int half = state.length / 2;
        double r00 = 0, r11 = 0;
        Complex r01 = Complex.ZERO;
        for (int k = 0; k < half; k++) {
            Complex a = state[k], b = state[half + k];
            r00 += a.abs2();
            r11 += b.abs2();
            r01 = r01.plus(a.times(b.conj()));
        }
        return new double[] {2 * r01.re(), -2 * r01.im(), r00 - r11};
    }
}
