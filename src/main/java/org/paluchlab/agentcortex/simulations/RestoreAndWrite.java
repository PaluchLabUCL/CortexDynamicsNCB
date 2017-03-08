package org.paluchlab.agentcortex.simulations;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.io.SimulationReader;
import org.paluchlab.agentcortex.io.SimulationWriter;

import java.io.File;

/**
 * Restores a previously saved simulation, and begins simulating from where it left off.
 *
 * Created on 1/21/15.
 */
public class RestoreAndWrite {
    public static void main(String[] args){
        if(args.length<1){
            System.out.println("you must supply a simulation.txt file eg:");
            System.out.println("RestoreAndWrite tag-simulation.txt");
            System.exit(0);
        }
        //SimulationReader r = SimulationReader.fromLockFile(new File(args[0]));
        File simFile = new File(args[0]);
        SimulationReader r = SimulationReader.fromSimulationFile(simFile);
        CortexModel m = r.model;
        m.setTimePoint(r.getTimePoint(r.getPointCount()-1));

        SimulationWriter writer = new SimulationWriter(r.getTag(), simFile.getParentFile());

        for(int i = 0; i<m.constants.STEPS_PER_SIMULATE; i++){
            m.stepSimulation();
            writer.writeTimeStep(m);
        }
        System.exit(0);
    }
}
