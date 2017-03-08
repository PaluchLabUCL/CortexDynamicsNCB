package org.paluchlab.agentcortex.agents;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.display.Painter3D;
import org.paluchlab.agentcortex.geometry.Line3D;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

/**
 * The myosin motor agent is used for drawing myosin motors. It also stores a reference to any bound filaments, and keeps
 * track of model constants and parameters.
 *
 * Created on 4/25/14.
 */
public class MyosinMotor extends Rod implements Agent{
    public final static int FRONT = 0;
    public final static int BACK = 1;

    public double K_m;
    public double F0;
    public double alpha_s;
    public double tau_B;

    double heads = 0.2;

    ActinFilament[] bound = new ActinFilament[2];


    @Override
    public void draw(Painter3D graphics) {

        double[] a = new double[]{
                position[0] - 0.5*length*direction[0],
                position[1] - 0.5*length*direction[1],
                position[2] - 0.5*length*direction[2]
        };

        double[] b = new double[]{
                position[0] + 0.5*length*direction[0],
                position[1] + 0.5*length*direction[1],
                position[2] + 0.5*length*direction[2]
        };

        graphics.setColor(Color.BLUE);
        graphics.setWidth(diameter);
        graphics.drawLine(a, b);
        graphics.drawSphere(a, heads);
        graphics.drawSphere(b, heads);
    }

    /**
     * Sets the 'head' slot to contain the target filament.
     *
     * @param fil filament that is bound.
     * @param head location of binding.
     */
    public void bind(ActinFilament fil, int head){

        bound[head]=fil;

    }

    /**
     * Checks if there is a filament at the requested slot.
     *
     * @param head
     * @return true if the location does not contain null.
     */
    public boolean isBound(int head){
        return bound[head]!=null;
    }

    /**
     * Sets the filament value to null, essentially unbinding the slot.
     *
     * @param head
     */
    public void unbind(int head){
        bound[head] = null;
    }


    @Override
    public double[] getPosition() {
        return position;
    }

    /**
     * Returns the bound filament at head.
     *
     * @param head
     * @return if bound, returns an actin filament
     */
    public ActinFilament getBound(int head) {
        return bound[head];
    }

    /**
     * checks if the required slot is null.
     *
     * @param head the required slot, using the constants FRONT or BACK.
     *
     * @return if there is no filament at the required slot.
     *
     */
    public boolean isFree(int head) {
        return bound[head]==null;
    }

    /**
     * Testing parameter to look at tension generation.
     *
     * @return
     */
    public double getFilamentDot(){
        if(isBound(MyosinMotor.FRONT)&&isBound(MyosinMotor.BACK)){

            return Line3D.dot(direction, bound[MyosinMotor.FRONT].direction) - Line3D.dot(direction, bound[MyosinMotor.BACK].direction);
        }
        return 0;
    }
}
