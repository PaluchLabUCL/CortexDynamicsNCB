package org.paluchlab.agentcortex.geometry;

import org.paluchlab.agentcortex.agents.Rod;

/**
 * Axis aligned 3D box. Used to consolidate some simple geometry commands.
 *
 * Created on 5/7/14.
 */
public class Box3D {
    double bx, by, bz;
    double cx, cy, cz;

    /**
     * empty box.
     */
    public Box3D(){

    }

    /**
     *
     */
    public Box3D(double[] center, double[] lengths){
        bx = center[0] - lengths[0]*0.5;
        cx = center[0] + lengths[0]*0.5;
        by = center[1] - lengths[1]*0.5;
        cy = center[1] + lengths[1]*0.5;
        bz = center[2] - lengths[2]*0.5;
        cz = center[2] + lengths[2]*0.5;
    }
    /**
     * create a box from a rod.
     *
     * @param r
     */
    public Box3D(Rod r){
        updateBounds(r);
    }

    /**
     * finds a box that contains the rod argument. This method oversizes the box.
     *
     * @param r
     */
    public void updateBounds(Rod r){
        double x1 = r.position[0] - r.length*0.5*r.direction[0];
        double x2 = r.position[0] + r.length*0.5*r.direction[0];
        if(x1>x2){
            cx = x1 + r.diameter;
            bx = x2 - r.diameter;
        } else{
            cx = x2 + r.diameter;
            bx = x1 - r.diameter;
        }

        double y1 = r.position[1] - r.length*0.5*r.direction[1];
        double y2 = r.position[1] + r.length*0.5*r.direction[1];
        if(y1>y2){
            cy = y1 + r.diameter;
            by = y2 - r.diameter;
        } else{
            cy = y2 + r.diameter;
            by = y1 - r.diameter;
        }

        double z1 = r.position[2] - r.length*0.5*r.direction[2];
        double z2 = r.position[2] + r.length*0.5*r.direction[2];
        if(z1>z2){
            cz = z1 + r.diameter;
            bz = z2 - r.diameter;
        } else{
            cz = z2 + r.diameter;
            bz = z1 - r.diameter;
        }
    }

    /**
     * Checks if this box contains the point. Returns true even if the point exactly lies on
     * one of the edges.
     * @param point {x, y, z}
     * @return true if the point is contained within this box.
     */
    public boolean contains(double[] point){
        return point[0]>=bx && point[0]<=cx
            && point[1]>=by && point[1]<=cy
            && point[2]>=bz && point[2]<=cz;
    }

    /**
     * Checks if the other box is contained in this box. If the boxes share an edge/side it can still return true.
     *
     * @param other the other box being checked.
     *
     * @return true if the other box is contained in this one.
     */
    public boolean contains(Box3D other){

        return other.bx>=bx && other.cx <= cx
            && other.by>=by && other.cy <= cy
            && other.bz>=bz && other.cz <= cz;

    }

    /**
     * Check for an intersection. Will return true when there is a shared edge.
     *
     * @param other
     * @return
     */
    public boolean intersects(Box3D other){
        return    checkAxis(bx, cx, other.bx, other.cx)
               && checkAxis(by, cy, other.by, other.cy)
               && checkAxis(bz, cz, other.bz, other.cz);

    }

    boolean checkAxis(double a_low, double a_high, double b_low, double b_high){
        return !(b_high<a_low || b_low>a_high);
    }
    /**
     * if x is contained in [range_low, range_high]
     * @param range_low
     * @param range_high
     * @param x
     * @return
     */
    boolean inRange(double range_low, double range_high, double x){
        return x>=range_low && x<=range_high;
    }

    public double getWidth(){
        return cx -bx;
    }

    public double getHeight(){
        return cy - by;
    }

    public double getDepth(){
        return cz - bz;
    }

    public double getOriginX(){
        return bx;
    }

    public double getOriginY(){
        return by;
    }

    public double getOriginZ(){
        return bz;
    }

    public double getCenterX(){
        return (bx + cx)*0.5;
    }
    public double getCenterY(){
        return (by + cy)*0.5;
    }
    public double getCenterZ(){
        return (bz + cz)*0.5;
    }

}
