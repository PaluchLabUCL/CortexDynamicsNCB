package org.paluchlab.agentcortex.analysis;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.io.SimulationReader;
import org.paluchlab.agentcortex.io.TimePoint;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analysis a top-level directory and finds the average tension contributed by each component for a group of simulations.
 *
 * Created by msmith on 7/13/15.
 */
public class TensionByParts {
    final static String header = "#Nx\tstdNx\tTactin\tstdTactin\tTmyosin\tstdTmyosin\tTlinks\tstdTlinks\tMxy\tstdMxy\tMz\tstdMz\tl\th0\n";

    /**
     *
     * Analysis all of the lock-files in the provided directory and returns a double[] containing the measurements.
     *
     * @param starting directory containing lock-files.
     * @param lock_names names of lock files.
     * @return {
     *            Nx - number of crosslinkers,
     *            stdNx - standard deviation of the number of crosslinkers,
     *            Tactin - Tension in the actin filaments.
     *            stdTactin - Standard deviation of the tension in the actin filaments.
     *            Tmyosin - Tension in myosin motors.
     *            stdTmyosin - Standard deviation of the tension.
     *            Tlinks - tension in the crosslinkers.
     *            stdTlinks - Standard deviation of the tension.
     *            Mxy - myosin dipole in the xy direction.
     *            stdMxy - standard deviation of xy dipole values.
     *            Mz - myosin dipole in the z direction.
     *            stdMz standard deviation.
     *            l - average length of actin filament.
     *            h0 - seeding thickness.
     *
     *         }
     */
    public static double[] plotDirectories(File starting, List<String> lock_names){

        List<File> locks = new ArrayList<>();
        for(String n: lock_names){

            locks.add(new File(starting, n));

        }

        int file_no = 0;
        double actin_t_sum = 0;
        double actin_t_sum_sqd = 0;

        double myosin_t_sum = 0;
        double myosin_t_sum_sqd = 0;

        double xlinkers_t_sum = 0;
        double xlinkers_t_sum_sqd = 0;

        double dxy = 0;
        double dz = 0;
        double dxy_sqd = 0;
        double dz_sqd = 0;
        double nx = 0;
        double nx_sqd = 0;
        double counter = 0;
        double length = -1;
        double thickness = -1;

        for(File f: locks){
            String label = f.getName().split(Pattern.quote("."))[0];

            SimulationReader r = SimulationReader.fromLockFile(f);

            if(r!=null) {
                List<double[]> tensions = new ArrayList<>();
                CortexModel model = r.model;
                for(int i = 0; i<r.getPointCount(); i++){
                    TimePoint tp = r.getTimePoint(i);
                    model.setTimePoint(tp);
                    model.measureTensionByParts(tensions);
                    if(i>=50){
                        double fils = model.getActin().size();
                        double n =  model.getCrosslinkers().size()/fils;
                        nx += n;
                        nx_sqd += n*n;
                    }
                }
                double it0 = 1/(model.constants.WIDTH*model.getT0());
                double it0sqd = it0*it0;



                for(int i = 50; i<tensions.size(); i++){
                    double[] row = tensions.get(i);
                    double a = row[1] + row[2];
                    double as = row[1]*row[1] + row[2]*row[2];
                    double m = row[3] + row[4];
                    double ms = row[3]*row[3] + row[4]*row[4];
                    double x = row[5] + row[6];
                    double xs = row[5]*row[5] + row[6]*row[6];
                    double d = row[7] + row[8];
                    double ds = row[7]*row[7] + row[8]*row[8];

                    actin_t_sum += a*it0;
                    actin_t_sum_sqd += as*it0sqd;
                    myosin_t_sum += m*it0;
                    myosin_t_sum_sqd += ms*it0sqd;
                    xlinkers_t_sum += x*it0;
                    xlinkers_t_sum_sqd += xs*it0sqd;
                    dxy += d;
                    dxy_sqd += ds;
                    dz += row[9];
                    dz_sqd += row[9]*row[9];
                    counter += 2;
                }
                file_no++;
                length = model.constants.ACTIN_LENGTH;
                thickness = model.constants.THICKNESS;
            }

        }
        actin_t_sum = actin_t_sum/counter;
        actin_t_sum_sqd = Math.sqrt(actin_t_sum_sqd/counter - actin_t_sum*actin_t_sum);
        myosin_t_sum = myosin_t_sum/counter;
        myosin_t_sum_sqd = Math.sqrt(myosin_t_sum_sqd/counter - myosin_t_sum*myosin_t_sum);
        xlinkers_t_sum = xlinkers_t_sum/counter;
        xlinkers_t_sum_sqd = Math.sqrt(xlinkers_t_sum_sqd/counter - xlinkers_t_sum*xlinkers_t_sum);
        nx = 2*nx/counter;
        nx_sqd = Math.sqrt(2*nx_sqd/counter - nx*nx);
        dxy = dxy/counter;
        dxy_sqd = Math.sqrt(dxy_sqd/counter - dxy*dxy);
        dz = dz*2/counter;
        dz_sqd = Math.sqrt(dz_sqd*2/counter - dz*dz);

        return new double[]{
                nx,                             //0 xlinkers
                nx_sqd,                         //1 s.dev.
                actin_t_sum,                    //2 actin tension
                actin_t_sum_sqd,                //3 actin tension std
                myosin_t_sum,                   //4 myosin tension
                myosin_t_sum_sqd,               //5 st. dev.
                xlinkers_t_sum,                 //6 xlink tension
                xlinkers_t_sum_sqd,             //7 st. dev.
                dxy,                            //8 myosin dipole (xy)
                dxy_sqd,                        //9 st. dev.
                dz,                             //10 myosin dipole (z)
                dz_sqd,                         //11 st. dev.
                length,                         //length
                thickness                       //h0
        };

    }


    public static void main(String[] args) throws IOException {
        if(args.length < 1){
            System.err.println("need to name input directories eg:");
            System.err.println("TensionByParts directory1 directory2");
            System.exit(-1);
        }
        List<double[]> values = new ArrayList<>();
        for(String topLevel: args) {
            File top = new File(topLevel);
            List<File> bases = Files.list(top.toPath())
                    .map(p->p.toFile())
                    .filter(File::isDirectory)
                    .collect(Collectors.toList());

            for (File base: bases) {
                File[] files = base.listFiles();
                List<String> lockNames = Arrays.stream(files).map(File::getName).filter(n -> n.endsWith("lock")).collect(Collectors.toList());
                values.add(plotDirectories(base, lockNames));
            }
            System.out.println("scanned: " + topLevel);
            PrintStream out = new PrintStream(new FileOutputStream(new File(topLevel + "-parts.txt")));
            out.print(header);
            Collections.sort(values, (d,o)->Double.compare(d[0],o[0]));
            for (double[] row : values) {
                for (double d : row) {
                    out.printf("%f\t", d);
                }
                out.println();
            }

            out.close();
            values.clear();
        }




    }


}
