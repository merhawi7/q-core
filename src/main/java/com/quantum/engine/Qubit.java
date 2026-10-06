package com.quantum.engine;

/** A single qubit a|0> + b|1> with real amplitudes (see QuantumSimulator for full complex state vectors). */
public class Qubit {
    private double alpha; // |0> amplitude
    private double beta;  // |1> amplitude

    public Qubit(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
        normalize();
    }

    public void normalize() {
        double norm = Math.sqrt(alpha * alpha + beta * beta);
        if (norm == 0) throw new IllegalArgumentException("zero vector is not a valid qubit");
        alpha /= norm;
        beta /= norm;
    }

    public double getAlpha() { return alpha; }
    public double getBeta() { return beta; }

    @Override
    public String toString() { return String.format("|\u03c8> = %.3f|0> + %.3f|1>", alpha, beta); }
}
