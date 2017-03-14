package org.paluchlab.agentcortex;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

import org.paluchlab.agentcortex.agents.*;
import org.paluchlab.agentcortex.display.GraphMachine;
import org.paluchlab.agentcortex.display.JavaScriptTerminal;
import org.paluchlab.agentcortex.geometry.Line3D;
import org.paluchlab.agentcortex.integrators.*;
import org.paluchlab.agentcortex.interactions.CrosslinkedFilaments;
import org.paluchlab.agentcortex.interactions.MyosinMotorBinding;
import org.paluchlab.agentcortex.io.SimulationWriter;
import org.paluchlab.agentcortex.io.TimePoint;

/**
 * Agent based 3D cortex model, keeps track of all the data. Provides an interface
 * to perform analysis.
 *
 * Created on 4/28/14.
 */
public class CortexModel {
    File log_file = new File("development-log.txt");
    public boolean GRAPHING = true;
    public GraphMachine graphMachine;
    public ModelConstants constants;
    protected List<ActinFilament> actins = new ArrayList<>();
    protected List<MyosinMotor> myosins = new ArrayList<>();
    protected List<Crosslinker> linkers = new ArrayList<>();

    protected List<CrosslinkedFilaments> xlinked = new ArrayList<>();
    protected List<MyosinMotorBinding> bindings = new ArrayList<>();
    
    public Random number_generator = new Random();
    public double time;
    JavaScriptTerminal terminal;


    protected double max_out_of_eq = 0;
    ForcePreparation prepare;

    protected Integrator integrator;

    protected CortexModel(){

    }

    /**
     * Starts the model to be used for simulations.
     *
     * @param headless true do not include graphics. false include graphics.
     */
    public CortexModel(boolean headless){
        constants = new ModelConstants();

        if(headless){
            GRAPHING=false;
        }
        if(!headless) {
            terminal = new JavaScriptTerminal(this);
            graphMachine = new GraphMachine();
        }


    }

    /**
     * Starts a headless version of the cortex model, that uses the provided constants.
     *
     * @param constants
     */
    public CortexModel(ModelConstants constants){
        setConstants(constants);
    }



    /**
     * Sets the model constants, and sets the force preparation mechanism to non-steric forces.
     *
     * @param constants
     */
    public void setConstants(ModelConstants constants){
        this.constants = constants;
        prepare = this::prepareNonStericForces;
    }

    /**
     * Sets the simulation to the timepoint that has been loaded from a file.
     *
     * @param tp timepoint containing actins, myosins, crosslinker bindings
     *           myosin motor bindings.
     */
    public void setTimePoint(TimePoint tp){
        actins.clear();
        actins.addAll(tp.getFilaments());
        myosins.clear();
        myosins.addAll(tp.getMotors());
        bindings.clear();
        bindings.addAll(tp.getBindings());
        xlinked.clear();
        xlinked.addAll(tp.getLinkers());
        linkers.clear();
        xlinked.forEach((w) -> linkers.add(w.getLink()));
        time  = tp.getTime();
        initializeIntegrator();
        number_generator = new Random();
    }



    /**
     * Creates a new adaptive integrator, and sets the agents and model.
     *
     */
    public void initializeIntegrator(){
        integrator = new AdaptiveEuler();
        integrator.setModel(this);
        integrator.setActins(actins);
        integrator.setMyosins(myosins);

    }


    /**
     *  Creates a double[] and adds it to the provided collection the tension values are unscaled.
     *  The values in the array are:
     *
     *  time, filament tension y-z, actin only y-z, filament tension x-y, actin only x-y, myosin_dipole, bind count, double bound, dXX, dYY, dZZ
     *  0     1                     2               3                     4               5              6           7             8    9    10
     *
     * @param output a collection for storing the *unscaled* results.
     */
    public void measureTension(Collection<double[]> output){
        prepareForces();
        double[] pos = new double[]{0,0,0};
        double[] x_dir = new double[]{1, 0, 0};
        double[] y_dir = new double[]{0, 1, 0};
        double[] yz = sliceNetwork(pos, x_dir);
        double[] xz = sliceNetwork(pos, y_dir);
        double[] ayz = sliceActinFilaments(pos, x_dir);
        double[] axz = sliceActinFilaments(pos, y_dir);
        double bind_count = 0;
        double double_bound = 0;
        double myosin_dipole = 0;
        double dipoleXX = 0;
        double dipoleYY = 0;
        double dipoleZZ = 0;
        for(MyosinMotorBinding binding: bindings){
            MyosinMotor motor = binding.motor;
            boolean f = false;
            if(motor.isBound(MyosinMotor.FRONT)){
                bind_count++;
                f = true;
            }
            if(motor.isBound(MyosinMotor.BACK)){
                bind_count++;
                if(f){
                    myosin_dipole += binding.getForceDipole();
                    double[] dd = binding.getDirectionalForceDipole();
                    dipoleXX += dd[0]*dd[0];
                    dipoleYY += dd[1]*dd[1];
                    dipoleZZ += dd[2]*dd[2];

                    double_bound++;
                }
            }
        }

        double_bound = double_bound==0?1:double_bound;

        double[] op = new double[]{
                time,
                0.5*(yz[0] + yz[1]),
                0.5*(ayz[0] + ayz[1]),
                0.5*(xz[0] + xz[1]),
                0.5*(axz[0] + axz[1]),
                myosin_dipole/double_bound,
                bind_count,
                double_bound,
                dipoleXX/double_bound,
                dipoleYY/double_bound,
                dipoleZZ/double_bound
        };
        output.add(op);
        clearForces();
    }

    /**
     *  Measures the tension in the components by slicing the filaments using perpendicular planes. Adds
     *  the results to the collection provided.
     *
     *  The result array is as follows:
     *
     *  time, actin y-z, actin x-z, myosin y-z, myosin x-z, xlinks y-z, xlinks x-z
     *  0     1               2         3           4               5        6
     *
     * @param output collectino where the *unscaled* results are returned.
     */
    public void measureTensionByParts(Collection<double[]> output){
        prepareForces();
        double[] pos = new double[]{0,0,0};
        double[] x_dir = new double[]{1, 0, 0};
        double[] y_dir = new double[]{0, 1, 0};
        double[] ayz = sliceActinFilaments(pos, x_dir);
        double[] axz = sliceActinFilaments(pos, y_dir);
        double[] myz = sliceMyosinMotors(pos, x_dir);
        double[] mxz = sliceMyosinMotors(pos, y_dir);
        double[] xyz = sliceCrossLinkers(pos, x_dir);
        double[] xxz = sliceCrossLinkers(pos, y_dir);
        double[] dipoles = totalMyosinDipole();

        double[] op = new double[]{
                time,
                0.5*(ayz[0] + ayz[1]),
                0.5*(axz[0] + axz[1]),
                0.5*(myz[0] + myz[1]),
                0.5*(mxz[0] + mxz[1]),
                0.5*(xyz[0] + xyz[1]),
                0.5*(xxz[0] + xxz[1]),
                dipoles[0], dipoles[1], dipoles[2]
        };
        output.add(op);
        clearForces();
    }

    /**
     * Measures the tension in bound myosin motors at the center.
     *
     * @return xyz components of the tension.
     */
    public double[] totalMyosinDipole(){
        double[] ret = new double[] {0,0,0};
        for(MyosinMotorBinding bind: bindings){
            MyosinMotor m = bind.motor;
            if(!(m.isFree(MyosinMotor.FRONT)||m.isFree(MyosinMotor.BACK))){
                List<double[]> fb = m.getTension(0);
                double[] a = fb.get(0);
                double[] b = fb.get(1);
                ret[0] += 0.5*(a[0] + b[0]);
                ret[1] += 0.5*(a[1] + b[1]);
                ret[2] += 0.5*(a[2] + b[2]);
            }


        }
        return ret;
    }

    /**
     * calculate p, average direction of filaments. and Q's &lt;n dot n&gt;.
     *
     * @param output The output data should be stored. Array will
     *               be in the order:
     *               time, px, py, pz, qxx, qxy, qxz, qyx, qyy, qyz, qzx, qzy, qzz, mqxx, mqyy, mqzz
     *               0     1   2   3   4    5    6    7    8    9    10   11   12   13    14    15
     */
    public void measureOrientation(Collection<double[]> output){
        double[] p = new double[3];
        double[] Q = new double[9];
        for(ActinFilament f: actins){
            double[] dir = f.direction;
            for(int i = 0; i<3; i++){
                p[i] += dir[i];
                for(int j = 0; j<3; j++){
                    Q[i + 3*j] += dir[i]*dir[j] + (i==j?-1.0/3.0:0.0);
                }
            }

        }

        double[] MQ = new double[9];
        for(Rod m: myosins){
            double[] dir = m.direction;
            for(int i = 0; i<3; i++){
                for(int j = 0; j<3; j++){
                    MQ[i + 3*j] += dir[i]*dir[j] + (i==j?-1.0/3.0:0.0);
                }
            }
        }

        for(int i = 0; i<3; i++){
            p[i] = p[i]/actins.size();
        }
        for(int i = 0; i<9; i++){
            Q[i] = Q[i]/actins.size();
            MQ[i] = MQ[i]/myosins.size();
        }

        double[] results = new double[]{
                time,
                p[0],p[1],p[2],
                Q[0],Q[1],Q[2],
                Q[3],Q[4],Q[5],
                Q[6],Q[7],Q[8],
                MQ[0], MQ[4], MQ[8]

        };
        output.add(results);

    }

    /**
     * Uses the graph machine to plot the angle distribution.
     *
     */
    void plotAngleDistribution(){
        List<double[]> distributions = measureActinFilamentOrientation();
        graphMachine.plotAngleDistribution(distributions);
    }


    /**
     * Calculates the angle actin filaments make with the z-plane, and the angle they make
     * with the x-axis in the z plane. Creates a histogram
     *
     * @return
     */
    List<double[]> measureActinFilamentOrientation(){
        final int ORIENTATION_BINS = 20;
        //the range goes from -1 to 1.
        double delta = ORIENTATION_BINS/Math.PI;
        double xy_delta = ORIENTATION_BINS/(2*Math.PI);

        double[] angle = new double[ORIENTATION_BINS];
        double[] xy_angle = new double[ORIENTATION_BINS];
        double[] count = new double[ORIENTATION_BINS];
        double[] xy_count = new double[ORIENTATION_BINS];

        double tics = 0;
        for(ActinFilament f: actins){
            int dex = (int)((Math.acos(f.direction[2]))*delta);
            dex=dex==ORIENTATION_BINS?ORIENTATION_BINS-1:dex;
            count[dex] += 1;

            if(f.direction[2]!=1 || f.direction[2]!=-1){
                tics++;
                double theta = Math.atan2(f.direction[0], f.direction[1]);
                int  xy_dex = (int)((theta + Math.PI )*xy_delta);
                xy_dex = xy_dex==ORIENTATION_BINS?xy_dex-1:xy_dex;
                xy_count[xy_dex] += 1;
            }


        }

        for(int i = 0; i<ORIENTATION_BINS; i++){
            angle[i] = ((i + 0.5)/delta);
            xy_angle[i] = ((i + 0.5)/xy_delta - Math.PI);
            count[i] = count[i]/actins.size();
            xy_count[i] = xy_count[i]/tics;
        }

        List<double[]> ret = new ArrayList<double[]>();
        ret.add(angle);
        ret.add(count);
        ret.add(xy_angle);
        ret.add(xy_count);
        return ret;


    }

    /**
     * Measures the surface tension for graphing while the simulation runs. The tension value is scaled.
     *
     */
    void measureTension(){
        if(actins.size()==0) return;
        prepareForces();

        double myosin_dot = 0;
        for(MyosinMotor motor: myosins){
            myosin_dot += motor.getFilamentDot();
        }

        double[] pos = new double[]{0,0,0};
        double[] x_dir = new double[]{1, 0, 0};
        double[] y_dir = new double[]{0, 1, 0};
        double[] yz = sliceNetwork(pos, x_dir);
        double[] xz = sliceNetwork(pos, y_dir);
        double s0 = 1/(constants.WIDTH*getT0());
        graphMachine.appendTensionPoint(0, time, yz[0]*s0);


        graphMachine.appendTensionPoint(1, time, yz[1]*s0);
        graphMachine.appendTensionPoint(2, time, xz[0]*s0);
        graphMachine.appendTensionPoint(3, time, xz[1]*s0);
        //graphMachine.appendTensionPoint(4, time, myosin_dot);
        graphMachine.refreshTensionPlot();
        clearForces();
    }

    /**
     * clear the forces on all of the rods.
     */
    public void clearForces(){
        for(ActinFilament actin: actins){
            actin.clearForces();
        }
        for(MyosinMotor motor: myosins){
            motor.clearForces();
        }
    }

    /**
     * Plots a graph of the myosin distribution along the z axis.
     *
     */
    void measureMyosinZDistribution(){
        double[] zvalues = new double[myosins.size()*2];
        for(int i = 0; i<myosins.size(); i++){

            MyosinMotor myosin = myosins.get(i);
            double[] head = myosin.getPoint(myosin.length*0.5);
            double[] tail = myosin.getPoint(-myosin.length*0.5);
            zvalues[2*i] = head[2];
            zvalues[2*i+1] = tail[2];

        }
        Arrays.sort(zvalues);
        double[] counts = new double[zvalues.length];
        for(int i = 0; i<zvalues.length; i++){
            counts[i] = i+1;
        }
        graphMachine.addMyosinDistribution(zvalues, counts, time);
    }

    /**
     * Slices the actin network with a plane at a defined position and normal, and measures the tension. Returns an
     * unscaled value.
     *
     * @param pos position of slicing plane
     * @param dir normal of slicing plane.
     * @return an array of length 2 the first value is the forward direction and the second value is the
     *         backwards direction.
     */
    public double[] sliceActinFilaments(double[] pos, double[] dir){
        double[] forward_backwards = new double[2];

        for(ActinFilament filly: actins){
            double[] center = getReflectedPoint(pos, filly.position);
            double[] direction = filly.direction;
            double location = Line3D.intersection(center, direction, pos, dir);
            if(2*Math.abs(location)>filly.length){
                continue;
            }


            //List<double[]> back_front = filly.getTension(location);
            List<double[]> back_front = filly.internalForce(location);
            double dot = Line3D.dot(dir, filly.direction);

            if(dot>0){
                //facing the same direction.
                forward_backwards[0] +=  Line3D.dot(dir, back_front.get(1));
                forward_backwards[1] +=  -Line3D.dot(dir, back_front.get(0));



            } else{
                //facing the opposite direction.
                forward_backwards[0] +=  Line3D.dot(dir, back_front.get(0));
                forward_backwards[1] +=  -Line3D.dot(dir, back_front.get(1));

                //facing the opposite direction.

            }

        }

        return forward_backwards;
    }

    /**
     * Slices the myosin motors with the provided plane. The motors and/or bindings can be sliced.
     *
     * @param pos
     * @param dir
     * @return
     */
    public double[] sliceMyosinMotors(double[] pos, double[] dir){
        double[] forward_backwards = new double[2];

        for(MyosinMotorBinding bind: bindings){
            MyosinMotor motor = bind.motor;
            if(motor.isBound(MyosinMotor.FRONT)){
                double[] a = motor.getPoint(motor.length*0.5);
                double[] b = motor.getBound(MyosinMotor.FRONT).getPoint(bind.binding_position[MyosinMotor.FRONT]);
                double f = sliceSpring(a, b, constants.MYOSIN_BIND_LENGTH, pos, dir);
                forward_backwards[0] += motor.K_m*f;
                forward_backwards[1] += motor.K_m*f;
            }

            if(motor.isBound(MyosinMotor.BACK)){
                double[] a = motor.getPoint(-motor.length*0.5);
                double[] b = motor.getBound(MyosinMotor.BACK).getPoint(bind.binding_position[MyosinMotor.BACK]);
                double f = sliceSpring(a, b, constants.MYOSIN_BIND_LENGTH, pos, dir);
                forward_backwards[0] += motor.K_m*f;
                forward_backwards[1] += motor.K_m*f;

            }

            double[] center = motor.position;
            double[] direction = motor.direction;
            double location = Line3D.intersection(center, direction, getReflectedPoint(center, pos), dir);
            if(Math.abs(location)>0.5*motor.length){
                continue;
            }

            List<double[]> back_front = motor.internalForce(location);

            double dot = Line3D.dot(dir, motor.direction);
            if(dot>0){
                //facing the same direction.
                forward_backwards[0] +=  Line3D.dot(dir, back_front.get(1));
                forward_backwards[1] +=  -Line3D.dot(dir, back_front.get(0));

            } else{
                //facing the opposite direction.
                forward_backwards[0] +=  Line3D.dot(dir, back_front.get(0));
                forward_backwards[1] +=  -Line3D.dot(dir, back_front.get(1));
            }

        }
        return forward_backwards;
    }

    /**
     * Returns the cumulative force of all the crosslinkers sliced by the provided plane.
     *
     * @param pos
     * @param dir
     * @return
     */
    public double[] sliceCrossLinkers(double[] pos, double[] dir){
        double[] forward_backwards = new double[2];


        for(Crosslinker c: linkers){
            double f = sliceSpring(c.A, c.B, c.length, pos, dir);

            forward_backwards[0] += f*c.K_x;
            forward_backwards[1] += f*c.K_x;


        }

        return forward_backwards;
    }

    /**
     * Slices all filaments, myosin, and crosslinkers. returns unscaled values.
     *
     * @param pos position of plane
     * @param dir normal to plane
     * @return { forward, backward } unscaled tension values.
     */
    public double[] sliceNetwork(double[] pos, double[] dir){
        double[] aT = sliceActinFilaments(pos, dir);
        double[] mT = sliceMyosinMotors(pos, dir);
        double[] cT = sliceCrossLinkers(pos, dir);
        double[] forward_backwards = new double[]{
                aT[0] + mT[0] + cT[0],
                aT[1] + mT[1] + cT[1]
        };

        return forward_backwards;
    }

    /**
     *  generic method used for slicing a spring, (eg myosin motor connection or crosslinker). Magnitude of force dotted
     *  along the direction of the slicing plane.
     *
     * @param a first point
     * @param b second point
     * @param rest_length
     * @param pos point on plane.
     * @param normal direction of plane.
     * @return positive if the spring is stretched, and negative if it is compressed.
     */
    public double sliceSpring(double[] a, double[] b, double rest_length, double[] pos, double[] normal){
        double[] O = getReflectedPoint(a, b);
        double dx = a[0] - O[0];
        double dy = a[1] - O[1];
        double dz = a[2] - O[2];

        double l = Math.sqrt(dx*dx + dy*dy + dz*dz);
        double[] center = new double[3];

        double[] direction = new double[]{dx/l, dy/l, dz/l};

        for(int i = 0; i<3; i++){
            center[i] = 0.5*(a[i] + O[i]);
        }

        double[] close_pos = getReflectedPoint(center, pos);

        double location = Line3D.intersection(center, direction, close_pos, normal);
        double force_magnitude = 0;
        if(Math.abs(location)<0.5*l){
            double dot = Math.abs(Line3D.dot(direction, normal));
            force_magnitude = (l - rest_length)*dot;

        }

        return force_magnitude;
    }

    /**
     *
     * Clears all agents, creates a new random number generator and refreshes the graphs.
     *
     */
    public void clearHistory(){
        actins.clear();
        myosins.clear();
        linkers.clear();
        xlinked.clear();
        bindings.clear();

        if(GRAPHING) {
            refreshPlots();
        }
        number_generator = new Random();
        time = 0;
    }

    /**
     * Refreshes plots, see GraphMachine
     */
    public void refreshPlots(){
        graphMachine.refreshPlots();
    }

    /**
     *
     * Seeds actin filaments in the box where the angles are only restricted by the model constant ANGLE_SIGMA.
     *
     */
    public void seedActinFilamentsFreeEdge(){
        for(int i = 0; i<constants.filaments; i++){

            //generate a free filament.
            double x = constants.SEED_WIDTH * number_generator.nextDouble() - 0.5 * constants.SEED_WIDTH;
            double y = constants.SEED_WIDTH * number_generator.nextDouble() - 0.5 * constants.SEED_WIDTH;
            double z = constants.THICKNESS * number_generator.nextDouble() - 0.5 * constants.THICKNESS;

            double theta = 2 * Math.PI * number_generator.nextDouble();


            ActinFilament f = createNewFilament();

            double phi = Math.acos(Math.sin(constants.ANGLE_SIGMA/2)*(1 - 2*number_generator.nextDouble()));

            f.direction[0] = Math.cos(theta) * Math.sin(phi);
            f.direction[1] = Math.sin(theta) * Math.sin(phi);
            f.direction[2] = Math.cos(phi);


            f.position[0] = x;
            f.position[1] = y;
            f.position[2] = z;

            f.updateBounds();
            actins.add(f);
        }
    }

    /**
     * Seeds myosin motors in a box without a membrane.
     *
     */
    public void seedMyosinMotorsFreeEdge(){
        for(int i = 0; i<constants.motors; i++) {
            MyosinMotor motor = createNewMyosinMotor();
            MyosinMotorBinding bind = new MyosinMotorBinding(this, motor);
            placeBoundMyosinMotor(motor, bind);
            bindings.add(bind);
            myosins.add(motor);
        }
    }

    /**
     * Places a myosin motor attached to at least 1 actin filament.
     * @param motor the rod that represents the motor.
     * @param bind the interactions of the motor heads.
     */
    public void placeBoundMyosinMotor(MyosinMotor motor, MyosinMotorBinding bind){
        ActinFilament host = actins.get(number_generator.nextInt(actins.size()));

        double s = (number_generator.nextDouble() - 0.5) * host.length;
        double[] host_a = host.getPoint(s);

        bind.bind(host, MyosinMotor.FRONT, s);

        List<ActinFilament> possibles = new ArrayList<>();
        for (ActinFilament target : actins) {
            if (host == target) continue;
            double separation = target.closestApproach(getReflectedPoint(target.position, host_a));
            if (separation < motor.length) {
                double[] intersections = target.getIntersections(getReflectedPoint(target.position, host_a), motor.length + 2*constants.MYOSIN_BIND_LENGTH);
                if (intersections.length == 0) {
                    //The attached head of the motor is too close to the center of the target filament.
                    continue;
                }
                possibles.add(target);
            }
        }
        double[] host_b;
        if (possibles.size() > 0) {

            ActinFilament other = possibles.get(number_generator.nextInt(possibles.size()));

            double[] intersections = other.getIntersections(getReflectedPoint(other.position, host_a), motor.length + 2*constants.MYOSIN_BIND_LENGTH);

            double s2 = intersections[number_generator.nextInt(intersections.length)];
            host_b = getReflectedPoint(host_a, other.getPoint(s2));
            bind.bind(other, MyosinMotor.BACK, s2);
        } else {
            double phi = Math.PI * number_generator.nextDouble();
            double theta = 2 * Math.PI * number_generator.nextDouble();
            double l = motor.length + motor.diameter + host.diameter;
            host_b = new double[]{
                    host_a[0] + l * Math.cos(theta) * Math.sin(phi),
                    host_a[1] + l * Math.sin(theta) * Math.sin(phi),
                    host_a[2] + l * Math.cos(phi)
            };
        }

        motor.position[0] = (host_a[0] + host_b[0]) * 0.5;
        motor.position[1] = (host_a[1] + host_b[1]) * 0.5;
        motor.position[2] = (host_a[2] + host_b[2]) * 0.5;


        double[] dir = Line3D.difference(host_a, host_b);

        System.arraycopy(Line3D.normalize(dir), 0, motor.direction, 0, 3);

        motor.updateBounds();

    }

    /**
     * Finds all intersecting actin filaments, an intersection is when the actin filaments are closer than the
     * crosslinker length. After finding all of the intersections, the probability of crossing is used to
     * see whether the a crosslinker is added.
     *
     */
    public void seedCrosslinkers(){

        double p = constants.CROSS_LINK_BIND_PROBABILITY;
        List<ActinFilament[]> crossed = new ArrayList<>();
        int filaments = actins.size();
        for(int i = 0; i<filaments; i++){
            for(int j = i+1; j<filaments; j++){
                ActinFilament meeny = actins.get(i);
                ActinFilament miney = actins.get(j);


                double d = getReflectedApproach(meeny, miney);


                if(d<constants.CROSS_LINK_LENGTH){
                    crossed.add(new ActinFilament[]{meeny, miney});
                }
            }
        }
        for(ActinFilament[] pair: crossed){
            if(number_generator.nextDouble()>p){
                continue;
            }
            crosslinkFilaments(pair[0], pair[1]);
        }

    }

    /**
     * Sets the force preparation method.
     *
     */
    public void initializeForcePreparation(){

        prepare = this::prepareNonStericForces;

    }

    /**
     * Initializes a simulation, prepares an integrator, force preparation. Clears the history (graphs and such)
     * seeds actin filaments, then seeds crosslinkers and relaxes stresses (shouldn't be any) then seeds myosin motors
     * and relaxes stresses again.
     */
    public void initializeSimulation(){

        initializeIntegrator();
        initializeForcePreparation();


        clearHistory();
        seedActinFilamentsFreeEdge();

        seedMyosinMotorsFreeEdge();
        relaxStresses();

        seedCrosslinkers();

        relaxStresses();
    }

    /**
     *  Finds the positions on each rod of closest approach with period boundary conditions.
     *
     * @param a
     * @param b the position of this rod will reflected across necessary boundaries to be the closest possible
     *          position to the center of a. This puts some restriction on the size of the box and the length of rods.
     * @return {s_a, s_b}
     */
    public double[] getReflectedIntersections(Rod a, Rod b){

        double[] reflected = getReflectedPoint(a.position, b.position);
        ProxyRod rod = new ProxyRod(b, reflected);

        return a.intersections(rod);

    }

    /**
     * Calls stepSimulation() a STEPS_PER_SIMULATE number of steps, then runs the callback. If graphing the graphs are
     * also updated. the callback is run at the end.
     *
     * @param callback a way to ensure things are notified when simulating is finished.
     */
    public void simulate(final Runnable callback){
        for(int i = 0; i<constants.STEPS_PER_SIMULATE; i++){
            stepSimulation();
            callback.run();
            printStatus();
            if(GRAPHING){
                measureTension();
                measureMyosinZDistribution();
                plotAngleDistribution();
            }
        }
        System.out.println("finito");
    }

    /**
     * Updates the myosin motor bindings.
     *
     * @param dt
     */
    public void updateInteractions(double dt){
        prepareForces();

        //step the myosin motors.
        Iterator<MyosinMotorBinding> motorator = bindings.iterator();
        while(motorator.hasNext()){
            MyosinMotorBinding motor = motorator.next();
            motor.update(constants.DT);
        }

        int m = bindings.size();
        //check if the motors are still bound. Replace if they have become free.
        for (int j = 0; j < m; j++) {
            MyosinMotorBinding motor_binding = bindings.get(j);
            if (motor_binding.hasFreeHead()) {
                // check if both heads are free and replace if that is the case.
                if (motor_binding.motor.isFree(MyosinMotor.FRONT) && motor_binding.motor.isFree(MyosinMotor.BACK)) {
                    placeBoundMyosinMotor(motor_binding.motor, motor_binding);
                }
            }
        }
        time += constants.DT;
    }

    /**
     * relax stresses.
     */
    public void relaxStresses(){
        boolean working = true;

        int count = 0;

        clearForces();
        double starting_threshold = constants.ERROR_THRESHOLD;

        double starting = prepareForces();

        double last_f=starting;
        double last_energy = calculateInteractionEnergy();

        do{
            integrator.relaxStep();
            double energy = calculateInteractionEnergy();
            starting = prepareForces();

            if(last_energy<energy ){
                constants.ERROR_THRESHOLD=constants.ERROR_THRESHOLD/2;
                integrator.rejectStep();
                starting = last_f;
                energy = last_energy;
            } else{
                last_f=starting;
            }


            if(starting < constants.RELAXATION_LIMIT && max_out_of_eq<0.01){
                working=false;

            } else if(count>=constants.SUB_STEPS){
                if(starting<constants.RELAXATION_LIMIT){
                    working = false;
                } else{
                    log("fail: " + time + " : " + starting + " ? " + constants.RELAXATION_LIMIT + " : " + max_out_of_eq + " ? " + 0.01);
                    count=0;
                }
            }
            last_energy = energy;

            //do a relax step.
            count++;

        }while(working);
        clearForces();
        constants.ERROR_THRESHOLD= starting_threshold;
    }

    /**
     * Loops over all of the steps per frame. Updates the interactions with a time step of dt, then relaxes the stresses
     * in the network.
     *
     */
    public void stepSimulation(){
        long begin = System.currentTimeMillis();
        long interactions = 0;
        long relaxations = 0;
        long t = begin;
        long s;

        for(int i = 0; i<constants.STEPS_PER_FRAME; i++){
            updateInteractions(constants.DT);
            s = System.currentTimeMillis();
            interactions += s - t;
            relaxStresses();
            t = System.currentTimeMillis();
            relaxations += t - s;
        }
        center();

        sanitizeRods();
        System.out.println("step: " + (System.currentTimeMillis()- begin) + " relax: " + relaxations + " interactions: " + interactions);

    }

    /**
     * Goes through all of the rods, and makes sure their x-y coordinates are contained in the simulation box, and
     * that their direction normalized.
     */
    public void sanitizeRods(){

        actins.stream().forEach(this::sanitizeRod);
        myosins.stream().forEach(this::sanitizeRod);
    }

    public void sanitizeRod(Rod r){
        boolean changed = false;
        if(r.position[0]<-0.5*constants.SEED_WIDTH){
            r.position[0] += constants.SEED_WIDTH;
            changed = true;
        } else if(r.position[0]>0.5*constants.SEED_WIDTH){
            r.position[0] -= constants.SEED_WIDTH;
            changed = false;
        }

        if(r.position[1]<-0.5*constants.SEED_WIDTH){
            r.position[1] += constants.SEED_WIDTH;
            changed = true;
        } else if(r.position[1]>0.5*constants.SEED_WIDTH){
            r.position[1] -= constants.SEED_WIDTH;
            changed = false;
        }



        double m = Math.sqrt(Math.pow(r.direction[0], 2) + Math.pow(r.direction[1], 2) + Math.pow(r.direction[2], 2));
        r.direction[0] = r.direction[0]/m;
        r.direction[1] = r.direction[1]/m;
        r.direction[2] = r.direction[2]/m;

        if(changed){
            r.updateBounds();
        }
    }

    /**
     * If the two filaments are close enough, they will be crosslinked after this method has run.
     *
     * @param fa
     * @param fb
     */
    public void crosslinkFilaments(ActinFilament fa, ActinFilament fb){
        if(fa.isBound(fb)) return;

        Crosslinker x = createNewCrossLinker();

        double[] sections = getReflectedIntersections(fa, fb);
        double[] a = fa.getPoint(sections[0]);

        double[] possible = fb.getIntersections(getReflectedPoint(fb.position, a), x.length);

        if(possible.length==0){
            log("Warning: cross linked filaments not close enough");
            possible = fa.getIntersections(fb.position, x.length);
            if(possible.length==0) return;
        }

        double bs = possible[number_generator.nextInt(possible.length)];


        double[] b = fb.getPoint(bs);
        x.A = a;
        x.B = b;

        double duration = Double.MAX_VALUE;

        xlinked.add(new CrosslinkedFilaments(this, fa, fb, x, sections[0], bs, duration));

        linkers.add(x);
    }

    /**
     * prepare crosslinker spring forces and forces due to myosin attachments.
     *
     * @return sum of out-of-balance force & torque values.
     */
    public double prepareNonStericForces(){
        int linkers = xlinked.size();
        for (int j = 0; j < linkers; j++) {
            xlinked.get(j).applyForces();
        }
        int bindingCount = bindings.size();
        for (int j = 0; j < bindingCount; j++) {
            bindings.get(j).applyForces();
        }

        double outOfEqSum = 0;

        double max = -Double.MAX_VALUE;

        int a = actins.size();

        for (int j = 0; j < a; j++) {
            ActinFilament f = actins.get(j);
            double v = f.prepareForces();
            outOfEqSum += v;
            max = max > v ? max : v;

        }

        int myosinCount = myosins.size();
        for (int j = 0; j < myosinCount; j++) {
            MyosinMotor m = myosins.get(j);
            double v = m.prepareForces();
            outOfEqSum += v;
            max = max > v ? max : v;

        }

        max_out_of_eq = max;

        return outOfEqSum;
    }


    public void printStatus(){
        double crosslinkers = linkers.size();
        double bind_count = 0;
        for(MyosinMotor motor: myosins){
            if(motor.isBound(MyosinMotor.FRONT)){
                bind_count++;
            }
            if(motor.isBound(MyosinMotor.BACK)){
                bind_count++;
            }
        }

        double forces = prepareForces();
        clearForces();
        System.out.println(String.format("%f\t%f\t%f\t%f\t%f\t%f", time, forces, max_out_of_eq, crosslinkers, bind_count, 1.0*bindings.size()));

    }

    public double prepareForces(){
        return prepare.prepareForces();
    }

    private double reflectedCollision(Rod other, Rod filament) {
        if(other==filament){
            return -1;
        }
        double[] pt = getReflectedPoint(filament.position, other.position);
        if ((pt[0] - other.position[0]) == 0 && (pt[1] - other.position[1]) == 0) {
            return filament.collide(other);
        } else {
            Rod r = new ProxyRod(other, pt);
            filament.collide(r);
        }
        return 0;
    }

    public double getReflectedApproach(ActinFilament a, ActinFilament b){

        double[] reflected = getReflectedPoint(a.position, b.position);

        ProxyRod rod = new ProxyRod(b,reflected);
        return a.closestApproach(rod);

    }

    /**
     * Creates a reflect point that is closer to the src, than the original target.
     *
     * @param src this point will not be moved
     * @param target this point will be moved to be the shorter distance in a reflected geometry.
     * @return the target possibly reflected to be the shorter distance to src.
     */
    public double[] getReflectedPoint(double[] src, double[] target){
        double hw = constants.WIDTH*0.5;
        double[] ret = new double[3];
        ret[2] = target[2];
        if( target[0] - src[0] > hw){
            ret[0] = target[0] - constants.WIDTH;
        } else if(target[0] - src[0]<-hw){
            ret[0] = target[0] + constants.WIDTH;
        } else{
            ret[0] = target[0];
        }


        if( target[1] - src[1] > hw){
            ret[1] = target[1] - constants.WIDTH;
        } else if(target[1] - src[1]<-hw){
            ret[1] = target[1] + constants.WIDTH;
        } else{
            ret[1] = target[1];
        }

        return ret;
    }


    public int getActinIndex(Rod i){

        return actins.indexOf(i);

    }


    public List<MyosinMotor> getMyosins() {
        return Collections.unmodifiableList(myosins);
    }


    public List<ActinFilament> getActin() {
        return Collections.unmodifiableList(actins);
    }

    public List<MyosinMotorBinding> getMotorBindings() {

        return Collections.unmodifiableList(bindings);

    }

    public List<CrosslinkedFilaments> getCrosslinkedFilaments() {
        return Collections.unmodifiableList(xlinked);
    }



    public List<Crosslinker> getCrosslinkers() {
        return Collections.unmodifiableList(linkers);
    }

    public ActinFilament createNewFilament(){
        ActinFilament f = new ActinFilament();
        f.length = constants.ACTIN_LENGTH + number_generator.nextGaussian()*constants.ACTIN_LENGTH_SIGMA;
        f.diameter = constants.ACTIN_DIAMETER;
        f.alpha_longitudinal = constants.ACTIN_ALPHA;
        f.alpha_perpendicular = constants.ACTIN_ALPHA;
        f.alpha_rotational = constants.ACTIN_ALPHA;
        return f;
    }

    public MyosinMotor createNewMyosinMotor(){
        MyosinMotor motor = new MyosinMotor();
        motor.length = constants.MYOSIN_LENGTH;
        motor.F0 = constants.MYOSIN_ACTIVE_FORCE;
        motor.alpha_longitudinal = constants.MYOSIN_ALPHA;
        motor.alpha_perpendicular = constants.MYOSIN_ALPHA;
        motor.alpha_rotational = constants.MYOSIN_ALPHA;
        motor.alpha_s = constants.MYOSIN_ALPHA_S;
        motor.K_m = constants.K_m;
        motor.diameter = constants.MYOSIN_DIAMETER;
        motor.tau_B = constants.MYOSIN_BINDING_TIME;
        return motor;
    }

    public Crosslinker createNewCrossLinker(){
        Crosslinker x = new Crosslinker();
        x.K_x = constants.K_x;
        x.length = constants.CROSS_LINK_LENGTH;
        return x;
    }


    public void addMyosin(MyosinMotor m){
        myosins.add(m);
    }

    public void addActin(ActinFilament f){
        actins.add(f);
    }

    public void addBinding(MyosinMotorBinding binding){
        bindings.add(binding);
    }

    public void addXLinker(Crosslinker link){
        linkers.add(link);
    }

    public void addCrossLinking(CrosslinkedFilaments linkage){
        xlinked.add(linkage);
    }

    public JavaScriptTerminal showTerminal() {
        terminal.setVisible(true);
        return terminal;
    }

    public void log(String message){

        LocalDateTime timed = LocalDateTime.now();
        try(

                BufferedWriter logging = Files.newBufferedWriter(
                        Paths.get(log_file.toURI()), Charset.forName("UTF8"), StandardOpenOption.APPEND, StandardOpenOption.CREATE
                )
        ) {
            logging.write(String.format("[%s]: %s\n", timed.toString(), message));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void center(){
        double z = 0;

        for(Rod r: getActin()){
            z += r.position[2];
        }
        z = z/actins.size();
        for(Rod r: getActin()){
            r.position[2] -= z;
        }

        for(Rod r: getMyosins()){
            r.position[2] -= z;
        }
    }

    /**
     * measures the tension by dividing the width along the x axis into 500 locations and measuring the tension by
     * slicing in the x plane at each point along the x-axis. Creates a plot with the tension compontents.
     *
     */
    public void scanTensionMeasurement() {
        int n = 500;
        double[] x = new double[n];
        double[] aT = new double[n];
        double[] mT = new double[n];
        double[] cT = new double[n];
        double[] T = new double[n];
        double dx = constants.WIDTH/(n-1);
        prepareForces();
        if(actins.size()==0) return;
        double[] x_dir = new double[]{1, 0, 0};

        for(int i = 0; i<n; i++) {

            double[] pos = new double[]{i*dx - 0.5*constants.WIDTH, 0, 0};
            //double[] y_dir = new double[]{0, 1, 0};
            double[] aFB = sliceActinFilaments(pos, x_dir);
            double[] mFB = sliceMyosinMotors(pos, x_dir);
            double[] cFB = sliceCrossLinkers(pos, x_dir);
            aT[i] = 0.5*aFB[0] + 0.5*aFB[1];
            x[i] = i*dx - 0.5*constants.WIDTH;
            mT[i] = 0.5*mFB[1] + 0.5*mFB[0];
            cT[i] = 0.5*cFB[0] + 0.5*cFB[1];
            double[] tFB = sliceNetwork(pos, x_dir);
            T[i] = 0.5*tFB[0] + 0.5*tFB[1];
        }
        Map<String, double[]> values = new HashMap<>();
        values.put("actins", aT);
        values.put("myosins", mT);
        values.put("crosslinkers", cT);
        values.put("sum", T);
        graphMachine.plot("Tension vs X", x, values);
        clearForces();

    }

    /**
     * Finds the average position and standard deviation of position for the actin filaments.
     *
     * @return {cx, cy, cz, stdx, stdy, stdz, nx}
     */
    public double[] measureAverageActinPositions() {

        double[] p = new double[7];

        actins.stream().forEach((a)->{
            p[0] += a.position[0];
            p[3] += a.position[0]*a.position[0];
            p[1] += a.position[1];
            p[4] += a.position[1]*a.position[1];
            p[2] += a.position[2];
            p[5] += a.position[2]*a.position[2];
        });

        p[0] = p[0]/actins.size();
        p[1] = p[1]/actins.size();
        p[2] = p[2]/actins.size();
        p[3] = Math.sqrt(p[3]/actins.size() - p[0]*p[0]);
        p[4] = Math.sqrt(p[4]/actins.size() - p[1]*p[1]);
        p[5] = Math.sqrt(p[5]/actins.size() - p[2]*p[2]);

        p[6] = linkers.size();
        return p;

    }

    /**
     * Measures the thickness based amount of filament. A lenght of z-axis 2 times the thickness is divided into 600
     * bins, then all of the actin filaments are scanned. The length of filament is projected along the z-axis to see
     * how many bins are occupied. Each bin receives an equal amount of filament.
     *
     *
     * @return Intensity (weighted histogram) of actin in each bin.
     */
    public double[] measureThickness(){
        int bins = 600;
        double rho = 1.0;
        double[] intensity = new double[bins];
        double max_x =2*constants.THICKNESS;
        double min_x = -max_x;
        double dx = (max_x - min_x)/bins;

        for(Rod r: actins){

            double total = rho*r.length;
            double z_projection = Math.abs(r.length*r.direction[2]);

            int bins_occupied = (int)(z_projection/dx);
            bins_occupied = bins_occupied==0?1:bins_occupied;
            double per_bin = total/bins_occupied;

            int first_bin = (int)((r.position[2] - 0.5* z_projection - min_x)/dx);

            for(int i = 0; i<bins_occupied; i++){
                int dex = first_bin + i;
                if(dex<0||dex>=bins){
                    continue;
                }

                intensity[dex]+= per_bin;
            }




        }
        return intensity;
    }

    /**
     * Creates a plot of the thickness measured above.
     *
     */
    public void plotThickness() {
        double[] intensity = measureThickness();

        int bins = intensity.length;
        double max_x =2*constants.THICKNESS;
        double min_x = -max_x;
        double dx = (max_x - min_x)/bins;

        double[] zs = new double[bins];
        for(int i = 0; i<bins; i++){
            zs[i] = dx*i + min_x;
        }
        graphMachine.addIntensityDistribution(zs, intensity);


    }

    /**
     * Calculates the energy in the interactions.
     *
     * @return sum of spring energies in the myosin motors and crosslinkers.
     */
    public double calculateInteractionEnergy(){
        double e = 0;
        for(CrosslinkedFilaments link: xlinked){
            e += link.getEnergy();
        }
        for(MyosinMotorBinding binding: bindings){
            e += binding.getEnergy();
        }
        return e;
    }

    /**
     * Calculates the normalization constant.
     *
     * @return reference tension
     */
    public double getT0(){
        double f0 = constants.MYOSIN_ACTIVE_FORCE;
        double lm = constants.MYOSIN_LENGTH + constants.MYOSIN_BIND_LENGTH*2;
        double W = constants.WIDTH;
        double Nm = constants.motors;
        return f0*lm*Nm/(W*W);

    }




}

/**
 * Interface for preparing forces. Current version only has one method for preparing forces.
 */
interface ForcePreparation{
    double prepareForces();
}