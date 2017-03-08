package org.paluchlab.agentcortex.display;

import java.awt.*;

/**
 * Interface for painting 3d objects.
 * Created on 4/28/14.
 */
public interface Painter3D {

    public void drawLine(double[] a, double[] b);
    public void drawSphere(double[] center, double radius);
    public void drawPlane(double[] center, double[] direction);
    public void setColor(Color c);
    public void setWidth(double w);

}
