package org.paluchlab.agentcortex.integrators;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.agents.ActinFilament;
import org.paluchlab.agentcortex.agents.MyosinMotor;
import org.paluchlab.agentcortex.agents.Rod;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class which the other integrators use ( only one implementation here ).
 * The primary reason for this class is to store forces, torques, directions
 * and positions, for comparison between steps.
 *
 * Created by msmith on 10/15/14.
 */
public abstract class Integrator {
    CortexModel model;
    List<MyosinMotor> myosins;
    List<ActinFilament> actins;
    List<double[]> positions = new ArrayList<>();
    List<double[]> directions = new ArrayList<>();
    List<double[]> forces = new ArrayList<>();
    List<double[]> torques = new ArrayList<>();
    public double dt;
    int force_states;

    public void setModel(CortexModel m){
        dt = m.constants.DT;
        model = m;
    }

    /**
     * sets the myosins that will be updated for relaxation.
     *
     * @param m motors.
     */
    public void setMyosins(List<MyosinMotor> m){
        myosins = m;
    }

    /**
     * Sets the actins that will be updated
     * @param a actins
     */
    public void setActins(List<ActinFilament> a){
        actins = a;
    }

    /**
     * Short circuit method to the models prepare forces.
     *
     * @return
     */
    double prepareForces(){
        return model.prepareForces();
    }

    /**
     * restors the positions of the actins and myosins to the positions previously
     * stored at the requested index.
     * @param index location of stored positions.
     */
    public void restorePositions(int index){
        double[] pos = positions.get(index);
        double[] dir = directions.get(index);

        for(int i = 0 ;i<actins.size(); i++){

            System.arraycopy(pos, 3 * i, actins.get(i).position, 0, 3);
            System.arraycopy(dir, 3 * i, actins.get(i).direction, 0, 3);

            actins.get(i).updateBounds();

        }
        int start = 3*actins.size();

        for(int j = 0; j<myosins.size(); j++){

            System.arraycopy( pos, 3*j + start,myosins.get(j).position, 0, 3);
            System.arraycopy( dir, 3*j + start, myosins.get(j).direction, 0, 3);

            myosins.get(j).updateBounds();
        }


    }

    /**
     * Stores the positions and directions of the current actin and myosins
     * at the requested location.
     *
     * @param index location for positions to be stored.
     */
    public void storePositions(int index){
        double[] pos,dir;

        if(index>=positions.size()) {
            pos = new double[3 * actins.size() + 3 * myosins.size()];
            dir = new double[3 * actins.size() + 3 * myosins.size()];
            positions.add(pos);
            directions.add(dir);
        } else {
            pos = positions.get(index);
            if(pos.length!=(3 * actins.size() + 3 * myosins.size())){
                //already stored but the wrong length. It will be replaced.
                pos = new double[3 * actins.size() + 3 * myosins.size()];
                dir = new double[3 * actins.size() + 3 * myosins.size()];
                positions.set(index, pos);
                directions.set(index, dir);
            } else {
                dir = directions.get(index);
            }
        }

        for(int i = 0 ;i<actins.size(); i++){

            System.arraycopy(actins.get(i).position, 0, pos, 3*i, 3);
            System.arraycopy(actins.get(i).direction, 0, dir, 3*i, 3);

        }
        int start = 3*actins.size();
        for(int j = 0; j<myosins.size(); j++){

            System.arraycopy(myosins.get(j).position, 0, pos, 3*j + start, 3);
            System.arraycopy(myosins.get(j).direction, 0, dir, 3*j + start, 3);
        }



    }

    /**
     * Stores the forces to be restorable, or in the case of the runge kutta (not included),
     * the forces can be used for calculations.
     *
     * @param index for updating forces using a hirer order technique than just gradient decent.
     */
    public void storeForceState(int index){
        int total = actins.size()*3 + myosins.size()*3;
        if(forces.size()==0 || forces.get(0).length<total){
            forces.clear();
            torques.clear();
            //create a new set of forces. for RK4.
            for(int i = 0; i<force_states; i++){
                forces.add(new double[total]);
                torques.add(new double[total]);
            }
        }

        final double[] force_set = forces.get(index);
        final double[] torque_set = torques.get(index);

        for(int i = 0; i<actins.size(); i++){
            int dex = 3*i;
            Rod rod = actins.get(i);
            force_set[dex] = rod.force[0];
            force_set[dex + 1] = rod.force[1];
            force_set[dex + 2] = rod.force[2];

            torque_set[dex] = rod.torque[0];
            torque_set[dex+1] = rod.torque[1];
            torque_set[dex+2] = rod.torque[2];

        }

        for(int i = 0; i<myosins.size(); i++){
            int dex = 3*i + actins.size()*3;
            Rod rod = myosins.get(i);
            force_set[dex] = rod.force[0];
            force_set[dex + 1] = rod.force[1];
            force_set[dex + 2] = rod.force[2];

            torque_set[dex] = rod.torque[0];
            torque_set[dex+1] = rod.torque[1];
            torque_set[dex+2] = rod.torque[2];

        }


    }

    /**
     * restores the prepared force state of the net forces/torques.
     *
     * @param index
     */
    void restoreForceState(int index){
        final double[] force_set = forces.get(index);
        final double[] torque_set = torques.get(index);

        for(int i = 0; i<actins.size(); i++){
            int dex = 3*i;
            Rod rod = actins.get(i);
            System.arraycopy(force_set, dex, rod.force, 0, 3);
            System.arraycopy(torque_set, dex, rod.torque, 0, 3);
        }

        for(int i = 0; i<myosins.size(); i++){
            int dex = 3*i + actins.size()*3;
            Rod rod = myosins.get(i);
            System.arraycopy(force_set, dex, rod.force, 0, 3);
            System.arraycopy(torque_set, dex, rod.torque, 0, 3);

        }
    }

    /**
     * Calculates the difference in position and direction between the current configuration
     * and the one specified by index.
     *
     * @param index index of the stored positions to be compared to.
     * @return rms of the difference between positions and directions for
     * all rods, actin and myosin.
     */
     double calculateDeviation(int index) {
        final double[] pos = positions.get(index);
        final double[] dir = directions.get(index);

        double error = 0;
        double v;
        for(int i = 0; i<actins.size(); i++){
            int dex = 3*i;
            Rod rod = actins.get(i);
            double[] p = rod.position;

            v = pos[dex] - p[0];
            error += v*v;
            v = pos[dex+1] - p[1];
            error += v*v;
            v = pos[dex+2] - p[2];
            error += v*v;

            double[] d = rod.direction;

            v = dir[dex] - d[0];
            error += v*v;
            v = dir[dex+1] - d[1];
            error += v*v;
            v = dir[dex+2] - d[2];
            error += v*v;

        }

        for(int i = 0; i<myosins.size(); i++){
            int dex = 3*i + actins.size()*3;
            Rod rod = myosins.get(i);
            double[] p = rod.position;

            v = pos[dex] - p[0];
            error += v*v;
            v = pos[dex+1] - p[1];
            error += v*v;
            v = pos[dex+2] - p[2];
            error += v*v;

            double[] d = rod.direction;

            v = dir[dex] - d[0];
            error += v*v;
            v = dir[dex+1] - d[1];
            error += v*v;
            v = dir[dex+2] - d[2];
            error += v*v;

        }
        return Math.sqrt(error);
    }

    /**
     * received the simulation with a prepared force state,
     * and returns it w/out prepared forces.
     */
    abstract public void relaxStep();

    /**
     * should be overriden for adaptive integrators.
     */
    public void rejectStep(){
        throw new RejectionNotSupportedException("this integrator does not support job rejection!");
    }
}

/**
 * If an integrator does not implement a reject step this will be thrown.
 */
class RejectionNotSupportedException extends RuntimeException{
    RejectionNotSupportedException(String message){
        super(message);
    }
}