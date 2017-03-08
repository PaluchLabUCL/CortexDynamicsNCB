package org.paluchlab.agentcortex.interactions;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.agents.ActinFilament;
import org.paluchlab.agentcortex.agents.Crosslinker;
import org.paluchlab.agentcortex.geometry.Line3D;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a crosslinking of two filaments. Keeps track of the time spent bound
 * the locations of the binding, the filaments bound to and the duration to stay
 * bound for.
 *
 * Created by msmith on 5/9/14.
 */
public class CrosslinkedFilaments{
    CortexModel model;
    public ActinFilament a, b;
    Crosslinker link;
    public double a_s, b_s;
    //the time does nothing. It is here to work with saving/loading simulation files.
    public double time;
    public double duration;
    public final static int NORMAL=0;

    public void setModel(CortexModel model){
        this.model = model;
    }

    /**
     * creates an interaction with all of the values determined.
     *
     * @param m model, for get the positions with reflected coordinates.
     * @param a first filament
     * @param b second filament
     * @param link the representative crosslinker
     * @param a_s position on first filament.
     * @param b_s position on second filament.
     * @param duration time to remain bound.
     */
    public CrosslinkedFilaments(CortexModel m, ActinFilament a, ActinFilament b, Crosslinker link, double a_s, double b_s, double duration){
        this.model = m;
        this.a = a;
        this.b = b;
        this.link = link;
        this.a_s = a_s;
        this.b_s = b_s;
        this.duration = duration;
        this.time = 0;
        a.bind(b);
        b.bind(a);

        double[] x1 = a.getPoint(a_s);
        double[] x2 = b.getPoint(b_s);

        double[] x2_close = model.getReflectedPoint(x1, x2);
        link.A = x1;
        link.B = x2_close;
        //topo = new CrossingTopography(x2, x2_close);
    }

    /**
     * Finds the distance between attachment points and calculates the force.
     * Applies the forces to both filaments.
     *
     */
    public void applyForces(){
        double[] x1 = a.getPoint(a_s);
        double[] x2 = b.getPoint(b_s);

        double[] x2_close = model.getReflectedPoint(x1, x2);

        double[] r = Line3D.difference(x2_close, x1);
        double mag = Line3D.magnitude(r);
        double ds = mag - link.length;

        if(mag==0){
            //System.out.println("Cross linker length is zero.");
            return;
        }

        double f = link.K_x * ds/ mag;
        double[] force_a = new double[] {r[0]*f, r[1]*f, r[2]*f, a_s};
        a.applyForce(force_a);
        double[] force_b = new double[] {-r[0]*f, -r[1]*f, -r[2]*f, b_s};
        b.applyForce(force_b);

        link.A = x1;
        link.B = x2_close;


    }

    /**
     * gets the representative crosslink.
     * @return agent
     */
    public Crosslinker getLink() {
        return link;
    }

    /**
     * For saving, current version only has one type.
     * @return 0
     */
    public int getType(){
        return NORMAL;
    }

    /**
     * Spring energy. 1/2 k(l-l0)^2
     * @return f0 l0 units
     */
    public double getEnergy(){
        double[] x1 = a.getPoint(a_s);
        double[] x2 = b.getPoint(b_s);

        double[] x2_close = model.getReflectedPoint(x1, x2);

        double[] r = Line3D.difference(x2_close, x1);
        double mag = Line3D.magnitude(r);
        double v =  mag - link.length;
        v = 0.5*v*v*link.K_x;
        return v;
    }

}
