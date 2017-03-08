package org.paluchlab.agentcortex.display;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Projection painter that projects onto the x-y plane.
 * Create on 4/28/14.
 */
public class XYProjection extends ProjectionPainter {
    public int[] getSceneCoordinates(double[] xyz){
        int x = (int)(scale_factor*(xyz[0]-simulation.getCenterX()) + view.getWidth()*0.5);
        int y = (int)(scale_factor*(xyz[1]-simulation.getCenterY()) + view.getHeight()*0.5);
        return new int[]{x,y};
    }

    @Override
    public double getDepth(double[] xyz) {

        return 4*(xyz[2] - simulation.getCenterZ())/simulation.getDepth();
    }
}
