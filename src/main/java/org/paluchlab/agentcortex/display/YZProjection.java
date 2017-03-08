package org.paluchlab.agentcortex.display;

/**
 * Projects onto the yz plane.
 *
 * Created on 4/30/14.
 */
public class YZProjection extends ProjectionPainter {
    @Override
    public int[] getSceneCoordinates(double[] xyz) {
        int x = (int)(scale_factor*(xyz[1]-simulation.getCenterY()) + view.getWidth()*0.5);
        int y = (int)(scale_factor*(xyz[2]-simulation.getCenterZ()) + view.getHeight()*0.5);
        return new int[]{x,y};
    }

    @Override
    public double getDepth(double[] xyz) {
        return 2*(xyz[0] - simulation.getCenterX())/simulation.getWidth();
    }
}
