package org.paluchlab.agentcortex.agents;

import org.junit.Assert;
import org.junit.Test;
import org.paluchlab.agentcortex.geometry.Line3D;


/**
 * Created by msmith on 4/19/16.
 */
public class RodTest {
    final static double TOL = 1e-15;

    @Test
    public void testZRodDistances() {

        ActinFilament a = new ActinFilament();
        System.arraycopy(new double[]{0,0,0}, 0, a.position, 0, 3);
        System.arraycopy(new double[]{0,0,1}, 0, a.direction, 0, 3);
        a.length = 1;

        testFilamentDistances(a);


    }
    @Test
    public void testXRodDistances() {

        ActinFilament a = new ActinFilament();
        System.arraycopy(new double[]{0,0,0}, 0, a.position, 0, 3);
        System.arraycopy(new double[]{1,0,0}, 0, a.direction, 0, 3);
        a.length = 1;

        testFilamentDistances(a);
    }

    @Test
    public void testYRodDistances() {

        ActinFilament a = new ActinFilament();
        System.arraycopy(new double[]{0,0,0}, 0, a.position, 0, 3);
        System.arraycopy(new double[]{0,1,0}, 0, a.direction, 0, 3);
        a.length = 1;

        testFilamentDistances(a);

    }

    @Test
    public void testXYZRodDistances() {

        ActinFilament a = new ActinFilament();
        double f = 1.0/Math.sqrt(3);
        System.arraycopy(new double[]{0,0,0}, 0, a.position, 0, 3);
        System.arraycopy(new double[]{f,f,f}, 0, a.direction, 0, 3);
        a.length = 1;

        testFilamentDistances(a);

    }


    /**
     * Checks the the closestApproach is transitive, by compare the supplied rod to a vertical rod (+z direction) with
     * a variety of angles.
     *
     * @param origin the rod to be checked.
     */
    public static void testFilamentDistances(Rod origin) {
        for(int j = 0; j<11; j++) {
            Rod b = new Rod();
            Rod.setDir(b, new double[]{0, 0, 1});
            b.length = 1.0;
            double delta = 2 * Math.PI * 0.05;

            for (int i = 0; i < 10; i++) {

                Rod.setPosition(b,new double[]{Math.cos(i * delta), Math.sin(i * delta), 0.4*(j - 5)});

                double d = origin.closestApproach(b);
                double d2 = b.closestApproach(origin);
                Assert.assertEquals(d, d2, TOL);
            }
        }
    }

    @Test
    public void testXZInterSections(){
        Rod a = new Rod();
        System.arraycopy(new double[]{0,0,0}, 0, a.position, 0, 3);
        System.arraycopy(new double[]{1/Math.sqrt(2), 0, 1/Math.sqrt(2)}, 0, a.direction, 0, 3);
        a.length = 1;
        testFilamentIntersections(a);
    }

    @Test
    public void testZYInterSections(){
        Rod a = new Rod();
        System.arraycopy(new double[]{0,0,0}, 0, a.position, 0, 3);
        System.arraycopy(new double[]{0, 1/Math.sqrt(2), 1/Math.sqrt(2)}, 0, a.direction, 0, 3);
        a.length = 1;
        testFilamentIntersections(a);
    }

    @Test
    public void testXYInterSections(){
        Rod a = new Rod();
        System.arraycopy(new double[]{0,0,0}, 0, a.position, 0, 3);
        System.arraycopy(new double[]{1/Math.sqrt(2), 1/Math.sqrt(2), 0}, 0, a.direction, 0, 3);
        a.length = 1;
        testFilamentIntersections(a);
    }


    public static void testFilamentIntersections(Rod origin){
        for(int j = 6; j<11; j++) {
            ActinFilament b = new ActinFilament();
            System.arraycopy(new double[]{0, 0, -1}, 0, b.direction, 0, 3);
            b.length = 0.5;
            double delta = 2 * Math.PI * 0.25;

            for (int i = 0; i < 4; i++) {
                System.arraycopy(new double[]{Math.cos(i * delta), Math.sin(i * delta), 0.4*(j - 5)}, 0, b.position, 0, 3);

                double[] d = origin.intersections(b);
                double[] x = b.intersections(origin);
                double[] o = origin.getPoint(d[0]);
                double[] p = b.getPoint(d[1]);


                double error = Line3D.magnitude(Line3D.difference(o,p)) - origin.closestApproach(b);
                Assert.assertEquals(d[0], x[1], TOL);
                Assert.assertEquals(d[1], x[0], TOL);
                Assert.assertEquals(error, 0, TOL);
            }
        }
    }

}
