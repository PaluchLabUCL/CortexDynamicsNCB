package org.paluchlab.agentcortex.integrators;

import org.paluchlab.agentcortex.agents.ActinFilament;
import org.paluchlab.agentcortex.agents.MyosinMotor;

/**
 * This class uses an Euler adaptive step to relax the simulations. It is assumed
 * that the forces were prepared before starting this method.
 *
 * First the position and force state is stored then rods, motors and filaments are updated
 * with a full dt step. Then the position and forces are restored, and the rods are
 * updated twice with 1/2 dt. The difference between the two steps gives an estimate
 * for the error. The time step is adjusted due to the error size, if the error is
 * too large, the time step is reduced and the initial positions restored.
 *
 * Created on 10/15/14.
 */
public class AdaptiveEuler extends Integrator{
    //sets the number of force states this integrator will store.
    {force_states=1;}
    @Override
    public void relaxStep() {

        //save positions.
        storePositions(0);
        storeForceState(0);

        //full time update
        for(MyosinMotor motor: myosins){
            motor.update(dt);
        }

        for(ActinFilament f: actins){
            f.update(dt);
        }
        //store positions for comparison.
        storePositions(1);

        //restore original positions.
        restorePositions(0);
        restoreForceState(0);

        for(MyosinMotor motor: myosins){
            motor.update(dt /2);
        }

        for(ActinFilament f: actins){
            f.update(dt /2);
        }

        double new_forces = prepareForces();

        for(MyosinMotor motor: myosins){
            motor.update(dt /2);
        }

        for(ActinFilament f: actins){
            f.update(dt /2);
        }
        double error = calculateDeviation(1);

        if(Double.isNaN(error)){
            dt = 0.5*dt;
            restorePositions(0);
        } else{
            double normal_error = error/model.constants.ERROR_THRESHOLD;

            double factor = 0.9/Math.sqrt(normal_error);
            if(factor>1.1) factor=1.1;
            dt = dt*factor;
            if(normal_error>1){
                restorePositions(0);
            }
        }


    }

    /**
     * This is called by the model, if the interaction
     * energy increased during the step.
     */
    @Override
    public void rejectStep(){
        restorePositions(0);
        restoreForceState(0);
    }
}
