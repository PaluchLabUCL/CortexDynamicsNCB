package org.paluchlab.agentcortex.io;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.ModelConstants;
import org.paluchlab.agentcortex.agents.ActinFilament;
import org.paluchlab.agentcortex.agents.MyosinMotor;
import org.paluchlab.agentcortex.agents.Rod;
import org.paluchlab.agentcortex.interactions.CrosslinkedFilaments;
import org.paluchlab.agentcortex.interactions.MyosinMotorBinding;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

/**
 * For reading parameters and restoring simulation data.
 *
 *
 * Created on 10/15/14.
 */
public class SimulationReader implements Iterable<TimePoint>{
    public final static String lock_suffix = ".lock";
    final static int[] heads = {MyosinMotor.FRONT, MyosinMotor.BACK};

    double[] actin_lengths;
    //List<TimePoint> points;
    Map<TimePointKey, SoftReference<TimePoint>> points = new HashMap<>();
    List<TimePointKey> keys = new ArrayList<>();
    public CortexModel model;
    public ModelConstants constants;
    boolean loaded = false;

    final File simulation;
    final File rods;
    final File interactions;
    public String tag;
    private static final int BUFFER_SIZE=512;
    private final static Charset utf8 = Charset.forName("UTF8");
    private final static int EOL = "\n".getBytes(utf8).length;

    final static String INT_TAIL = "-interactions.dat";
    final static String ROD_TAIL = "-rods.dat";

    /**
     * Creates a simulation read that uses the provided files.
     *
     * @param simulation
     * @param rods
     * @param interactions
     */
    public SimulationReader(File simulation, File rods, File interactions){
        this.simulation=simulation;
        this.rods = rods;
        this.interactions=interactions;
    }

    /**
     * Get a specified timepoint.
     *
     * @param i index of the time point.
     * @return
     */
    public TimePoint getTimePoint(int i){

        return loadTimePoint(i);

    }

    /**
     *
     * @return the number of points that were loaded.
     *
     */
    public int getPointCount(){
        return points.size();
    }

    /**
     * loads the simulation data, model constants and actin lengths, from the simulation file.
     *
     */
    public void loadSimulation(){
        List<String> lines = new ArrayList<>();
        try(BufferedReader r = Files.newBufferedReader(simulation.toPath(), Charset.forName("UTF8"))){
            String s;
            while((s=r.readLine())!=null){
                if(s.charAt(0)=='#'||s.length()==0){
                    continue;
                }
                lines.add(s.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        loadSimulation(lines);

    }

    /**
     * Load the simulation from the List of lines. Simulation files are separated into two sections. Parameters and
     * actin filament lengths.
     *
     * @param lines Lines from a simulation file. Tab separated parameters, tag names, or actin lengths...only.
     */
    private void loadSimulation(List<String> lines){
        constants = new ModelConstants();
        List<String> constant_lines = getTagSection(lines, SimulationWriter.parameter_tag);
        int i = 0;
        for(String line: constant_lines){

            String[] pair = line.split(Pattern.quote("\t"));
            i += setConstant(pair, constants);
        }
        List<String> actin_parameters = getTagSection(lines, SimulationWriter.actin_tag);
        actin_lengths = new double[actin_parameters.size()];

        if(actin_parameters.size()!=constants.filaments){
            System.err.println("warning: actin property lines is not the same length as constants.filaments!");
        }

        Iterator<String> siter = actin_parameters.iterator();
        for(i = 0; i<actin_parameters.size();i++){
            actin_lengths[i] = Double.parseDouble(siter.next());
        }

        model = new CortexModel(constants);
        model.number_generator = new Random();
    }

    /**
     * Gets the simulation data between the tags provided. This is *not* xml.
     *
     * @param items original list of values
     * @param tag the tag to be looked for.
     * @return all of the lines between the lines with the opening and closing tags.
     */
    static List<String> getTagSection(List<String> items, String tag){
        int first = items.indexOf(String.format("<%s>",tag));
        int last = items.indexOf(String.format("</%s>", tag));
        return items.subList(first+1, last);
    }

    /**
     * For setting a parameter value based on a key/value pair of strings.
     *
     * @param pair {key, value}
     * @param constants the model object that will be updated with the new value.
     * @return 0 for success 1 for failure
     */
    public static int setConstant(String[] pair, ModelConstants constants){
        int ret = 0;
        try {
            Field f = ModelConstants.class.getDeclaredField(pair[0]);
            if(f.getType()==int.class){
                f.setInt(constants, Integer.parseInt(pair[1]));
            } else if(f.getType()==double.class){
                f.setDouble(constants, Double.parseDouble(pair[1]));
            } else if(f.getType()==boolean.class){
                f.setBoolean(constants, Boolean.parseBoolean(pair[1]));
            } else{
                throw new FieldFormatException(pair[0] + "\t" + pair[1]);
            }
        } catch (NoSuchFieldException e) {
            System.err.println("no such constant: " + pair[0] + "\t" + pair[1]);
            ret = 1;
        } catch (FieldFormatException e){
            System.out.println("undefined field type, skipped: " + pair[0] + "\t" + pair[1]);
            e.printStackTrace();
            ret = 1;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            ret = 1;
        }
        return ret;
    }

    /**
     * Loads the rod and interaction data from their respective files.
     *
     * @return true on success.
     */
    public boolean loadRodsAndInteractions(){
        boolean success = true;
        try(
            BinaryElementsFile rodElements = new BinaryElementsFile(rods.toPath());
            BinaryElementsFile interactionElements = new BinaryElementsFile(interactions.toPath())
        ) {
            boolean working = true;
            while(working){

                byte[] rodData = rodElements.readNextChunk();
                byte[] interactionData = interactionElements.readNextChunk();
                if(rodData==null||interactionData==null){
                    if(rodData!=interactionData){
                        System.err.println("Files do not contain the same number of points!");
                    }
                    //one of the files has come to an end...hopefully both of them.
                    working=false;
                } else{
                    TimePointKey key = new TimePointKey(
                            keys.size(),
                            rodElements.getLastPosition(),
                            rodElements.getLastSize()
                    );


                    key.setInteractionPositions(
                            interactionElements.getLastPosition(),
                            interactionElements.getLastSize()
                    );

                    TimePoint tp = null;
                    try{
                        tp = loadTimePoint(rodData);
                    } catch(Exception e){
                        System.err.println("failed to load rod line at: " + keys.size());
                        e.printStackTrace();
                        continue;
                    }
                    try {
                        loadInteraction(tp, interactionData);
                    } catch (FileFormatException e) {
                        System.err.println("failed to load interaction line at: " + keys.size());
                        e.printStackTrace();
                        continue;
                    } catch (IndexOutOfBoundsException e){
                        System.err.println("failed to load interaction line at: " + keys.size());
                        e.printStackTrace();
                        continue;
                    }

                    keys.add(key);
                    points.put(key, new SoftReference<>(tp));
                    tp.setModel(model);



                }

            }

        } catch (IOException e) {
            e.printStackTrace();
            success = false;
        }

        return success;
    }

    /**
     * Loads interaction data from the provided bytes.
     *
     * @param tp where the interaction data will be stored.
     * @param s binary data with serialized interactions.
     * @throws FileFormatException
     * @throws IOException
     */
    void loadInteraction(TimePoint tp, byte[] s) throws FileFormatException, IOException {
        DataInput input = new DataInputStream(new ByteArrayInputStream(s));
        double t = input.readDouble();
        int count = input.readInt();

        if(count!=constants.motors){
            throw new FileFormatException("an invalid number of motor bindings have been specified");
        }

        if(t!=tp.time){
            throw new FileFormatException("Time point does not match time for interactions.");
        }

        for(int i = 0; i<count; i++){
            MyosinMotor m = tp.motors.get(input.readInt());
            MyosinMotorBinding bind = new MyosinMotorBinding(model, m);

            for(int head: heads){

                int f_dex = input.readInt();
                double pos = input.readDouble();
                double ctime = input.readDouble();
                double ftime = input.readDouble();

                if(f_dex>=0){
                    bind.bind(tp.filaments.get(f_dex),head, pos);
                    bind.current_time[head] = ctime;
                    bind.unbind_time[head] = ftime;
                }
            }
            tp.bindings.add(bind);
        }

        int x_count = input.readInt();
        for(int i = 0; i<x_count; i++){
            int type = input.readInt();
            CrosslinkedFilaments xf = null;
            switch(type){
                case CrosslinkedFilaments.NORMAL:
                    ActinFilament a = tp.filaments.get(input.readInt());
                    double as = input.readDouble();
                    ActinFilament b = tp.filaments.get(input.readInt());
                    double bs = input.readDouble();
                    t = input.readDouble();
                    double f = input.readDouble();
                    xf = new CrosslinkedFilaments(model, a, b,model.createNewCrossLinker(), as, bs, f - t);
                    break;
                default:
                    System.out.println("Bad crosslinked filament!");
            }
            if(xf!=null){
                tp.linkers.add(xf);
            }
        }


    }

    /**
     * Creates a new time point with rod data de-serialized from the chunk of data provided.
     *
     * @param line serialized rod (myosin motors and actin filaments) data.
     *
     * @return a new time point with rod data (positions and directions).
     */
    public TimePoint loadTimePoint(byte[] line) throws IOException {
        DataInput input = new DataInputStream(new ByteArrayInputStream(line));
        int ms = constants.motors;
        int dex = 0;
        TimePoint p = new TimePoint(input.readDouble());
        int as = input.readInt();
        dex++;

        for(int i = 0; i<as; i++){
            ActinFilament f = model.createNewFilament();
            f.length = actin_lengths[i];
            Rod.setPosition(f, new double[]{
                    input.readDouble(),
                    input.readDouble(),
                    input.readDouble(),
            });
            dex = dex+3;
            Rod.setDir(f, new double[]{
                    input.readDouble(),
                    input.readDouble(),
                    input.readDouble(),
            });
            dex = dex+3;
            p.filaments.add(f);
        }
        for(int i = 0; i<ms; i++){
            MyosinMotor m = model.createNewMyosinMotor();
            Rod.setPosition(m, new double[]{
                    input.readDouble(),
                    input.readDouble(),
                    input.readDouble(),
            });
            dex = dex+3;
            Rod.setDir(m, new double[]{
                    input.readDouble(),
                    input.readDouble(),
                    input.readDouble(),
            });
            dex = dex+3;
            p.motors.add(m);
        }
        return p;
    }

    /**
     * Loads the simulation data which prepares the constants and stores the actin length values. Then loads the rod
     * and interaction data.
     *
     * @return whether the rods and interactions were successfully loaded.
     */
    public boolean loadData(){
        boolean success;
        loadSimulation();
        success = loadRodsAndInteractions();

        loaded = true;
        return success;
    }

    /**
     * Uses the loaded time points to measure tension.
     *
     * @return a list of double[]'s where each double[] corresponds to 1 time point.
     */
    public List<double[]> generateTensionMeasurements(){


        List<double[]> measurements = new ArrayList<>();
        for(TimePointKey key: keys){
            TimePoint tp = loadTimePoint(key);
            model.setTimePoint(tp);
            model.measureTension(measurements);
        }


        return measurements;
    }

    public Iterator<TimePoint> iterator(){
        return new Iterator<TimePoint>(){
            int current = 0;
            int last = getPointCount();
            @Override
            public boolean hasNext() {
                return current<last;
            }

            @Override
            public TimePoint next() {

                return getTimePoint(current++);
            }
        };
    }

    /**
     * Loads a simulation assuming the interactions, rods and simulation files are named according to the lock file.
     *
     * @param lockFile
     * @return
     */
    public static SimulationReader fromLockFile(File lockFile){
        int last = lockFile.getName().indexOf(lock_suffix);
        String tag = lockFile.getName().substring(0,last);
        File d = lockFile.getParentFile();
        File interactions = new File(d, tag + INT_TAIL);
        File rods = new File(d, tag + ROD_TAIL);
        File sim = new File(d, tag + "-simulation.txt");
        if(!rods.exists()||!interactions.exists()||!sim.exists()){
            System.err.println(lockFile.getName() + " did not contain the requisite files!");
            return null;
        }
        try {
            SimulationReader simmer = new SimulationReader(sim, rods, interactions);
            simmer.loadData();
            simmer.setTag(tag);
            return simmer;

        } catch(Exception e){
            System.err.println("failed to load lock file: " + lockFile);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Loads a simulation assuming the interactions, rods and simulation files are named accordingly.
     *
     * @param simFile
     * @return
     */
    public static SimulationReader fromSimulationFile(File simFile){
        int last = simFile.getName().indexOf("-simulation.txt");
        String tag = simFile.getName().substring(0, last);
        File d = simFile.getParentFile();
        File interactions = new File(d, tag + INT_TAIL);
        File rods = new File(d, tag + ROD_TAIL);

        if(!rods.exists()||!interactions.exists()||!simFile.exists()){
            System.err.println(simFile.getName() + " did not contain the requisite files!");
            return null;
        }
        try {
            SimulationReader simmer = new SimulationReader(simFile, rods, interactions);
            simmer.loadData();
            simmer.setTag(tag);
            return simmer;

        } catch(Exception e){
            System.err.println("failed to load simulation file: " + simFile);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Initialize a model with constants loaded from the stream.
     *
     * @param model
     * @param stream
     */
    public static void loadConstants(CortexModel model, InputStream stream) throws IOException {
        SimulationReader sim = new SimulationReader(null, null, null);
        BufferedReader buff = new BufferedReader(new InputStreamReader(stream, Charset.forName("UTF8")));
        String s;
        List<String> lines = new ArrayList<>();
        while((s=buff.readLine())!=null){
            if(s.startsWith("#")||s.isEmpty()){
                continue;
            }
            lines.add(s);
        }
        sim.loadSimulation(lines);
        model.constants = sim.constants;
    }

    /**
     * Loads a simulation assuming the interactions, rods and simulation files are named accordingly.
     *
     * @param simFile
     * @return
     */
    public static boolean isValidSimulationFile(File simFile){
        int last = simFile.getName().indexOf("-simulation.txt");
        String tag = simFile.getName().substring(0, last);
        File d = simFile.getParentFile();
        File interactions = new File(d, tag + INT_TAIL);
        File rods = new File(d, tag + ROD_TAIL);

        if(!rods.exists()||!interactions.exists()||!simFile.exists()){
            System.err.println(simFile.getName() + " did not contain the requisite files!");
            return false;
        }

        return true;
    }


    /**
     * For testing loading data.
     *
     * @param args
     */
    public static void main(String[] args){
        File s = new File("simulation.txt");
        File i = new File("interactions.dat");
        File r = new File("rods.dat");
        SimulationReader simmer = new SimulationReader(s,r,i);
        simmer.loadSimulation();
        //simmer.loadRods();
        //simmer.loadInteractions();
        simmer.loadRodsAndInteractions();

        ArrayList<double[]> measurements = new ArrayList<>();

        for(TimePointKey key: simmer.keys){
            TimePoint tp = simmer.loadTimePoint(key);
            simmer.model.setTimePoint(tp);
            simmer.model.measureTension(measurements);
        }

        for(double[] row: measurements){
           for(double d: row){
               System.out.print(d + "\t");
           }
            System.out.println("");
        }

    }

    /**
     *
     * @return Model constants loaded from a simulation file.
     */
    public ModelConstants getConstants() {
        return constants;
    }

    /**
     *
     * @param model
     */
    public void setModel(CortexModel model) {
        this.model = model;
        for(int i = 0; i<getPointCount(); i++){
            TimePoint point = getTimePoint(i);
            for(MyosinMotorBinding binding: point.getBindings()){
                binding.setModel(model);
            }
            for(CrosslinkedFilaments x: point.getLinkers()){
                x.setModel(model);
            }

        }
    }

    /**
     * Go through all the time points and create a list of order measurements for each timepoint.
     *
     * @return List of the data returned form CortexModel#measureOrientation
     */
    public List<double[]> generateOrderMeasurements() {
        List<double[]> l = new ArrayList<>();
        for(TimePointKey key: keys){
            TimePoint p = loadTimePoint(key);
            model.setTimePoint(p);
            model.measureOrientation(l);
        }
        return l;
    }

    /**
     * Goes through all timepoints and measures the average filament positions.
     *
     * @return List of the data returned from CortexModel#measureAverageActinPositions
     */
    public List<double[]> generateAverageFilamentPositions() {
        List<double[]> positions = new ArrayList<double[]>();
        for(TimePointKey key: keys){
            TimePoint p = loadTimePoint(key);
            model.setTimePoint(p);
            positions.add(model.measureAverageActinPositions());
        }
        return positions;
    }

    /**
     * Gets the total number of bound myosin heads for the required time point.
     *
     * @param i index of the required timepoint.
     * @return number of bound myosin heads. (max 2 times the number of myosins).
     */
    public double getBoundMyosinHeadCount(int i) {
        return getTimePoint(i).getMotors().stream().mapToDouble(
                m->(m.isBound(MyosinMotor.FRONT)?1:0) +( m.isBound(MyosinMotor.BACK)?1:0)
        ).sum();

    }

    /**
     * sets the which is used for deciding the the file-names and referencing data.
     * @param t the new tag
     */
    public void setTag(String t){
        tag = t;
    }

    /**
     *
     * @return the tag name, either set or interpreted from the lock/simulation file.
     */
    public String getTag(){
        return tag;
    }

    private TimePoint loadTimePoint(int i){
        TimePointKey key = keys.get(i);


        return loadTimePoint(key);
    }

    /**
     * Retrieves a time point. Timepoints are stored in a map with soft references because a simulation data can take
     * a lot of space. If too much memory is needed, timepoints will be reloaded from the file.
     *
     * @param key
     * @return
     */
    private TimePoint loadTimePoint(TimePointKey key){
        SoftReference<TimePoint> pointRef = points.get(key);
        TimePoint point = pointRef.get();
        if(point==null){
            point = restoreTimePoint(key);
            point.setModel(model);
        }
        return point;
    }

    /**
     * Restore a time point from an already read file. This occurs when a simulation is taking too much memory.
     *
     * @param key indicates file positions and chunch sizes for reloading data.
     *
     * @return the restored time point.
     */
    private TimePoint restoreTimePoint(TimePointKey key){
        System.out.println("restoring time point: " + key.point);

        ByteBuffer rodBytes = ByteBuffer.wrap(new byte[key.rodLength]);
        try(FileChannel rodChannel = FileChannel.open(rods.toPath())){
            int sum = 0;
            rodChannel.position(key.rodPosition);
            while(sum<key.rodLength){
                sum += rodChannel.read(rodBytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        ByteBuffer iaBytes = ByteBuffer.wrap(new byte[key.interactionLength]);

        try(FileChannel iaChannel = FileChannel.open(interactions.toPath())){

            iaChannel.position(key.interactionPosition);
            int sum = 0;
            int calls = 0;
            while(sum<key.interactionLength) {
                sum += iaChannel.read(iaBytes);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        try{
            TimePoint tp = loadTimePoint(rodBytes.array());
            loadInteraction(tp, iaBytes.array());
            points.put(key, new SoftReference<>(tp));
            return tp;
        } catch (IOException | FileFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

}


class FieldFormatException extends Exception{
    FieldFormatException(String message){
        super(message);
    }
}



class FileFormatException extends Exception{
    FileFormatException(String message){ super(message);}
}

/**
 * For storing timepoints in a map, and recovering them if they have been removed due to memory pressure.
 *
 */
class TimePointKey{

    int point;
    long rodPosition, interactionPosition;
    int rodLength, interactionLength;
    public TimePointKey(int point, long rPosition, int rLength){
        rodPosition = rPosition;
        rodLength = rLength;
        this.point = point;
    }

    public void setInteractionPositions(long iPosition, int iLength){
        interactionPosition = iPosition;
        interactionLength = iLength;
    }

}