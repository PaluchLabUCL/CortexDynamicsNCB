package org.paluchlab.agentcortex.agents;

import org.paluchlab.agentcortex.display.Painter3D;

import java.awt.*;

/**
 * Generic Crosslinker for crosslinking actin filaments. Used for keeping track of constants and drawing purposes.
 *
 * Created on 4/25/14.
 */
public class Crosslinker implements Agent {
    public double[] A, B;
    double radius = 0.04;
    public double length;
    public double K_x;
    Color color = new Color(155, 255, 155);
    @Override
    public void draw(Painter3D graphics) {
        graphics.setWidth(0.02);
        graphics.setColor(color);
        graphics.drawLine(A, B);
        graphics.drawSphere(A, radius);
        graphics.drawSphere(B, radius);

    }

    @Override
    public double[] getPosition() {
        return new double[]{0.5*(A[0] + B[0]), 0.5*(A[1] + B[1]), 0.5*(A[2] + B[2])};
    }


    @Override
    public void drawForces(Painter3D graphics){
        //doesn't actually exist.
    }

}
