package org.paluchlab.agentcortex.agents;

import org.paluchlab.agentcortex.display.Painter3D;
import org.paluchlab.agentcortex.geometry.Line3D;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A rod implementation for actin. Implements Agent so it can be drawn and keeps track of the other actinfilaments it
 * is crosslinked too.
 *
 * Created on 4/25/14.
 */
public class ActinFilament extends Rod implements Agent {


    public List<ActinFilament> bound = new ArrayList<>();

    @Override
    public void draw(Painter3D painter) {
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

        //painter.setColor(java.awt.Color.RED);
        painter.setColor(new Color(255, 0, 0));
        painter.setWidth(diameter);
        painter.drawLine(a, b);
    }

    /**
     * Binds an actin filament.
     *
     * @param f filament that will be bound.
     */
    public void bind(ActinFilament f){
        bound.add(f);
    }

    /**
     * for unbinding filaments.
     *
     * @param f
     */
    public void unbind(ActinFilament f){
        bound.remove(f);
    }


    /**
     * Checks if this filament is crosslinked to the argument filamnet.
     *
     * @param f
     * @return true if bound contains f.
     */
    public boolean isBound(ActinFilament f){
        return bound.contains(f);
    }

    @Override
    public double[] getPosition() {
        return position;
    }

}