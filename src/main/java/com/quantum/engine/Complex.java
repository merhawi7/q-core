package com.quantum.engine;

/** Minimal immutable complex number (no external math library needed). */
public record Complex(double re, double im) {
    public static final Complex ZERO = new Complex(0, 0);
    public static final Complex ONE = new Complex(1, 0);

    public Complex plus(Complex o) { return new Complex(re + o.re, im + o.im); }
    public Complex times(Complex o) { return new Complex(re * o.re - im * o.im, re * o.im + im * o.re); }
    public Complex conj() { return new Complex(re, -im); }
    public double abs2() { return re * re + im * im; }
}
