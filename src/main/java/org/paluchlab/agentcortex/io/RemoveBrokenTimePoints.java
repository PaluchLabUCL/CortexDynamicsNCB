package org.paluchlab.agentcortex.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * For removing time points that did not get written completely before a running simulation was terminated.
 *
 *
 * Created on 4/27/15.
 */
public class RemoveBrokenTimePoints {

    enum ScanMode{
        doNothing, modifyEnd
    }

    public static ScanMode mode;


    public static void main(String[] args) throws IOException {
        if(args.length<2){
            System.out.println("you must supply a mode and simulation.txt file eg:");
            System.out.println("java -jar RemoveBrokenTimePoints.jar 0 tag-simulation.txt");
            System.out.println("this sets the scan mode to be 0.");
            System.exit(0);
        }
        try {
            switch (Integer.parseInt(args[0])) {
                case 0:
                    mode = ScanMode.doNothing;
                    break;
                case 1:
                    mode = ScanMode.modifyEnd;
                    break;

            }
        } catch(NumberFormatException exc){
            System.out.println("the first argument must be the scan mode!");
            System.exit(-1);
        }

        //for logging results.
        BufferedWriter writer = Files.newBufferedWriter(Paths.get("unsuccessful.txt"), StandardOpenOption.CREATE);
        //SimulationReader r = SimulationReader.fromLockFile(new File(args[0]));
        for(int i = 1; i<args.length; i++){
            LoadResult r = checkSimulationFile(args[i]);
            if(!r.success){
                writer.write(args[i]);
                for(String s: r.messages){
                    writer.write('\t' + s);
                }
                writer.write('\n');
            }
        }
        writer.close();
    }


    /**
     * Loads a simulation and uses the scan mode to determine if it will just check the quality of the files, or also
     * attempt to repair the simulation.
     *
     * The principle form of repair is to just truncate both the interaction file and rod file so they are the same
     * length, with matching time points.
     *
     * @param simName
     * @return
     */
    public static LoadResult checkSimulationFile(String simName){
        System.out.println("\n\n==========" + simName + "==========");
        File simFile = new File(simName);
        int last = simFile.getName().indexOf("-simulation.txt");
        String tag = simFile.getName().substring(0,last);
        File d = simFile.getParentFile();
        File interactions = new File(d, tag + "-interactions.dat");
        File rods = new File(d, tag + "-rods.dat");
        LoadResult result = new LoadResult();
        if(!rods.exists()||!interactions.exists()||!simFile.exists()){
            result.fail(simFile.getName() + " did not contain the requisite files!");
            return result;
        }
        try {
            SimulationReader simmer = new SimulationReader(simFile, rods, interactions);
            simmer.loadSimulation();

            int good = 0;
            try(
                    BinaryElementsFile rodElements = new BinaryElementsFile(rods.toPath(), true);
                    BinaryElementsFile interactionElements = new BinaryElementsFile(interactions.toPath(), true)
            ) {
                boolean working = true;
                while(working){

                    byte[] rodLine  = rodElements.readNextChunk();
                    byte[] interactionLine  = interactionElements.readNextChunk();
                    if(rodLine==null||interactionLine==null){
                        if(rodLine!=interactionLine){
                            result.fail("failed: Files do not contain the same number of lines!");
                            switch(mode){
                                case modifyEnd:
                                    if(interactionLine==null){
                                        rodElements.removeLastLine();
                                    } else {
                                        result.fail("rod file is too short. data files corrupt");
                                    }
                                    break;
                                case doNothing:

                            }
                        }
                        //one of the files has come to an end...hopefully both of them.
                        working=false;
                    } else{

                        TimePoint tp = null;
                        try{
                            tp = simmer.loadTimePoint(rodLine);
                        } catch(Exception e){
                            result.fail("failed to load rod line at: " + good);
                            continue;
                        }
                        try {
                            simmer.loadInteraction(tp, interactionLine);
                            good++;
                        } catch (Exception e) {
                            result.fail("failed to load interaction line at: " + good);
                            switch(mode){
                                case modifyEnd:
                                    rodElements.removeLastLine();
                                    interactionElements.removeLastLine();
                                    break;
                                case doNothing:
                            }
                            continue;
                        }

                    }

                }
                System.out.println("good: " + good);

            } catch (IOException e) {
                result.fail("broken");
                e.printStackTrace();
                return result;
            }
            return result;

        } catch(Exception e){
            result.fail("failed to load simulation file: " + simFile);
            e.printStackTrace();
        }
        return result;
    }

    /**
     * for storing messages, and the general success state of the operation.
     */
    static class LoadResult{
        List<String> messages = new ArrayList<String>();
        boolean success = true;
        public void fail(String message){
            System.out.println("*" + message);
            messages.add(message);
            success=false;
        }


    }
}
