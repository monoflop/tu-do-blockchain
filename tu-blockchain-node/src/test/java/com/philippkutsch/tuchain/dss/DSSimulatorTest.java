package com.philippkutsch.tuchain.dss;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

@Ignore
public class DSSimulatorTest {
    @Test
    public void testSimulate() throws IOException, InterruptedException {
        File csvOutput = new File("simulation.csv");
        DSSimulator simulator = new DSSimulator(csvOutput, 60, 4, 70, 30);
        simulator.simulate();
    }
}
