package org.paluchlab.agentcortex.io;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.ModelConstants;
import org.paluchlab.agentcortex.agents.ActinFilament;
import org.paluchlab.agentcortex.agents.MyosinMotor;
import org.paluchlab.agentcortex.interactions.CrosslinkedFilaments;
import org.paluchlab.agentcortex.interactions.MyosinMotorBinding;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The goal of this class is the write enough information to recreate the simulation at any given time step.
 *
 * Created on 10/13/14.
 */
public class SimulationWriter {
    //Simulation files contain constants, and the length of each actin filament.
    final File simulation;
    //rods contains all of the positions and directions of the myosin motors and actins for each time step.
    final File rods;
    //interactions keeps track of crosslinker binding, and myosin motor bindings.
    final File interactions;
    final static String VERSION="0.1";
    final File directory;
    final static String parameter_tag = "parameters";
    final static String actin_tag = "actin";
    final static String myosin_tag = "myosin";
    final private Object writeLock = new Object();
    private boolean stopped = false;
    private byte[] rodData, interactionData;

    /**
     * Creates a new simulation, using the timestamp + retry to create a unique tag.
     *
     */
    public SimulationWriter(){

        this.directory = new File(".");
        int locked;
        String tag;
        do{
            tag = "" + System.currentTimeMillis();
            locked = createLockFile(tag);

        } while(locked!=0);
        simulation = new File(directory, tag + "-simulation.txt");
        rods = new File(directory, tag + SimulationReader.ROD_TAIL);
        interactions = new File(directory, tag + SimulationReader.INT_TAIL);

    }

    /**
     * Creates a new simulation writer, the writer will only be created if a lockfile with the provided tag does not
     * exist. This is used for possibly restarting simulations, or creating new simulations with a specific tagname.
     *
     * @param tag
     * @param directory
     */
    public SimulationWriter(String tag, File directory){
        this.directory = directory;
        if(createLockFile(tag)!=0){
            System.exit(-1);
        }
        simulation = new File(directory, tag + "-simulation.txt");
        rods = new File(directory, tag + SimulationReader.ROD_TAIL);
        interactions = new File(directory, tag + SimulationReader.INT_TAIL);
    }

    /**
     * Attempts to create the requested lockfile.
     * @param tag file to be create will be $tag.lock
     * @return -1 for failure, 0 for success.
     */
    private int createLockFile(String tag){
        String lock_file = tag + ".lock";

        try(BufferedWriter wb = Files.newBufferedWriter(
                new File(directory, lock_file).toPath(),
                Charset.forName("UTF8"),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.SYNC
        )
        ){

            wb.write(Date.from(Instant.now()).toString());
            wb.write("\n");
            wb.write("Cortex Dynamics Simulation version: ");
            wb.write(VERSION);
        } catch (IOException e) {
            System.out.println("Failed to create lock file");
            return -1;
        }
        return 0;
    }

    /**
     * Writes simulation data, constants and actin lengths, to the simulation file.
     *
     * @param model
     */
    public void writeSimulation(CortexModel model){
        ModelConstants constants = model.constants;
        try(BufferedWriter wb = Files.newBufferedWriter(
                Paths.get(simulation.toURI()),
                Charset.forName("UTF8"),
                StandardOpenOption.SYNC,
                StandardOpenOption.CREATE_NEW
                                                        )
                                                            ){

            wb.write("#Cortex Dynamics Simulation version: ");
            wb.write(VERSION);
            wb.write("\n");
            wb.write(String.format("<%s>\n", parameter_tag));
            wb.write("#Constant\tValue\n");
            Field[] fields =  ModelConstants.class.getDeclaredFields();
            for(Field field: fields){
                String name = field.getName();
                try {

                    String value = field.get(constants).toString();
                    wb.write(String.format("%s\t%s\n", name, value));
                } catch (IllegalAccessException e) {
                    System.out.println("ignoring constant field: " + name);
                    e.printStackTrace();
                }

            }
            wb.write(String.format("</%s>\n", parameter_tag));

            wb.write(String.format("<%s>\n", actin_tag));
            List<ActinFilament> actins = model.getActin();
            for(ActinFilament actin: actins){
                wb.write(String.format("%f\n", actin.length));
            }
            wb.write(String.format("</%s>\n", actin_tag));

        } catch (IOException e) {
            System.out.println("Failed to write simulation file.");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Serializes the rod data into a byte array to be written later.
     *
     * @param model
     */
    private void prepareRods(CortexModel model){

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try(DataOutputStream rodStream = new DataOutputStream(data)){
            //save 4 bytes for the chunk-size.
            rodStream.writeInt(-1);
            rodStream.writeDouble(model.time);
            List<ActinFilament> actins = model.getActin();
            rodStream.writeInt(actins.size());
            for(ActinFilament rod: actins){
                double[] p = rod.position;
                double[] d = rod.direction;
                rodStream.writeDouble(p[0]);
                rodStream.writeDouble(p[1]);
                rodStream.writeDouble(p[2]);

                rodStream.writeDouble(d[0]);
                rodStream.writeDouble(d[1]);
                rodStream.writeDouble(d[2]);

            }
            List<MyosinMotor> motors = model.getMyosins();
            for(MyosinMotor rod: motors){
                double[] p = rod.position;
                double[] d = rod.direction;
                rodStream.writeDouble(p[0]);
                rodStream.writeDouble(p[1]);
                rodStream.writeDouble(p[2]);

                rodStream.writeDouble(d[0]);
                rodStream.writeDouble(d[1]);
                rodStream.writeDouble(d[2]);
            }


            rodData = data.toByteArray();
            int v = rodData.length - 4;
            rodData[0] = (byte)(0xff & (v >> 24));
            rodData[1] = (byte)(0xff & (v >> 16));
            rodData[2] = (byte)(0xff & (v >>    8));
            rodData[3] = (byte)(0xff & v);

        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    /**
     * Writes both the rod and interactions for a time step.
     *
     * @param model
     */
    public void writeTimeStep(CortexModel model){
        prepareInteractions(model);
        prepareRods(model);
        completeTransaction();
    }

    /**
     * Writes interaction data to a byte array.
     *
     * @param model
     */
    private void prepareInteractions(CortexModel model){
        List<MyosinMotorBinding> bindings = model.getMotorBindings();
        List<ActinFilament> actins = model.getActin();
        List<MyosinMotor> motors = model.getMyosins();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try(DataOutputStream interactionStream = new DataOutputStream(data)) {
            interactionStream.writeInt(-1);
            interactionStream.writeDouble(model.time);
            interactionStream.writeInt(bindings.size());
            for(MyosinMotorBinding bind: bindings){
                MyosinMotor m = bind.motor;
                int index = motors.indexOf(m);
                interactionStream.writeInt(index);
                int dex = -1;
                double ctime = 0;
                double ftime = 0;
                double s = 0;
                int head = MyosinMotor.FRONT;
                if(m.isBound(head)){
                    ActinFilament f = m.getBound(head);
                    dex = actins.indexOf(f);
                    s = bind.binding_position[head];
                    ftime = bind.unbind_time[head];
                    ctime = bind.current_time[head];
                }
                interactionStream.writeInt(dex);
                interactionStream.writeDouble(s);
                interactionStream.writeDouble(ctime);
                interactionStream.writeDouble(ftime);

                dex = -1;
                ctime = 0;
                ftime = 0;
                s = 0;
                head = MyosinMotor.BACK;
                if(m.isBound(head)){
                    ActinFilament f = m.getBound(head);
                    dex = actins.indexOf(f);
                    s = bind.binding_position[head];
                    ftime = bind.unbind_time[head];
                    ctime = bind.current_time[head];
                }
                interactionStream.writeInt(dex);
                interactionStream.writeDouble(s);
                interactionStream.writeDouble(ctime);
                interactionStream.writeDouble(ftime);

            }

            List<CrosslinkedFilaments> xlinked = model.getCrosslinkedFilaments();
            interactionStream.writeInt(xlinked.size());
            for(CrosslinkedFilaments linked: xlinked){
                int type = linked.getType();
                interactionStream.writeInt(type);
                switch(type){
                    case CrosslinkedFilaments.NORMAL:
                        int a = actins.indexOf(linked.a);
                        double as = linked.a_s;
                        int b = actins.indexOf(linked.b);
                        double bs = linked.b_s;
                        double t = linked.time;
                        double f = linked.duration;
                        interactionStream.writeInt(a);
                        interactionStream.writeDouble(as);
                        interactionStream.writeInt(b);
                        interactionStream.writeDouble(bs);
                        interactionStream.writeDouble(t);
                        interactionStream.writeDouble(f);
                        break;
                    default:
                        System.out.println("Bad crosslinked filament!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        interactionData = data.toByteArray();
        int v = interactionData.length -4;
        interactionData[0] = (byte)(0xff & (v >> 24));
        interactionData[1] = (byte)(0xff & (v >> 16));
        interactionData[2] = (byte)(0xff & (v >>    8));
        interactionData[3] = (byte)(0xff & v);

    }

    /**
     * Writes the prepared byte arrays for both interactions and rods to their respective files.
     *
     */
    private void completeTransaction(){

        synchronized(writeLock){
            if(!stopped){
                try( OutputStream interactionStream = Files.newOutputStream(
                        interactions.toPath(),
                        StandardOpenOption.SYNC,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.CREATE
                ); OutputStream rodStream = Files.newOutputStream(
                        rods.toPath(),
                        StandardOpenOption.SYNC,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.CREATE
                )){
                    rodStream.write(rodData);
                    interactionStream.write(interactionData);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Waits for completeTransaction to finish, and prevents writing any more interactions.
     */
    public void close(){
        synchronized(writeLock){
            stopped=true;
        }
    }

}