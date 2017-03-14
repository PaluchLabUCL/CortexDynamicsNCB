package org.paluchlab.agentcortex.geometry;

import org.junit.Assert;
import org.junit.Test;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.paluchlab.agentcortex.geometry.Line3D.closestApproachPosition;
import static org.paluchlab.agentcortex.geometry.Line3D.cross;
import static org.paluchlab.agentcortex.geometry.Line3D.difference;
import static org.paluchlab.agentcortex.geometry.Line3D.distance;
import static org.paluchlab.agentcortex.geometry.Line3D.dot;
import static org.paluchlab.agentcortex.geometry.Line3D.intersection;
import static org.paluchlab.agentcortex.geometry.Line3D.magnitude;
import static org.paluchlab.agentcortex.geometry.Line3D.rotate;
import static org.paluchlab.agentcortex.geometry.Line3D.smallAngleRotate;


public class Line3DTest{
    final static double TOL = 1e-15;
    final static double APPROX = 1e-6;
    @Test
    public void crossTest(){

        Assert.assertArrayEquals(new double[]{0, 0, -1}, cross(new double[] {0, 1, 0}, new double[]{1, 0, 0}), TOL);
        Assert.assertArrayEquals(new double[]{0, 0, 1}, cross(new double[] {1, 0, 0}, new double[]{0, 1, 0}), TOL);
        Assert.assertArrayEquals(new double[]{1, 0, 0}, cross(new double[] {0, 1, 0}, new double[]{0, 0, 1}), TOL);
        Assert.assertArrayEquals(new double[]{-1, 0, 0}, cross(new double[] {0, 0, 1}, new double[]{0, 1, 0}), TOL);
        Assert.assertArrayEquals(new double[]{0, -1, 0}, cross(new double[] {1, 0, 0}, new double[]{0, 0, 1}), TOL);
        Assert.assertArrayEquals(new double[]{0, 1, 0}, cross(new double[] {0, 0, 1}, new double[]{1, 0, 0}), TOL);

    }

    @Test
    public void rotationTest(){
        System.out.println("start y into -x");
        double[] v = {0, 1, 0};
        double[] axis = {0, 0, 1};
        double angle = Math.PI/2.0;

        Assert.assertArrayEquals(rotate(axis, 0.001, v), smallAngleRotate(axis, 0.001, v), APPROX);

        for(int i = 0; i<4; i++) {
            //v = smallAngleRotate(axis, angle, v);
            v = rotate(axis, angle, v);
        }

        Assert.assertArrayEquals(new double[]{0, 1, 0}, v, TOL);


        System.out.println("start x into -z");
        v = new double[]{1, 0, 0};
        axis = new double[]{0, 1, 0};
        angle = Math.PI/2.0;

        Assert.assertArrayEquals(rotate(axis, 0.001, v), smallAngleRotate(axis, 0.001, v), APPROX);

        for(int i = 0; i<4; i++) {
            v = rotate(axis, angle, v);
        }

        Assert.assertArrayEquals(new double[]{1, 0, 0}, v, TOL);


        System.out.println("start y into z");
        v = new double[]{0, 1, 0};
        axis = new double[]{1, 0, 0};
        angle = Math.PI/2.0;

        Assert.assertArrayEquals(rotate(axis, 0.001, v), smallAngleRotate(axis, 0.001, v), APPROX);

        for(int i = 0; i<4; i++) {
            v = rotate(axis, angle, v);
        }

        Assert.assertArrayEquals(new double[]{0, 1, 0}, v, TOL);


        System.out.println("start x into -z with ||");
        v = new double[]{1, 1, 0};
        axis = new double[]{0, 1, 0};
        angle = Math.PI/2.0;
        Assert.assertArrayEquals(rotate(axis, 0.001, v), smallAngleRotate(axis, 0.001, v), APPROX);

        for(int i = 0; i<4; i++) {
            v = rotate(axis, angle, v);
        }

        Assert.assertArrayEquals(new double[]{1, 1, 0}, v, TOL);


        System.out.println("start y into z with ||");
        v = new double[]{1, 1, 0};
        axis = new double[]{1, 0, 0};
        angle = Math.PI/2.0;
        Assert.assertArrayEquals(rotate(axis, 0.001, v), smallAngleRotate(axis, 0.001, v), APPROX);

        for(int i = 0; i<4; i++) {
            v = rotate(axis, angle, v);
        }

        Assert.assertArrayEquals(new double[]{1, 1, 0}, v, TOL);


        System.out.println("start y into -x with ||");
        v = new double[]{0, 1, 1};
        axis = new double[]{0, 0, 1};
        angle = Math.PI/2.0;
        Assert.assertArrayEquals(rotate(axis, 0.001, v), smallAngleRotate(axis, 0.001, v), APPROX);

        for(int i = 0; i<4; i++) {
            v = rotate(axis, angle, v);
        }

        Assert.assertArrayEquals(new double[]{0, 1, 1}, v, TOL);

    }

    /**
     * Checks the intersections of lines and planes. The line is centered at 1,0,0 and
     * the plane is centered at 0, 0, 0. The angles of the planes normal and the lines direction
     * are scanned and the intersections are compared.
     */
    @Test
    public void intersectionTest(){
        double[] position = new double[] {1, 0, 0};

        double[] plane = new double[]{0,0,0};

        double DTHETA = Math.PI*2/9.0;
        double DPHI = Math.PI*2/4.0;

        for(int j = 0; j<10; j++){
            for(int k = 0; k<5; k++){
                for(int l = 0; l<5; l++){

                    for(int i = 0; i<10; i++){
                        double[] direction = new double[]{
                                Math.sin(DTHETA*j)*Math.cos(DPHI*k),
                                Math.cos(DTHETA*j)*Math.cos(DPHI*k),
                                Math.sin(DPHI*k)
                        };

                        double[] normal = new double[]{
                                Math.sin(DTHETA*i)*Math.cos(DPHI*l),
                                Math.cos(DTHETA*i)*Math.cos(DPHI*l),
                                Math.sin(DPHI*l)
                        };

                        double in = intersection(position, direction, plane, normal);
                        if(in == 0){
                            //the plane passes through the center of the point.
                            Assert.assertEquals(normal[0], 0, TOL);
                        }

                        double[] pt = new double[3];
                        for(int m = 0; m<3; m++){
                            pt[m] = position[m] + in*direction[m];
                        }

                        Assert.assertEquals(0, dot(difference(plane, pt), normal), TOL);

                    }

                }
            }
        }

    }

    /**
     * Given a direction, iterates through angles to check that the distance found, is the same
     * as the distance found by finding the point of closes approach and comparing that to the
     * original point.
     *
     * @param direction direction of line that will be scanned.
     */
    static void scanDirection(double[] direction){
        double[] center = {0,0,0};
        double length = 1;

        for(int i = 0; i<20; i++){
            double plane_x = Math.cos(Math.PI*0.05*i);
            double plane_y = Math.sin(Math.PI*0.05*i);
            for(int j = 0; j<20; j++){
                double xy = Math.cos(0.1*Math.PI*j);
                double z = Math.sin(0.1*Math.PI*j);
                double[] point = new double[]{plane_x*xy, plane_y*xy, z};

                double d = distance(center, direction, length, point);
                double s = closestApproachPosition(center, direction, length, point);
                double[] tip = new double[3];
                double sn;
                if(s>=length*0.5){
                    sn = length*0.5;
                } else if(s<=-length*0.5){
                    sn = -length*0.5;
                } else{
                    sn = s;
                }

                for(int k = 0; k<3; k++){
                    tip[k] = center[k] + sn*direction[k];
                }
                double mag = magnitude(difference(tip, point));
                Assert.assertEquals(mag, d, TOL);


            }
        }

    }

    @Test
    public void closestApproachCheck(){
        //z direction
        scanDirection(new double[]{0,0,1});

        //y direction
        scanDirection(new double[]{0,1,0});

        //x direction
        scanDirection(new double[]{1,0,0});

        //-z direction
        scanDirection(new double[]{0,0,-1});

        //-y direction
        scanDirection(new double[]{0,-1,0});

        //-x direction
        scanDirection(new double[]{-1,0,0});

    }

    @Test
    public void testPreComputedIntercepts() throws IOException {
        String name = "org/paluchlab/agentcortex/geometry/points_and_lines.txt";
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        String s;
        double inputTol = 0.0001;
        double l = 1;
        int count = 0;
        while((s=reader.readLine())!=null){
            String[] data = s.split("\\t");
            if(data.length==11){

                double[] c = {Double.parseDouble(data[0]), Double.parseDouble(data[1]), Double.parseDouble(data[2])};
                double[] n = {Double.parseDouble(data[3]), Double.parseDouble(data[4]), Double.parseDouble(data[5])};
                double[] p = {Double.parseDouble(data[6]), Double.parseDouble(data[7]), Double.parseDouble(data[8])};
                double d = Double.parseDouble(data[9]);
                double loc = Double.parseDouble(data[10]);
                Assert.assertEquals(Line3D.closestApproachPosition(c, n, l, p), loc, inputTol);
                Assert.assertEquals(Line3D.distance(c, n, l, p), d, inputTol);
                count++;

            }
        }
        Assert.assertEquals(count, 1000);
    }



}