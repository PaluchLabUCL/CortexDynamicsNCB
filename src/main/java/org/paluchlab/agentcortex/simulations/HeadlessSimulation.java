package org.paluchlab.agentcortex.simulations;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.io.SimulationReader;
import org.paluchlab.agentcortex.io.SimulationWriter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * For running a simulation without using a graphical display. The current time will be used to name the files for
 * writing the data, and they will be written in the current working directory.
 *
 * Created on 1/12/16.
 */
public class HeadlessSimulation {

    /**
     * Entry point for headless simulation. If an argument is passed the first argument will be used as a parameter file
     * to start the simulation with a specified set of parameters instead of the default values found in
     * @param args
     */
    public static void main(String[] args){

        CortexModel m =  new CortexModel(true);
        if(args.length>0){
            try {
                Files.lines(
                        Paths.get(args[0]),
                        Charset.forName("UTF8")
                ).filter(
                        s->!s.startsWith("#")
                ).forEach(
                        s-> SimulationReader.setConstant(s.split("\\s"), m.constants)
                );

            } catch (IOException e) {
                System.err.println("parameters were not successfully loaded!");
                e.printStackTrace();
            }
        }

        final SimulationWriter writer = new SimulationWriter();
        m.initializeSimulation();

        writer.writeSimulation(m);
        writer.writeTimeStep(m);
        Runtime r = Runtime.getRuntime();
        r.addShutdownHook(new Thread(){
            @Override
            public void run(){
                System.out.println("shutting down");
                writer.close();
                System.out.println("successfully closed");
            }
        });

        for(int i = 0; i<m.constants.STEPS_PER_SIMULATE; i++){
            m.stepSimulation();
            writer.writeTimeStep(m);
        }

        System.exit(0);
    }
}
