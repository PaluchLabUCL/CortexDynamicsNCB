package org.paluchlab.agentcortex.interactions;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.agents.ActinFilament;
import org.paluchlab.agentcortex.agents.MyosinMotor;
import org.paluchlab.agentcortex.geometry.Line3D;

import java.util.Arrays;

/**
 * Tracks the binding positions of a single myosin motor. The spontaneous unbind time, the current time and binding
 * position are tracked for each head.
 *
 *
 * The relative value is positioned in the array via the MyosinMotor constant value, FRONT or BACK.
 *
 * Created by on 5/11/14.
 */
public class MyosinMotorBinding {
    /* currently location of bound head along filament.) */
    public double[] binding_position = new double[2];
    /* For updating the position of the myosin motor. */
    double[] sliding = new double[2];
    /* The time when a head will unbind. */
    public double[] unbind_time = new double[2];
    /* the time elapsed since binding. */
    public double[] current_time = new double[2];
    CortexModel model;
    public MyosinMotor motor;
    boolean bound;

    /**
     * Creates a binding that tracks the supplied motor. The model is necessary for constants, and geometry.
     *
     *
     * @param model used for geometry.
     * @param motor motor that is tracked. The motor is  Biding and unbinding times, and
     */
    public MyosinMotorBinding(CortexModel model, MyosinMotor motor){
        this.model = model;
        this.motor = motor;

        bound = false;
    }

    /**
     * Changes the model.
     *
     * @param model new model to be used.
     */
    public void setModel(CortexModel model){
        this.model = model;
    }

    /**
     *  Binds the myosin motor head to the provided filament.
     *
     * @param f
     * @param head
     * @param position
     */
    public void bind(ActinFilament f, int head, double position){
        binding_position[head] = position;
        motor.bind(f, head);
        bound = true;
        current_time[head] = 0;
        unbind_time[head] = -motor.tau_B*Math.log(model.number_generator.nextDouble());
    }

    /**
     * Applys forces to the tracked myosin motor.
     *
     */
    public void applyForces(){
        if(motor.isBound(MyosinMotor.FRONT)){
            headForce(MyosinMotor.FRONT);
        }

        if(motor.isBound(MyosinMotor.BACK)){
            headForce(MyosinMotor.BACK);
        }
    }

    /**
     * Calculates the dipole as defined as the total force along the axis of the motor. defined as:
     *
     *   (f_{front} - f_{back}) dot motor.direction.
     *
     * Due to the fact myosin motors *only* have two forces, front and back, the force is always axial and
     * the dot product is redundant.
     *
     * @return The front force minus the back force dotted with the direction of the motor.
     */
    public double getForceDipole(){

        double[] front = getHeadForce(MyosinMotor.FRONT);
        double[] back = getHeadForce(MyosinMotor.BACK);
        front[0] -= back[0];
        front[1] -= back[1];
        front[2] -= back[2];

        return Line3D.dot(motor.direction, front);
    }

    /**
     * The dipole as a direction. The direction is the direction of the motor.
     *
     * @return {dx, dy, dz} components of the dipole
     */
    public double[] getDirectionalForceDipole(){
        double f = getForceDipole();
        return new double[]{f*motor.direction[0], f*motor.direction[1], f*motor.direction[2]};
    }

    /**
     * Applies a head force to the indicated head, and updates the sliding parameter.
     *
     * @param head which head will be checked, and force applied.
     */
    void headForce(int head){
        ActinFilament filament = motor.getBound(head);
        double ml = head==motor.FRONT?0.5*motor.length:-0.5*motor.length;
        double[] front_head = motor.getPoint(ml);
        double[] attached_position = filament.getPoint( binding_position[head]);

        double[] reflected = model.getReflectedPoint(front_head, attached_position);

        //line from myosin head to actin filament attachment location.
        double[] r = Line3D.difference(reflected, front_head);

        double mag = Line3D.magnitude(r);
        double separation = mag - model.constants.MYOSIN_BIND_LENGTH;

        double f = mag==0?0:motor.K_m/mag*separation;

        double[] head_force = new double[]{ f*r[0], f*r[1], f*r[2], ml };
        motor.applyForce(head_force);
        double dot = Line3D.dot(head_force, filament.direction);
        sliding[head] = (motor.F0 - dot)/motor.alpha_s;

        double[] filament_force = new double[]{-head_force[0], -head_force[1], -head_force[2], binding_position[head]};
        filament.applyForce(filament_force);
    }

    /**
     * Just calculates the head force
     * @param head
     * @return {fx, fy, fz}
     */
    double[] getHeadForce(int head){
        ActinFilament filament = motor.getBound(head);
        double ml = head==motor.FRONT?0.5*motor.length:-0.5*motor.length;
        double[] front_head = motor.getPoint(ml);
        double[] attached_position = filament.getPoint( binding_position[head]);

        double[] reflected = model.getReflectedPoint(front_head, attached_position);

        //line from myosin head to actin filament attachment location.
        double[] r = Line3D.difference(reflected, front_head);

        double mag = Line3D.magnitude(r);
        double separation = mag - model.constants.MYOSIN_BIND_LENGTH;
        double f = mag==0?0:motor.K_m/mag*separation;

        double[] head_force = new double[]{ f*r[0], f*r[1], f*r[2], ml };

        return head_force;
    }

    /**
     *
     * This will displace the head according to the forces calculated during headForce.
     *
     * @param dt time step size
     * @param head front or back
     */
    void slideHead(double dt, int head){
        if(motor.isFree(head)) return;
        binding_position[head] += sliding[head]*dt;
        if(Math.abs(binding_position[head])>motor.getBound(head).length*0.5) {
            //unbind
            motor.unbind(head);
        }
    }

    /**
     * Checks each head to see if they will unbind during the current step. Moves each head according to force balance.
     *
     * @param dt time step
     */
    public void update(double dt){
        checkTime(MyosinMotor.FRONT, dt);
        checkTime(MyosinMotor.BACK, dt);

        slideHead(dt, MyosinMotor.FRONT);
        slideHead(dt, MyosinMotor.BACK);

        if(motor.isFree(MyosinMotor.FRONT)&&motor.isFree(MyosinMotor.BACK)){
            bound=false;
        }

    }

    /**
     * If the head is bound, updates its current time, and checks if it has passed the unbind time.
     *
     * @param head
     * @param dt
     */
    public void checkTime(int head, double dt){
        if(motor.isBound(head)){
            current_time[head] += dt;
            if(current_time[head]>unbind_time[head]){
                motor.unbind(head);
            }
        }
    }

    /**
     * Checks if at least one head is free.
     *
     * @return false if both heads are bound.
     */
    public boolean hasFreeHead(){
        return motor.isFree(MyosinMotor.FRONT)||motor.isFree(MyosinMotor.BACK);
    }


    /**
     * Elastic energy for both heads.
     *
     * @return 1/2 k x**2 + 1/2 k x**2
     */
    public double getEnergy(){
        double energy = 0;
        if(motor.isBound(MyosinMotor.FRONT)){
            energy += getHeadEnergy(MyosinMotor.FRONT);
        }

        if(motor.isBound(MyosinMotor.BACK)){
            energy += getHeadEnergy(MyosinMotor.BACK);
        }
        return energy;


    }

    /**
     * Energy for a single head.
     *
     * @param head
     * @return 1/2 k dx^2
     */
    public double getHeadEnergy(int head){

        ActinFilament filament = motor.getBound(head);
        double ml = head==motor.FRONT?0.5*motor.length:-0.5*motor.length;
        double[] front_head = motor.getPoint(ml);
        double[] attached_position = filament.getPoint( binding_position[head]);

        double[] reflected = model.getReflectedPoint(front_head, attached_position);

        //line from myosin head to actin filament attachment location.
        double[] r = Line3D.difference(reflected, front_head);

        double mag = Line3D.magnitude(r);
        double separation = mag - model.constants.MYOSIN_BIND_LENGTH;

        return 0.5*separation*separation*motor.K_m;

    }

}
