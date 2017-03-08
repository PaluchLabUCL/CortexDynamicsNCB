package org.paluchlab.agentcortex.geometry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Utilities for working with double[]'s that represent 3D coordinates, vectors,
 * or pairs of double[]s that represent lines.
 *
 * Created by msmith on 4/29/14.
 */
public class Line3D {
    /**
     * a dot b
     * @param a xyz
     * @param b xyz
     * @return a dot b
     */
    public static double dot(double[] a, double[] b){
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    /**
     * a - b
     * @param a xyz
     * @param b xyz
     * @return (a - b)
     */
    public static double[] difference(double[] a, double[] b){
        return new double[]{a[0] - b[0], a[1]-b[1], a[2]-b[2]};
    }

    /**
     *
     * @param a 3d vector {x, y, z}
     * @return |a|
     */
    public static double magnitude(double[] a){
        return Math.sqrt(a[0]*a[0] + a[1]*a[1] + a[2]*a[2]);
    }

    /**
     * The location of the point can be placed in 1 of 3 regions, end point space (x 2) or stalk space.
     *
     *     end point space
     *
     *          |
     *          |
     *          |        stalk region.
     *          |
     *          |
     *
     *      end point space
     *
     * If the point is in end point space then the distance is the distance to the nearest end point.
     *
     * Finds the minimum distance between a line segment, and a point.
     * @param center - center of line segment.
     * @param direction - direction of line segment.
     * @param length - length of line segment.
     * @param point point in question.
     * @return
     */
    public static double distance(double[] center, double[] direction, double length, double[] point){
        //find which zone point lies in.
        double[] r = difference(point, center);
        double proj = dot(direction,r);
        double half = length/2.0;
        if(proj<-half){
            //bottom region of end point space
            double[] to_bottom = new double[]{
                    center[0] - half*direction[0] - point[0],
                    center[1] - half*direction[1] - point[1],
                    center[2] - half*direction[2] - point[2]
            };
            return magnitude(to_bottom);
        } else if(proj>half){
            //top region
            double[] to_top = new double[]{
                    center[0] + half*direction[0] - point[0],
                    center[1] + half*direction[1] - point[1],
                    center[2] + half*direction[2] - point[2]
            };
            return magnitude(to_top);
        } else{

            double[] normal = {r[0] - proj*direction[0], r[1] - proj*direction[1], r[2] - proj*direction[2]};
            return magnitude(normal);
        }

    }

    /**
     * Find the position along a line segment that is closest to a point.
     *
     * @param center of line segment
     * @param direction of line segment
     * @param length of line segment
     * @param point to find relative location.
     * @return
     */
    static public double closestApproachPosition(double[] center, double[] direction, double length, double[] point){
        //find which zone point lies in.
        double[] r = difference(point, center);
        double proj = dot(direction,r);
        double half = length/2.0;
        if(proj<-half){
            return -half;
        } else if(proj>half){
            return half;
        } else{
            return proj;
        }
    }

    /**
     * Finds the position along a line where intersects the provided plane. The distance is the distance
     * along the direction of the line to the point of intersection. This can return NaN.
     *
     * @param center center of line.
     * @param direction direction of line.
     * @param plane center of plane
     * @param normal normal of plane.
     * @return distance along line where intersection occurs.
     */
    static public double intersection(double[] center, double[] direction, double[] plane, double[] normal){

        double unit = dot(direction, normal);
        double[] to_plane = difference(plane, center);
        double proj = dot(to_plane, normal);

        return proj/unit;
    }

    /**
     * Intersection of a sphere and a line segment. The returned values are up to two points of the line segment.
     * The line goes from +lenth/2 to -lenth/2 from the center.
     *
     * @param center - center of the line
     * @param direction - needs to be normalized!!!
     * @param length - length of line segment.
     * @param point - center of sphere.
     * @param radius - radius of sphere
     * @return and array of two s based coordinates.
     */
    static public double[] sphereBounds(double[] center, double[] direction, double length, double[] point, double radius){
        //find which zone point lies in.
        double[] r = difference(point, center);
        double proj = dot(direction,r);
        double half = length/2.0;

        double[] r_perp = { r[0] - proj*direction[0], r[1] - proj*direction[1], r[2] - proj*direction[2]};
        double perp = magnitude(r_perp);
        if(perp>radius){
            return new double[]{};
        }
        List<Double> points = new ArrayList<>(2);
        double captured = Math.sqrt(radius*radius - perp*perp);

        double forward = proj + captured;
        double backward = proj - captured;

        if(forward>=-half && forward<=half){
            points.add(forward);
        }

        if(backward>=-half && backward<=half){
            points.add(backward);
        }

        double[] ret = new double[points.size()];

        for(int i = 0; i<points.size(); i++){
            ret[i] = points.get(i);
        }
        return ret;
    }






    /**
     * Performs a small angle rotation. Assumes that ange is small and that axis dot vector is ~0.
     *
     * @param axis
     * @param angle
     * @param vector
     * @return
     */
    final static double one_sixth = 1.0/6.0;
    static public double[] smallAngleRotate(double[] axis, double angle, double[] vector){
        double sin = angle;
        double[] cross = cross(axis, vector);
        return new double[]{
                vector[0] + cross[0]*sin,
                vector[1] + cross[1]*sin,
                vector[2] + cross[2]*sin
        };

    }


    /**
     * Complete axis angle rotation.
     * @param axis
     * @param angle
     * @param vector
     * @return
     */
    static public double[] rotate(double[] axis, double angle, double[] vector) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double[] cross = cross(axis, vector);
        double dot = dot(axis, vector) * (1 - cos);
        return new double[]{
                vector[0] * cos + cross[0] * sin + axis[0] * dot,
                vector[1] * cos + cross[1] * sin + axis[1] * dot,
                vector[2] * cos + cross[2] * sin + axis[2] * dot
        };

    }


    /**
     * cross product
     *
     * @param a {ax, ay, az}
     * @param b {bx, by, bz}
     * @return a x b
     *
     */
    public static double[] cross(double[] a, double[] b){

        return new double[]{
                a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - b[2]*a[0],
                a[0]*b[1] - a[1]*b[0]

        };

    }

    /**
     * returns a new vector with the same direction, but a magnitude of 1.
     *
     * @param v
     * @return {nx, ny, nz}
     */
    public static double[] normalize(double[] v){

        double mag = v[0]*v[0] + v[1]*v[1] + v[2]*v[2];
        mag = Math.sqrt(mag);

        return new double[]{v[0]/mag, v[1]/mag, v[2]/mag};

    }



}