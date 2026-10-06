package com.quantum.api;

import com.quantum.engine.Complex;
import com.quantum.engine.QuantumCircuit;
import com.quantum.engine.QuantumGate;
import com.quantum.engine.QuantumSimulator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/circuit")
@CrossOrigin(origins = "http://localhost:5173") // React dev server, if you add one
public class CircuitController {

    public record OpDto(String gate, int target, Integer control) {}
    public record RunRequest(int qubits, List<OpDto> ops) {}
    public record RunResponse(Map<String, Double> probabilities, String measured, double[] bloch,
                              String mode, boolean deviceEnabled) {}

    private final QuantumSimulator simulator = new QuantumSimulator();
    private final SequencePlanner planner = new SequencePlanner(simulator);
    private final DeviceClient device;

    public CircuitController(DeviceClient device) { this.device = device; }

    @PostMapping("/run")
    public RunResponse run(@RequestBody RunRequest req) {
        QuantumCircuit circuit = new QuantumCircuit(req.qubits());
        for (OpDto op : req.ops() == null ? List.<OpDto>of() : req.ops()) {
            QuantumGate g = QuantumGate.valueOf(op.gate().toUpperCase());
            if (g.isTwoQubit()) circuit.addCNOT(op.control() == null ? -1 : op.control(), op.target());
            else circuit.addGate(g, op.target());
        }
        Complex[] state = simulator.run(circuit);
        Map<String, Double> p = simulator.probabilities(state, req.qubits());
        String measured = simulator.measure(state, req.qubits());
        long nonZero = p.values().stream().filter(v -> v > 1e-9).count();

        device.play(planner.plan(circuit, measured)); // the device now acts out the 5 parts, in order
        return new RunResponse(p, measured, simulator.bloch(state), nonZero > 1 ? "superposition" : "measurement",
                device.enabled());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> bad(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
