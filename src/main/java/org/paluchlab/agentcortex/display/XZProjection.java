package org.paluchlab.agentcortex.display;

/**
 * Projects onto the xz plane.
 *
 * Created on 4/30/14.
 */
public class XZProjection extends ProjectionPainter{
    @Override
    public int[] getSceneCoordinates(double[] xyz) {
        int x = (int)(scale_factor*(xyz[0]-simulation.getCenterX()) + view.getWidth()*0.5);
        int y = (int)(scale_factor*(xyz[2]-simulation.getCenterZ()) + view.getWidth()*0.5);
        return new int[]{x,y};
    }

    @Override
    public double getDepth(double[] xyz) {
        return 2*(xyz[1] - simulation.getCenterY())/simulation.getHeight();
    }
}
