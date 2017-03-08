package org.paluchlab.agentcortex.analysis;

import org.paluchlab.agentcortex.io.SimulationReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Analyzes all of the lock files in a directory!
 *
 * Created on 10/23/14.
 */
public class DirectoryCollector {
    final static String header = "#version 1.0\n" +
            "#0 filament length\t" +
            "#1 tension\t" +
            "#2 tension std\t" +
            "#3 qxx + qyy\t" +
            "#4 q std\t" +
            "#5 thickness by centers\t" +
            "#6 t std\t" +
            "#7 actin filament density\t" +
            "#8 x links/filament\t" +
            "#9 xlinks std\t" +
            "#10 q myosin\t" +
            "#11 myosin q std\t" +
            "#12 dipole\t" +
            "#13 dipole std\t" +
            "#14 bound_myosins\t" +
            "#15 sigmaXX\t" +
            "#16 sigmaXXstd\t" +
            "#17 sigmaYY\t" +
            "#18 sigmaYYstd\t" +
            "#19 sigmaZZ\t" +
            "#20 sigmaZZstd\t";

    /**
     * Analysis all of the lockfiles in the provided directories.
     * @param starting directory containing lock files.
     * @param lock_names names of lockfiles.
     * @return {
     *       //0 filament length
     *       //1 tension (scaled!)
     *       //2 tension std (scaled!)
     *       //3 qxx + qyy
     *       //4 q std
     *       //5 thickness by centers
     *       //6 t std
     *       //7 actin filament density
     *       //8 x links/filament
     *       //9 xlinks std
     *       //10 q myosin
     *       //11 myosin q std
     *       //12 dipole
     *       //13 dipole std
     *       //14 bound_myosins
     *       //15 sigmaXX
     *       //16 sigmaXXstd
     *       //17 sigmaYY
     *       //18 sigmaYYstd
     *       //19 sigmaZZ
     *       //20 sigmaZZstd
     *       }
     */
    public double[] plotDirectories(File starting, String[] lock_names){

        List<File> locks = new ArrayList<>();
        for(String n: lock_names){

            locks.add(new File(starting, n));

        }

        int file_no = 0;

        double global_q_sum = 0;
        double global_q_sum_sqd = 0;
        double global_t_sum = 0;
        double global_t_sum_sqd = 0;
        double global_counter = 0;
        double thickness_sum = 0;
        double thickness_sum_sqd = 0;
        double actin_filament_count = 0;
        double actin_filament_counter = 0;
        double crosslinker_number = 0;
        double crosslinker_counter = 0;
        double crosslinker_sqd = 0;
        double bound_myosins = 0;

        double myosin_Q = 0;
        double myosin_Q_sqd = 0;
        double myosin_dipole = 0;
        double myosin_dipole_sqd = 0;

        double[] dipoleII = new double[3];
        double[] dipoleIIsqd = new double[3];

        double myosin_motor_count = 0;

        double LENGTH=1;
        //scale
        double factor;
        for(File f: locks){
            String label = f.getName().split(Pattern.quote("."))[0];

            SimulationReader r = SimulationReader.fromLockFile(f);

            if(r!=null) {

                List<double[]> t = r.generateTensionMeasurements();
                List<double[]> o = r.generateOrderMeasurements();
                List<double[]> average_positions = r.generateAverageFilamentPositions();

                actin_filament_count += r.constants.filaments;
                actin_filament_counter += 1;
                LENGTH=r.model.constants.ACTIN_LENGTH;
                for(int i = 0; i<t.size(); i++){
                    factor = 1.0/(r.model.constants.WIDTH*r.model.getT0());
                    double[] tensionRow = t.get(i);
                    double t1 = tensionRow[1]*factor;
                    double t2 = tensionRow[3]*factor;
                    double fdp = tensionRow[5];

                    double[] row = o.get(i);

                    double q1 = row[4];
                    double q2 = row[8];

                    double[] average_position = average_positions.get(i);

                    if(i>50) {
                        bound_myosins += r.getBoundMyosinHeadCount(i);
                        global_t_sum += t1 + t2;
                        global_t_sum_sqd +=  t1*t1 + t2*t2;
                        double qxy = q1 + q2;

                        global_q_sum += qxy;
                        global_q_sum_sqd += qxy*qxy;
                        global_counter += 1;
                        double z_sig = average_position[5];
                        double z_ave = average_position[2];

                        double tv =  Math.sqrt(z_sig*z_sig*12 + 8*z_ave*z_ave);
                        thickness_sum += tv;
                        thickness_sum_sqd += tv*tv;

                        crosslinker_number += average_position[6];
                        crosslinker_sqd += average_position[6]*average_position[6];
                        crosslinker_counter+=1;

                        double mq = row[13] + row[14];
                        myosin_Q += mq;
                        myosin_Q_sqd += mq*mq;

                        myosin_dipole += fdp;
                        myosin_dipole_sqd += fdp*fdp;
                        for(int j = 0; j<3; j++){
                            dipoleII[j] += tensionRow[8 + j];
                            dipoleIIsqd[j] += tensionRow[8+j]*tensionRow[8+j];
                        }

                        myosin_motor_count++;
                    }
                }
                //tension.addAll(t);
                //order.addAll(o);
                file_no++;
            }

        }

        global_q_sum = global_q_sum/global_counter;
        global_q_sum_sqd = Math.sqrt(global_q_sum_sqd/global_counter - global_q_sum*global_q_sum);
        global_t_sum = 0.5*global_t_sum/global_counter;
        global_t_sum_sqd = Math.sqrt(0.5*global_t_sum_sqd/global_counter - global_t_sum*global_t_sum);

        thickness_sum = thickness_sum/global_counter;
        thickness_sum_sqd = Math.sqrt(thickness_sum_sqd/global_counter - thickness_sum*thickness_sum);

        actin_filament_count =actin_filament_count/actin_filament_counter;

        crosslinker_number = crosslinker_number/crosslinker_counter;
        crosslinker_sqd = Math.sqrt(crosslinker_sqd/crosslinker_counter - crosslinker_number*crosslinker_number);
        myosin_Q = myosin_Q/myosin_motor_count;
        myosin_Q_sqd = Math.sqrt(myosin_Q_sqd/myosin_motor_count - myosin_Q*myosin_Q);
        myosin_dipole = myosin_dipole/myosin_motor_count;
        myosin_dipole_sqd = Math.sqrt(myosin_dipole_sqd/myosin_motor_count - myosin_dipole*myosin_dipole);
        for(int i = 0; i<3; i++){
            dipoleII[i] = dipoleII[i]/myosin_motor_count;
            dipoleIIsqd[i] = Math.sqrt(dipoleIIsqd[i]/myosin_motor_count - dipoleII[i]*dipoleII[i]);
        }

        return new double[]{
                LENGTH,                                      //0 filament length
                global_t_sum,                                //1 tension (scaled!)
                global_t_sum_sqd,                            //2 tension std (scaled!)
                global_q_sum,                                //3 qxx + qyy
                global_q_sum_sqd,                            //4 q std
                thickness_sum,                               //5 thickness by centers
                thickness_sum_sqd,                           //6 t std
                actin_filament_count / thickness_sum / 250.0,//7 actin filament density
                crosslinker_number / actin_filament_count,   //8 x links/filament
                crosslinker_sqd  / actin_filament_count,     //9 xlinks std
                myosin_Q,                                    //10 q myosin
                myosin_Q_sqd,                                //11 myosin q std
                myosin_dipole,                               //12 dipole
                myosin_dipole_sqd,                           //13 dipole std
                bound_myosins/global_counter,                //14 bound_myosins
                dipoleII[0],                                 //15 sigmaXX
                dipoleIIsqd[0],                              //16 sigmaXXstd
                dipoleII[1],                                 //17 sigmaYY
                dipoleIIsqd[1],                              //18 sigmaYYstd
                dipoleII[2],                                 //19 sigmaZZ
                dipoleIIsqd[2]                               //20 sigmaZZstd
        };

    }
}
