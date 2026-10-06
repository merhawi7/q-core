package com.quantum.engine;

/** Gate matrices. CNOT is stored as 4x4 for reference; the simulator applies it by swapping amplitudes. */
public enum QuantumGate {
    H(m(r(1 / Math.sqrt(2)), r(1 / Math.sqrt(2)), r(1 / Math.sqrt(2)), r(-1 / Math.sqrt(2)))),
    X(m(Complex.ZERO, Complex.ONE, Complex.ONE, Complex.ZERO)),
    Y(m(Complex.ZERO, new Complex(0, -1), new Complex(0, 1), Complex.ZERO)),
    Z(m(Complex.ONE, Complex.ZERO, Complex.ZERO, new Complex(-1, 0))),
    S(m(Complex.ONE, Complex.ZERO, Complex.ZERO, new Complex(0, 1))),
    T(m(Complex.ONE, Complex.ZERO, Complex.ZERO, new Complex(Math.cos(Math.PI / 4), Math.sin(Math.PI / 4)))),
    CNOT(new Complex[][] {
            {Complex.ONE, Complex.ZERO, Complex.ZERO, Complex.ZERO},
            {Complex.ZERO, Complex.ONE, Complex.ZERO, Complex.ZERO},
            {Complex.ZERO, Complex.ZERO, Complex.ZERO, Complex.ONE},
            {Complex.ZERO, Complex.ZERO, Complex.ONE, Complex.ZERO}});

    private final Complex[][] matrix;

    QuantumGate(Complex[][] matrix) { this.matrix = matrix; }

    public Complex[][] getMatrix() { return matrix; }
    public boolean isTwoQubit() { return this == CNOT; }

    private static Complex r(double v) { return new Complex(v, 0); }
    private static Complex[][] m(Complex a, Complex b, Complex c, Complex d) { return new Complex[][] {{a, b}, {c, d}}; }
}
