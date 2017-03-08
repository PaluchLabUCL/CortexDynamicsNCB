package org.paluchlab.agentcortex.agents;

import org.paluchlab.agentcortex.display.Painter3D;


/**
 * Generic interface for 'agents' which can be drawn.
 * Created on 4/25/14.
 */
public interface Agent {
    /**
     * Called during drawing, it is up to the agent to decide how it is drawn.
     * @param graphics
     */
    void draw(Painter3D graphics);
    /**
     * Called during drawing, it is up to the agent to decide how it is drawn, or if forces are drawing.
     * @param graphics
     */
    void drawForces(Painter3D graphics);

    /**
     * An estimate of position, used for sorting.
     * @return {x, y, z}
     */
    double[] getPosition();
}
