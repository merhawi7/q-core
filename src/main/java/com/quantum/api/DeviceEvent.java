package com.quantum.api;

import java.util.Map;

/**
 * One message for the ESP32. stage: 0 idle, 1 room temperature, 2 4K, 3 1K, 4 100 mK,
 * 5 the 10 mK qubit chip (gates run here, then the result). atMs is the delay after Run is pressed.
 * target is the qubit the current gate acts on, or -1.
 */
public record DeviceEvent(int atMs, int stage, String mode, int step, int steps, int target,
                          String gate, Map<String, Double> p, String measured, String caption) {}
