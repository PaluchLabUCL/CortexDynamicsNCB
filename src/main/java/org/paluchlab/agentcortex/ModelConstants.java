package org.paluchlab.agentcortex;

/**
 * This class contains all of the constants that will be used. All lengths are 250nm Time is in seconds.
 *
 * Created by msmith on 5/9/14.
 */
public class ModelConstants {
    //initialization
    public int filaments = 2928;
    public int motors = 122;

    //simulation
    public double DT = 1e-3;
    public double WIDTH = 10;
    public double SEED_WIDTH= 10;
    public double THICKNESS = 1.03;
    public double STEPS_PER_SIMULATE=500;
    public double STEPS_PER_FRAME= 5e2;
    public double SUB_STEPS=10000;
    public double RELAXATION_LIMIT = 2;
    public double ERROR_THRESHOLD = 1e-4;
    //parameters
    public double MYOSIN_LENGTH = 0.8; //300nm
    public double MYOSIN_DIAMETER = 0.2; //50nm
    public double MYOSIN_ACTIVE_FORCE = 1;
    public double MYOSIN_ALPHA_S = 1;
    public double MYOSIN_ALPHA = 1;


    public double K_m = 100;
    public double MYOSIN_BIND_LENGTH = 0.2;
    public double MYOSIN_BINDING_TIME = 75;

    public double ACTIN_LENGTH = 2.0;  //500 nm.
    public double ACTIN_DIAMETER = 0.032;//8 nm.
    public double ACTIN_ALPHA = 1.0;
    public double ACTIN_LENGTH_SIGMA = 0.0;

    public double ANGLE_SIGMA = 0.39269908;

    public double CROSS_LINK_LENGTH = 0.2;
    public double CROSS_LINK_BIND_PROBABILITY = .25;

    public double K_x = 100;

}