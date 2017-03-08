package org.paluchlab.agentcortex.analysis;

import lightgraph.DataSet;
import lightgraph.Graph;
import lightgraph.GraphLine;
import lightgraph.GraphPoints;
import org.paluchlab.agentcortex.io.SimulationReader;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompts the user for a file, loads all of the lock files in the parent directory of that file and plots the output.
 *
 * Created on 11/5/14.
 */
public class GraphingDirectoryCollector {
    Color[] colors = new Color[]{
            Color.RED, Color.BLUE, Color.BLACK, Color.CYAN, Color.DARK_GRAY, Color.YELLOW, Color.LIGHT_GRAY
    };
    public GraphingDirectoryCollector(){

    }

    final static double sqrt_12 = Math.sqrt(12);
    public void plotDirectories(File starting){

        File[] files = starting.listFiles();
        List<File> locks = new ArrayList<>();
        for(File f: files){

            if(f.getName().endsWith(".lock")){
                locks.add(f);
            }

        }

        //List<double[]> tension = new ArrayList<>();
        //List<double[]> order = new ArrayList<>();

        Graph tension_plot = new Graph();
        Graph order_plot = new Graph();
        Graph q_plot = new Graph();
        Graph xq_plot = new Graph();
        Graph m_plot = new Graph();
        Graph bm_plot = new Graph();
        Graph thickness_plot = new Graph();

        Graph positions_plot = new Graph();
        double[] t_sum=null;
        double[] t_sum_sqd=null;

        double[] m_sum=null;
        double[] m_sum_sqd=null;

        double[] counters=null;


        int file_no = 0;

        double global_q_sum = 0;
        double global_q_sum_sqd = 0;
        double global_t_sum = 0;
        double global_t_sum_sqd = 0;
        double global_counter = 0;
        double thickness_sum = 0;
        double thickness_sum_sqd = 0;
        double[] thickness_values = null;
        double actin_filament_count = 0;
        double actin_filament_counter = 0;
        double crosslinker_number = 0;
        double crosslinker_counter = 0;
        for(File f: locks){
            String label = f.getName().split(Pattern.quote("."))[0];

            SimulationReader r = SimulationReader.fromLockFile(f);

            if(r!=null) {
                //convert from to
                double invSigma0 = 1.0/(r.model.constants.WIDTH*r.model.getT0());

                List<double[]> t = r.generateTensionMeasurements();
                List<double[]> o = r.generateOrderMeasurements();
                List<double[]> average_positions = r.generateAverageFilamentPositions();

                actin_filament_count += r.constants.filaments;
                actin_filament_counter += 1;

                if(t_sum==null){
                    t_sum = new double[t.size()];
                    t_sum_sqd = new double[t.size()];
                    m_sum = new double[t.size()];
                    m_sum_sqd = new double[t.size()];
                    counters = new double[t.size()];
                    thickness_values = new double[t.size()];

                } else if(t_sum.length<t.size()){
                    int n = t.size();
                    t_sum = extendDouble(t_sum, n);
                    t_sum_sqd = extendDouble(t_sum_sqd, n);
                    m_sum = extendDouble(m_sum, n);
                    m_sum_sqd = extendDouble(m_sum_sqd, n);
                    counters = extendDouble(counters, n);
                    thickness_values = extendDouble(thickness_values, n);
                }

                double[] x = new double[t.size()];
                double[] t1 = new double[t.size()];
                double[] t2 = new double[t.size()];
                double[] bound_motors = new double[t.size()];

                double[] o1 = new double[t.size()];
                double[] o2 = new double[t.size()];
                double[] o3 = new double[t.size()];

                double[] q1 = new double[t.size()];
                double[] q2 = new double[t.size()];
                double[] q3 = new double[t.size()];

                double[] xq1 = new double[t.size()];
                double[] xq2 = new double[t.size()];
                double[] xq3 = new double[t.size()];

                double[] m = new double[t.size()];

                double[] pos_x = new double[t.size()];
                double[] pos_y = new double[t.size()];
                double[] pos_z = new double[t.size()];

                for(int i = 0; i<t.size(); i++){
                    double[] row = t.get(i);
                    x[i] = row[0];
                    t1[i] = row[1]*invSigma0;
                    t2[i] = row[3]*invSigma0;
                    bound_motors[i] = row[6];

                    t_sum[i] += t1[i] + t2[i];
                    t_sum_sqd[i] += t1[i]*t1[i] + t2[i]*t2[i];

                    m[i] = row[5];

                    m_sum[i] += m[i];
                    m_sum_sqd[i] += m[i]*m[i];

                    counters[i] += 1;

                    row = o.get(i);
                    o1[i] = row[1];
                    o2[i] = row[2];
                    o3[i] = row[3];

                    q1[i] = row[4];
                    q2[i] = row[8];
                    q3[i] = row[12];

                    xq1[i] = row[5];
                    xq2[i] = row[6];
                    xq3[i] = row[9];

                    double[] average_position = average_positions.get(i);
                    pos_x[i] = average_position[0];
                    pos_y[i] = average_position[1];
                    pos_z[i] = average_position[2];

                    thickness_values[i] = average_position[5];

                    if(i>50) {
                        global_t_sum += t1[i] + t2[i];
                        global_t_sum_sqd +=  t1[i]*t1[i] + t2[i]*t2[i];
                        double qxy = q1[i] + q2[i];

                        global_q_sum += qxy;
                        global_q_sum_sqd += qxy*qxy;
                        global_counter += 1;
                        double z_ave=average_position[2];
                        double z_sig=average_position[5];
                        double tv = Math.sqrt(z_sig*z_sig*12 + 8*z_ave*z_ave);
                        thickness_sum += tv;
                        thickness_sum_sqd += tv*tv;

                        crosslinker_number += average_position[6];
                        crosslinker_counter+=1;
                    }



                }

                DataSet set = tension_plot.addData(x, t1);
                set.setLabel(label + " txx");
                set.setPoints(null);
                Color c = colors[file_no%colors.length];
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 20);
                set.setColor(c);
                set.setLineWidth(1);

                set = tension_plot.addData(x, t2);
                set.setLabel(label + " tyy");
                set.setPoints(null);
                set.setLineWidth(1);
                set.setColor(c);

                set = order_plot.addData(x,o1);
                set.setLabel(label + " px");
                set.setColor(colors[file_no%colors.length]);
                set.setPoints(GraphPoints.crossPlus());

                set = order_plot.addData(x,o2);
                set.setLabel(label + " py");
                set.setColor(colors[file_no%colors.length]);
                set.setPoints(GraphPoints.crossX());

                set = order_plot.addData(x,o3);
                set.setLabel(label + " pz");
                set.setColor(colors[file_no%colors.length]);
                set.setPoints(GraphPoints.hollowTriangles());

                set = q_plot.addData(x,q1);
                set.setLabel(label + " qxx");

                set = q_plot.addData(x,q2);
                set.setLabel(label + " qyy");

                set = q_plot.addData(x,q3);
                set.setLabel(label + " qzz");

                set = xq_plot.addData(x,xq1);
                set.setLabel(label + " qxy");

                set = xq_plot.addData(x,xq2);
                set.setLabel(label + " qxz");

                set = xq_plot.addData(x,xq3);
                set.setLabel(label + " qyz");

                set = m_plot.addData(x, m);
                set.setLabel(label);
                set.setPoints(GraphPoints.dots());
                set.setColor(colors[file_no%colors.length]);
                set.setLine(null);

                set = bm_plot.addData(x, bound_motors);
                set.setLabel(label);
                set.setColor(colors[file_no%colors.length]);
                set.setPoints(null);

                set = positions_plot.addData(x, pos_x);
                set.setLabel("x " + label);
                set.setColor(colors[file_no%colors.length]);
                set.setPoints(null);
                set.setLineWidth(2.0);

                set = positions_plot.addData(x, pos_y);
                set.setLabel("y " + label);
                set.setColor(colors[file_no%colors.length]);
                set.setPoints(null);
                set.setLine(GraphLine.longDashes());
                set.setLineWidth(2.0);

                set = positions_plot.addData(x, pos_z);
                set.setLabel("z " + label);
                set.setColor(colors[file_no%colors.length]);
                set.setPoints(null);
                set.setLine(GraphLine.shortDashes());
                set.setLineWidth(2.0);


                set = thickness_plot.addData(x, thickness_values);
                set.setLabel("thick " + label);
                set.setColor(colors[file_no%colors.length]);
                set.setPoints(null);
                set.setLine(GraphLine.shortDashes());
                set.setLineWidth(2.0);


                //tension.addAll(t);
                //order.addAll(o);
                file_no++;
            }

        }
        double[] times = new double[t_sum.length];
        double dt = 0.5;
        for(int i = 0; i<t_sum.length; i++){
            times[i] = dt*(i + 1) ;
            if(counters[i]>0) {
                t_sum[i] = t_sum[i] / (2 * counters[i]);
                t_sum_sqd[i] = Math.sqrt(t_sum_sqd[i]/(2*counters[i]) - t_sum[i]*t_sum[i]);

                m_sum[i] = m_sum[i]/counters[i];
                m_sum_sqd[i] = Math.sqrt(m_sum_sqd[i]/counters[i] - m_sum[i]*m_sum[i]);

            }
        }

        Graph plot = tension_plot;
        DataSet set = tension_plot.addData(times, t_sum);
        set.setColor(Color.BLUE);
        set.setPoints(null);

        double[] sub = new double[times.length/20];
        double[] std = new double[times.length/20];
        double[] y = new double[times.length/20];
        for(int i = 0; i<sub.length; i++){
            sub[i] = t_sum[20*i];
            std[i] = t_sum_sqd[20*i];
            y[i] = times[20*i];
        }

        set = tension_plot.addData(y, sub);
        set.addYErrorBars(std);
        set.setPoints(GraphPoints.outlinedCircles());
        set.setColor(Color.BLUE);
        set.setLine(null);

        tension_plot.setYRange(0, 1);

        plot.setXLabel("Time(s)");
        plot.setYLabel("Surface Tension (\u03c3 / \u03c3<tspan baseline-shift=\"sub\" font-size=\"8pt\">0</tspan>)");
        plot.setTitle("Tension vs Time");

        tension_plot.show(true, "Tension vs Time");

        plot = order_plot;
        plot.setXLabel("time(s)");
        plot.setYLabel("Average P");
        plot.setTitle("P vs Time");

        order_plot.show(false, "p vs Time");

        plot = q_plot;
        plot.setXLabel("time(s)");
        plot.setYLabel("Average Q");
        plot.setTitle("Average Q vs Time");

        q_plot.show(false, "q vs Time");

        plot = xq_plot;
        plot.setXLabel("time(s)");
        plot.setYLabel("Average Q");
        plot.setTitle("off-diagonal Q vs Time");

        xq_plot.show(false, "cross q vs Time");

        plot = m_plot;
        plot.setXLabel("time(s)");
        plot.setYLabel("Average M");
        plot.setTitle("Order Parameter vs Time");
        set = plot.addData(times, m_sum);
        set.setColor(Color.GREEN);
        set.setPoints(GraphPoints.outlinedTriangles());
        sub = new double[sub.length];
        std = new double[sub.length];
        y = new double[sub.length];
        for(int i = 0; i<sub.length; i++){
            sub[i] = m_sum[20*i];
            std[i] = m_sum_sqd[20*i];
            y[i] = times[20*i];
        }

        set = plot.addData(y, sub);
        set.addYErrorBars(std);
        set.setPoints(null);
        set.setLine(null);
        set.setColor(Color.BLACK);

        //set.addYErrorBars(m_sum_sqd);
        m_plot.show(false, "m-parameter");

        plot = positions_plot;
        plot.setXLabel("time(s)");
        plot.setYLabel("Average position");
        plot.setTitle("Average Positions vs Time");
        plot.show(false);

        plot = thickness_plot;
        plot.setXLabel("time(s)");
        plot.setYLabel("z-position std-deviation");
        plot.setTitle("Thickness Measurement");
        plot.show(false);

        global_q_sum = global_q_sum/global_counter;
        global_q_sum_sqd = Math.sqrt(global_q_sum_sqd/global_counter - global_q_sum*global_q_sum);
        global_t_sum = 0.5*global_t_sum/global_counter;
        global_t_sum_sqd = Math.sqrt(0.5*global_t_sum_sqd/global_counter - global_t_sum*global_t_sum);

        thickness_sum = thickness_sum/global_counter;
        thickness_sum_sqd = Math.sqrt(thickness_sum_sqd/global_counter - thickness_sum*thickness_sum);

        actin_filament_count =actin_filament_count/actin_filament_counter;

        crosslinker_number = crosslinker_number/crosslinker_counter;
        System.out.println("#average tension, std dev, average qxx + qyy, std dev");
        System.out.println(
                global_t_sum + " " + global_t_sum_sqd + " "
                        + global_q_sum + " " + global_q_sum_sqd + " "
                        + thickness_sum + " " + thickness_sum_sqd + " "
                        + actin_filament_count/thickness_sum/250.0 + " " + crosslinker_number/actin_filament_count
                        );

        bm_plot.show(false, "Bound myosins vs Time.");
    }

    public double[] extendDouble(double[] original, int new_length){
        double[] x = new double[new_length];
        System.arraycopy(original, 0, x, 0, original.length);
        return x;
    }
    public static void main(String[] args){
        GraphingDirectoryCollector gdc = new GraphingDirectoryCollector();
        File f = gdc.getFile();
        if(f==null)System.exit(0);
        gdc.plotDirectories(f);
    }

    private File getFile() {
        FileDialog fd = new FileDialog((Frame)null, "select folder with lock files!");
        fd.setMode(FileDialog.LOAD);
        fd.setVisible(true);

        String dir_name = fd.getDirectory();
        if(dir_name!=null) {
            return new File(dir_name);
        } else{
            return null;
        }
    }

}
