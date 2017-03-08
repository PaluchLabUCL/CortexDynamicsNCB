package org.paluchlab.agentcortex.display;

import lightgraph.DataSet;
import lightgraph.Graph;

import java.util.List;
import java.util.Map;

/**
 * A class for managing and producing graphs when the GUI is running.
 *
 * Created on 10/14/14.
 */
public class GraphMachine {
    Graph tension_plot;
    Graph distribution_plot;
    Graph angle_distribution;
    Graph intensities;

    /**
     * Creates a new graph machine and intializes two graphs. tension and distribution plots.
     *
     */
    public GraphMachine(){
        tension_plot = new Graph();
        distribution_plot = new Graph();
    }

    /**
     * Adds a tension point.
     *
     * @param index which value (xz0, xz1, yz0, yz1)
     * @param x time
     * @param y tension.
     */
    public void appendTensionPoint(int index, double x, double y){
        tension_plot.appendPoint(index, x, y);
    }

    /**
     * repaints and scales the tension plot.
     *
     */
    public void refreshTensionPlot() {
        tension_plot.refresh(true);
    }

    /**
     *
     * @param zvalues
     * @param counts
     * @param time
     */
    public void addMyosinDistribution(double[] zvalues, double[] counts, double time) {
        DataSet set = distribution_plot.addData(zvalues, counts);
        set.setLabel(String.format("time: %2.2f", time));
        distribution_plot.refresh(true);
    }

    /**
     * clears all of the stored values in the existing plots and shows them again.
     *
     */
    public void refreshPlots() {
        tension_plot.clearData();
        DataSet set = tension_plot.addData(new double[]{0}, new double[]{0});
        set.setLabel("yz[0]");

        set = tension_plot.addData(new double[]{0}, new double[]{0});
        set.setLabel("yz[1]");

        set = tension_plot.addData(new double[]{0}, new double[]{0});
        set.setLabel("xz[0]");

        set = tension_plot.addData(new double[]{0}, new double[]{0});
        set.setLabel("xz[1]");

        //set = tension_plot.addData(new double[]{0}, new double[]{0});
        //set.setLabel("myosin-dot");

        tension_plot.show(false);

        distribution_plot.clearData();
        distribution_plot.show(false);

        if(angle_distribution!=null){
            angle_distribution.clearData();
        }

    }

    /**
     * Plots the supplied data as histograms between the angles of -pi and pi.
     *
     * @param distributions List of four arrays of values: {x, z-theta, x, xy-theta}
     */
    public void plotAngleDistribution(List<double[]> distributions) {
        if(angle_distribution==null){
            angle_distribution = new Graph();
            angle_distribution.setXRange(-Math.PI, Math.PI);
            angle_distribution.setYRange(0,1);
            angle_distribution.show(false, "Angle Distributions");
        }
        DataSet s = angle_distribution.addData(distributions.get(0), distributions.get(1));
        s.setLabel("z");
        s.setPoints(null);

        s = angle_distribution.addData(distributions.get(2), distributions.get(3));
        s.setLabel("xy");
        s.setPoints(null);

        angle_distribution.refresh(false);
    }

    /**
     *
     *  Shortcut to plot the collection of y values vs a same x values
     *
     * @param s title of window
     * @param x x values
     * @param y collection of yvalues.
     */
    public void plot(String s, double[] x, double[] ... y) {

        Graph g = new Graph();
        for(double[] ys: y){
            g.addData(x, ys);
        }
        g.show(false, s);
    }

    /**
     * Plots the map of y values verses x and labels each curve.
     *
     * @param s window title
     * @param x x-axis values
     * @param labeledValues
     */
    public void plot(String s, double[] x, Map<String, double[]> labeledValues) {

        Graph g = new Graph();
        for(Map.Entry<String, double[]> entry: labeledValues.entrySet()){
            DataSet set = g.addData(x, entry.getValue());
            set.setLabel(entry.getKey());
        }
        g.show(false, s);
    }

    /**
     * returns the tension plot.
     *
     * @return The current tension graph.
     */
    public Graph getTensionPlot() {
        return tension_plot;
    }

    /**
     * Adds an intensity distribution if there is already an intensity distribution plot. Otherwise creates one.
     * Intensities are plotted along the z-axis. to measure thickness.
     *
     * @param zs x-axis of graph.
     * @param intensity intensity values.
     */
    public void addIntensityDistribution(double[] zs, double[] intensity) {
        if(intensities==null){
            intensities = new Graph(zs, intensity);
        } else{
            intensities.addData(zs, intensity);
            intensities.refresh(true);
        }

        intensities.show(false);
    }
}
