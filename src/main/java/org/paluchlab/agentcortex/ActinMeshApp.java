package org.paluchlab.agentcortex;

import org.paluchlab.agentcortex.display.SwingDisplay;

/**
 * Created by msmith on 4/25/14.
 */
public class ActinMeshApp {
    /**
     * Starts a new simulation with graphical display, defaults to the current ModelConstants values.
     *
     * @param args
     */
    public static void main(String[] args){

        CortexModel model = new CortexModel(false);
        model.GRAPHING = true;
        new SwingDisplay(model).start();

    }

}
