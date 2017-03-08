package org.paluchlab.agentcortex.display;

import org.paluchlab.agentcortex.agents.Agent;
import org.paluchlab.agentcortex.geometry.Box3D;
import org.paluchlab.agentcortex.geometry.Line3D;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A painter for projecting a 3D scene onto 2D image and drawing using the java Graphics2D object.
 *
 * Created on 4/30/14.
 */
abstract public class ProjectionPainter implements Painter3D{

    Rectangle2D view;
    Box3D simulation;
    Graphics2D graphics;
    Color color = Color.BLACK;
    double principal_axis = 1;
    double scale_factor;
    List<int[]> translations = new ArrayList<>();

    public ProjectionPainter(){
        view = new Rectangle2D.Double(0,0,600, 600);
        setBounds(new Box3D(new double[]{0,0,0}, new double[]{10, 10, 2}));
    }

    /**
     * Sets the graphis this painter will render too.
     *
     * @param graphics
     */
    public void setGraphics(Graphics2D graphics){
        this.graphics = graphics;
    }


    @Override
    public void drawLine(double[] a, double[] b) {
        translations.clear();
        translations.add(new int[]{0,0});

        int[] p1 = getSceneCoordinates(a);
        int[] p2 = getSceneCoordinates(b);

        if(p1[0]<0||p2[0]<0){
            translations.add(new int[]{(int)view.getWidth(), 0});
        }

        if(p1[0]>view.getWidth()||p2[0]>view.getHeight()){
            translations.add(new int[]{-(int)view.getWidth(), 0});
        }
        int xt = translations.size();

        if(p1[1]<0||p2[1]<0){
            for(int i = 0; i<xt; i++){
                int[] o = translations.get(i);
                translations.add(new int[]{o[0], (int)view.getHeight()});
            }
        }

        if(p1[1]>view.getHeight()||p2[1]<view.getHeight()){
            for(int i = 0; i<xt; i++){
                int[] o = translations.get(i);
                translations.add(new int[]{o[0], -(int)view.getHeight()});
            }
        }



        double[] center = new double[]{0.5*(a[0] + b[0]), 0.5*(a[1] + b[1]), 0.5*(a[2]+b[2])};
            setColor(getDepth(center), graphics);
            for(int[] t: translations) {
                graphics.drawLine(p1[0] + t[0] , (int) view.getHeight() - p1[1] - t[1], p2[0] + t[0], (int) view.getHeight() - p2[1] - t[1]);
            }
    }

    @Override
    public void setWidth(double w){
        float iw = (float)(w*view.getWidth()/principal_axis);
        iw = iw<1?1:iw;

        graphics.setStroke(new BasicStroke(iw, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
    }

    /**
     * making a color brighter.
     *
     * @param v
     * @param color
     * @return
     */
    int brighten(double v, int color){
        double l = v - 1;
        if(l>1){
            l=1;
        }
        double f = color*(1-l) + 255*l;
        return (int)f;
    }

    /**
     * Sets the depth corrected color used for drawing.
     *
     * @param depth
     * @param graphics
     */
    void setColor(double depth, Graphics2D graphics){
        double lerp = (depth + 1)*0.5;
        if(lerp<0){
            graphics.setColor(Color.BLACK);
        } else if(lerp>1.0){

            Color c = new Color(
                    brighten(lerp, color.getRed()),
                    brighten(lerp, color.getGreen()),
                    brighten(lerp, color.getBlue()));
            //graphics.setComposite(AlphaComposite.SrcAtop);
            graphics.setColor(c);
        } else{
            lerp = lerp<0.2?0.2:lerp;
            Color c = new Color(
                    (int)(color.getRed()*lerp),
                    (int)(color.getGreen()*lerp),
                    (int)(color.getBlue()*lerp), 255);
            graphics.setColor(c);
        }

    }

    /**
     * Converts from 3d coordinates to 2D.
     * @param xyz position.
     * @return {x, y}
     */
    abstract public int[] getSceneCoordinates(double[] xyz);

    /**
     *
     * @param xyz position
     * @return the out of plane depth.
     */
    abstract public double getDepth(double[] xyz);

    @Override
    public void drawSphere(double[] center, double radius) {

        double[] a = new double[]{
                center[0] - 0.5*radius,
                center[1] - 0.5*radius,
                center[2] - 0.5*radius
        };

        double[] b = new double[]{
                center[0] + 0.5*radius,
                center[1] + 0.5*radius,
                center[2] + 0.5*radius
        };
        int[] corner = getSceneCoordinates(a);
        int[] opposite = getSceneCoordinates(b);


        translations.clear();
        translations.add(new int[]{0,0});

        if(corner[0]<0){
            translations.add(new int[]{(int)view.getWidth(), 0});
        }

        if(opposite[0]>view.getWidth()){
            translations.add(new int[]{-(int)view.getWidth(), 0});
        }
        int xt = translations.size();

        if(corner[1]<0){
            for(int i = 0; i<xt; i++){
                int[] o = translations.get(i);
                translations.add(new int[]{o[0], (int)view.getHeight()});
            }
        }

        if(opposite[1]>view.getHeight()){
            for(int i = 0; i<xt; i++){
                int[] o = translations.get(i);
                translations.add(new int[]{o[0], -(int)view.getHeight()});
            }
        }

        setColor(getDepth(center), graphics);
        //graphics.fillOval(corner[0], (int)view.getHeight() - opposite[1], opposite[0] - corner[0], opposite[1] - corner[1]);
        for(int[] t: translations) {
            graphics.fillOval(corner[0] + t[0], (int)view.getHeight() - opposite[1] - t[1], opposite[0] - corner[0], opposite[1] - corner[1]);
        }
    }

    /**
     * Sorts the list based on the projected depth. Used for drawing closer objects, above shallower ones.
     * @param list
     */
    public void depthSort(java.util.List<Agent> list){
        java.util.Collections.sort(list, new Comparator<Agent>() {
            public int compare(Agent aa, Agent ab) {
                double[] a = aa.getPosition();
                double[] b = ab.getPosition();
                double da = getDepth(a);
                double db = getDepth(b);
                return Double.compare(da, db);
            }
        });
    }

    /**
     * base color.
     *
     * @param c
     */
    public void setColor(Color c){
        color = c;
    }

    /**
     * sets the scale when changing simulation size.
     *
     * @param bounds
     */
    public void setBounds(Box3D bounds){
        simulation = bounds;
        principal_axis = max(bounds.getWidth(), bounds.getHeight(), bounds.getDepth());
        scale_factor = max(view.getHeight(), view.getWidth())/principal_axis;
    }

    /**
     * draws a projection of a plane as a line.
     * @param center
     * @param normal
     */
    public void drawPlane(double[] center, double[] normal){
        //go through 4 posts and draw the plan. This is for drawing the membrane
        double x0 = simulation.getOriginX();
        double y0 = simulation.getOriginY();
        double z0 = simulation.getCenterZ();

        double[] up = new double[]{0, 0, 1};

        double[] r0 = new double[]{x0, y0, z0};

        double s0 = Line3D.intersection(r0, up, center, normal);

        double x1 = simulation.getWidth() + x0;
        double y1 = y0;
        double z1 = z0;

        double[] r1 = new double[] {x1, y1, z1};

        double s1 = Line3D.intersection(r1, up, center, normal);

        drawLine(new double[]{x0, y0, z0 + s0}, new double[]{x1, y1, z1 + s1});

        double x2 = x1;
        double y2 = y1 + simulation.getHeight();
        double z2 = z1;

        double[] r2 = {x2, y2, z2};
        double s2 = Line3D.intersection(r2, up, center, normal);

        drawLine(new double[]{x1, y1, z1+s1}, new double[]{x2, y2, z2 + s2});

        double x3 = x0;
        double y3 = y2;
        double z3 = z2;

        double[] r3 = {x3, y3, z3};
        double s3 = Line3D.intersection(r3, up, center, normal);

        drawLine(new double[]{x2, y2, z2+s2}, new double[]{x3, y3, z3 + s3});
        drawLine(new double[]{x3, y3, z3+s3}, new double[]{x0, y0, z0+s0});

    }

    /**
     * returns the max of the values passed.
     *
     * @param values
     * @return
     */
    static double max(double ... values){
        double ret = values[0];
        for(int i = 1; i<values.length; i++){
            ret = values[i]>ret?values[i]:ret;
        }
        return ret;
    }

    /**
     * Changes the size of the view.
     *
     * @param rect
     */
    public void setView(Rectangle2D rect){
        this.view = rect;
    }
}
