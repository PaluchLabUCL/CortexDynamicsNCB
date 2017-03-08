package org.paluchlab.agentcortex.analysis;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.agents.Agent;
import org.paluchlab.agentcortex.display.ProjectionPainter;
import org.paluchlab.agentcortex.display.XYProjection;
import org.paluchlab.agentcortex.display.XZProjection;
import org.paluchlab.agentcortex.display.YZProjection;
import org.paluchlab.agentcortex.geometry.Box3D;
import org.paluchlab.agentcortex.io.SimulationReader;
import org.paluchlab.agentcortex.io.TimePoint;

import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

/**
 * Requests the user selects a lock-file and then creates an image stack with the projection of the simulation
 * for each frame in the lock-file. ImageJ is then started and the user can save the resulting stack.
 *
 * Created on 1/11/16.
 */
public class CreateMovieFromLock {
    static int thickness = 200;
    static int height = 600;
    static int width = 600;
    static int border = 5;
    static Color bg = Color.BLACK;

    /**
     * Makes an x-y projection of the network.
     *
     * @param m
     * @return
     */
    public static BufferedImage getXYProjection(CortexModel m){

        Box3D bounds = new Box3D(
                new double[]{0,0,0},
                new double[]{
                        m.constants.WIDTH, m.constants.WIDTH, 3*m.constants.THICKNESS
                }
        );

        ArrayList<Agent> drawing =  new ArrayList<>();
        drawing.addAll(m.getMyosins());
        drawing.addAll(m.getActin());
        drawing.addAll(m.getCrosslinkers());

        final ProjectionPainter painter1 = new XYProjection();
        BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        painter1.setBounds(bounds);
        painter1.depthSort(drawing);
        Graphics2D g2d = ret.createGraphics();
        g2d.setColor(bg);
        g2d.fillRect(0,0, width, height);
        painter1.setGraphics(g2d);

        for(Agent agent: drawing){
            agent.draw(painter1);
        }

        g2d.dispose();
        return ret;
    }

    /**
     * Creates an x-z projection of network.
     *
     * @param m
     * @return
     */
    public static BufferedImage getXZProjection(CortexModel m){

        Box3D bounds = new Box3D(
                new double[]{0,0,0},
                new double[]{
                        m.constants.WIDTH, m.constants.WIDTH, 3*m.constants.THICKNESS
                }
        );

        ArrayList<Agent> drawing =  new ArrayList<>();
        drawing.addAll(m.getMyosins());
        drawing.addAll(m.getActin());
        drawing.addAll(m.getCrosslinkers());

        final ProjectionPainter painter1 = new XZProjection();
        BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        painter1.setBounds(bounds);
        painter1.depthSort(drawing);
        Graphics2D g2d = ret.createGraphics();
        g2d.setColor(bg);
        g2d.fillRect(0,0, width, height);
        painter1.setGraphics(g2d);

        for(Agent agent: drawing){
            agent.draw(painter1);
        }

        g2d.dispose();
        return ret;
    }

    /**
     * Creates a y-z projection of the network.
     *
     * @param m
     * @return
     */
    public static BufferedImage getYZProjection(CortexModel m){

        Box3D bounds = new Box3D(
                new double[]{0,0,0},
                new double[]{
                        m.constants.WIDTH, m.constants.WIDTH, 3*m.constants.THICKNESS
                }
        );

        ArrayList<Agent> drawing =  new ArrayList<>();
        drawing.addAll(m.getMyosins());
        drawing.addAll(m.getActin());

        drawing.addAll(m.getCrosslinkers());

        final ProjectionPainter painter1 = new YZProjection();
        BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        painter1.setBounds(bounds);
        painter1.depthSort(drawing);
        Graphics2D g2d = ret.createGraphics();
        g2d.setColor(bg);
        g2d.fillRect(0,0, width, height);
        painter1.setGraphics(g2d);

        for(Agent agent: drawing){
            agent.draw(painter1);
        }

        g2d.dispose();
        return ret;
    }

    /**
     * Creates all three projections, then draws and rotates them to 1 final image.
     *
     * @param model
     * @return image containing all three projections.
     */
    public static BufferedImage getMovieFrame(CortexModel model){

        BufferedImage xy = getXYProjection(model);
        BufferedImage xz = getXZProjection(model);
        BufferedImage yz = getYZProjection(model);


        BufferedImage xz_crop = new BufferedImage(width, thickness, xy.getType());
        Graphics2D g = (Graphics2D)xz_crop.getGraphics();
        int oy = (height - thickness)/2;
        g.drawImage(xz,0, 0, width, thickness, 0, oy, width, oy+thickness, null);
        g.dispose();

        BufferedImage yz_crop = new BufferedImage(thickness, width, xy.getType());
        g = (Graphics2D)yz_crop.getGraphics();
        g.setTransform(AffineTransform.getQuadrantRotateInstance(1));
        //g.setTransform(AffineTransform.getRotateInstance(Math.PI/4));
        int oa = -(width-thickness)/2;
        g.drawImage(yz, 0, oa, height,oa+thickness,0, -oa, width, -oa+thickness, null);
        //g.drawImage(yz, 0, 0, null);
        g.dispose();

        BufferedImage out = new BufferedImage(width+thickness+border, height + thickness + border, xy.getType());
        g = (Graphics2D)out.getGraphics();
        g.drawImage(xy, 0, 0, null);
        g.drawImage(xz_crop, 0, height + border, null);
        g.drawImage(yz_crop, border+width, 0, null);
        g.dispose();
        return out;
    }


    /**
     * Opens a file dialog to select the lock file, creates an image stack, and then opens imagej to display images.
     *
     * @param args
     */
    public static void main(String[] args){
        FileDialog fd = new FileDialog((Frame)null, "Select lock file!");
        fd.setMode(FileDialog.LOAD);
        fd.setVisible(true);

        File lock = new File(fd.getDirectory(), fd.getFile());
        System.out.println(lock);
        SimulationReader reads = SimulationReader.fromLockFile(lock);
        ImageStack stack = null;

        for(TimePoint tp: reads){
            reads.model.setTimePoint(tp);
            BufferedImage frame = getMovieFrame(reads.model);

            if(stack==null){
                stack = new ImageStack(frame.getWidth(), frame.getHeight());
            }

            stack.addSlice(new ColorProcessor(frame));
        }


        if(stack != null){
            String title = new File(fd.getDirectory()).getName();
            String lockTag = shortLockName(fd.getFile());
            ImageJ.main(args);
            new ImagePlus(title + "-" + lockTag, stack).show();
        }
    }

    final static String lockSuffix = ".lock";
    public static String shortLockName(String lockName){
        int l = lockName.length();
        String r;
        if(lockName.contains(lockSuffix)){
            int start = l - lockSuffix.length();
            if(start>=4){
                r = lockName.substring(start-4, start);
            } else {

                r = lockName.substring(0, start);
            }
        } else{
            if(l>=4){
                r = lockName.substring(l-4, l);
            } else {
                r = lockName;
            }
        }

        return r;
    }
}
