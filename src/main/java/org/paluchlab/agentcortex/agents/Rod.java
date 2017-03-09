package org.paluchlab.agentcortex.agents;

import org.paluchlab.agentcortex.display.Painter3D;
import org.paluchlab.agentcortex.geometry.Box3D;
import org.paluchlab.agentcortex.geometry.Line3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generic rod class, a rod is a fixed length rod that has a position and direction.  It includes methods for
 * updating rod dynamics, applying forces and updating positions and directions accordingly.
 *
 * This includes basic geometric methods for finding intersecting rods.
 *
 * Created on 5/11/14.
 */
public class Rod{
    public double[] torque = new double[3];
    public double[] force = new double[3];

    final public double[] position;
    final public double[] direction;
    public double length;
    public double diameter;

    public double alpha_rotational;
    public double alpha_longitudinal;
    public double alpha_perpendicular;
    public double repulsion;
    Box3D bounds = new Box3D();

    List<double[]> forces = new ArrayList<>();

    /**
     * Create a new rod with empty position and direction.
     */
    public Rod(){
        position = new double[3];
        direction = new double[3];
    }

    /**
     * Calculates the absolute value.
     *
     * @param v
     * @return 0, or a value greater than 0. avoids -0.
     */
    public static double abs(double v){
        return v==0?0: v>0?v:-v;
    }

    /**
     * Finds the distance of closest approach to the argument rod.
     *
     * @param other
     * @return the distance between points of closest approach.
     */
    public double closestApproach(Rod other) {
        double dot = Line3D.dot(direction, other.direction);
        double l_parallel = Math.abs(dot) * other.length;

        double[] r = Line3D.difference(other.position, position);
        double z_norm = Line3D.dot(direction, r);

        double h = 0.5 * length;

        double o = z_norm + l_parallel * 0.5;
        double p = z_norm - l_parallel * 0.5;

        if (p > h) {
            //no stalk space, pure cap.
            return Line3D.distance(
                    other.position,
                    other.direction,
                    other.length,
                    new double[]{position[0] + direction[0] * h, position[1] + direction[1] * h, position[2] + h * direction[2]}
            );
        } else if (o > h && p > -h) {

            //top cap space and some stalk space.
            double l_c = (o - h) * other.length / l_parallel;

            double delta = other.length * 0.5 - l_c * 0.5;

            delta = dot < 0 ? -delta : delta;

            double[] new_center = new double[]{
                    other.position[0] + other.direction[0] * delta,
                    other.position[1] + other.direction[1] * delta,
                    other.position[2] + other.direction[2] * delta
            };

            double cap_distance = Line3D.distance(
                    new_center,
                    other.direction,
                    l_c,
                    new double[]{position[0] + direction[0] * h, position[1] + direction[1] * h, position[2] + h * direction[2]}
            );


            //next step... find region left in stalk.

            //delta is in the opposite direction of the previous delta.
            delta = dot < 0 ? l_c / 2 : -l_c / 2;

            double[] stalk_center = new double[]{
                    other.position[0] + other.direction[0] * delta,
                    other.position[1] + other.direction[1] * delta,
                    other.position[2] + other.direction[2] * delta
            };

            double[] r_stalk = Line3D.difference(stalk_center, position);

            z_norm = Line3D.dot(direction, r_stalk);

            double[] r_perp = new double[]{
                    r_stalk[0] - z_norm * direction[0],
                    r_stalk[1] - z_norm * direction[1],
                    r_stalk[2] - z_norm * direction[2]
            };

            double[] t_perp = new double[]{
                    other.direction[0] - dot * direction[0],
                    other.direction[1] - dot * direction[1],
                    other.direction[2] - dot * direction[2]
            };

            double m = Line3D.magnitude(t_perp);
            double stalk_length = other.length - l_c;
            double l_perp = m * stalk_length;

            double stalk_distance;

            if (m == 0) {
                stalk_distance = Line3D.magnitude(r_perp);
            } else {
                //normalize.
                t_perp[0] = t_perp[0] / m;
                t_perp[1] = t_perp[1] / m;
                t_perp[2] = t_perp[2] / m;

                stalk_distance = Line3D.distance(r_perp, t_perp, l_perp, new double[]{0, 0, 0});
            }


            return stalk_distance < cap_distance ? stalk_distance : cap_distance;

        } else if (o > h && p < -h) {

            //top cap and lengh.
            double l_top = (o - h) * other.length / l_parallel;

            //bottom cap space.
            double l_bottom = -(p + h) * other.length / l_parallel;

            double l_stalk = other.length - l_top - l_bottom;

            double top_delta = other.length * 0.5 - l_top * 0.5;
            double bottom_delta = l_bottom * 0.5 - other.length * 0.5;
            double stalk_delta = (l_bottom - l_top) * 0.5;

            //move in opposite directions if dot is negative.
            top_delta = dot < 0 ? -top_delta : top_delta;
            bottom_delta = dot < 0 ? -bottom_delta : bottom_delta;
            stalk_delta = dot < 0 ? -stalk_delta : stalk_delta;

            double[] top_center = new double[]{
                    other.position[0] + other.direction[0] * top_delta,
                    other.position[1] + other.direction[1] * top_delta,
                    other.position[2] + other.direction[2] * top_delta
            };


            double top_distance = Line3D.distance(
                    top_center,
                    other.direction,
                    l_top,
                    new double[]{position[0] + direction[0] * h, position[1] + direction[1] * h, position[2] + h * direction[2]}
            );


            //next step... find region left in stalk.

            //delta is in the opposite direction of the cap region delta.

            double[] bottom_center = new double[]{
                    other.position[0] + other.direction[0] * bottom_delta,
                    other.position[1] + other.direction[1] * bottom_delta,
                    other.position[2] + other.direction[2] * bottom_delta
            };


            double bottom_distance = Line3D.distance(
                    bottom_center,
                    other.direction,
                    l_bottom,
                    new double[]{position[0] - direction[0] * h, position[1] - direction[1] * h, position[2] - h * direction[2]}
            );

            double[] stalk_center = new double[]{
                    other.position[0] + other.direction[0] * stalk_delta,
                    other.position[1] + other.direction[1] * stalk_delta,
                    other.position[2] + other.direction[2] * stalk_delta
            };

            double[] r_stalk = Line3D.difference(stalk_center, position);

            z_norm = Line3D.dot(direction, r_stalk);

            double[] r_perp = new double[]{
                    r_stalk[0] - z_norm * direction[0],
                    r_stalk[1] - z_norm * direction[1],
                    r_stalk[2] - z_norm * direction[2]
            };


            double[] t_perp = new double[]{
                    other.direction[0] - dot * direction[0],
                    other.direction[1] - dot * direction[1],
                    other.direction[2] - dot * direction[2]
            };

            double m = Line3D.magnitude(t_perp);
            double stalk_distance;
            double l_perp = m * (l_stalk);
            if (m == 0) {
                stalk_distance = Line3D.magnitude(r_perp);
            } else {
                //normalize.
                t_perp[0] = t_perp[0] / m;
                t_perp[1] = t_perp[1] / m;
                t_perp[2] = t_perp[2] / m;

                stalk_distance = Line3D.distance(r_perp, t_perp, l_perp, new double[]{0, 0, 0});
            }
            double caps = top_distance < bottom_distance ? top_distance : bottom_distance;
            return caps < stalk_distance ? caps : stalk_distance;

        } else if (o <= h && p > -h) {
            //only stalk region
            double[] r_perp = new double[]{
                    r[0] - z_norm * direction[0],
                    r[1] - z_norm * direction[1],
                    r[2] - z_norm * direction[2]
            };
            double[] t_perp = new double[]{
                    other.direction[0] - dot * direction[0],
                    other.direction[1] - dot * direction[1],
                    other.direction[2] - dot * direction[2]
            };
            double m = Line3D.magnitude(t_perp);
            double l_perp = m * other.length;

            if (m == 0) {
                return Line3D.magnitude(r_perp);
            }
            //normalize.
            t_perp[0] = t_perp[0] / m;
            t_perp[1] = t_perp[1] / m;
            t_perp[2] = t_perp[2] / m;

            return Line3D.distance(r_perp, t_perp, l_perp, new double[]{0, 0, 0});

        } else if (o > -h) {
            //bottom cap and some stalk.
            double l_c = -(p + h) * other.length / l_parallel;

            double delta = other.length * 0.5 - l_c * 0.5;

            //move in opposite direction as the first case.
            delta = dot > 0 ? -delta : delta;

            double[] new_center = new double[]{
                    other.position[0] + other.direction[0] * delta,
                    other.position[1] + other.direction[1] * delta,
                    other.position[2] + other.direction[2] * delta
            };

            double cap_distance = Line3D.distance(
                    new_center,
                    other.direction,
                    l_c,
                    new double[]{position[0] - direction[0] * h, position[1] - direction[1] * h, position[2] - h * direction[2]}
            );


            //next step... find region left in stalk.

            //delta is in the opposite direction of the cap region delta.
            delta = dot > 0 ? l_c / 2 : -l_c / 2;

            double[] stalk_center = new double[]{
                    other.position[0] + other.direction[0] * delta,
                    other.position[1] + other.direction[1] * delta,
                    other.position[2] + other.direction[2] * delta
            };

            double[] r_stalk = Line3D.difference(stalk_center, position);

            z_norm = Line3D.dot(direction, r_stalk);

            double[] r_perp = new double[]{
                    r_stalk[0] - z_norm * direction[0],
                    r_stalk[1] - z_norm * direction[1],
                    r_stalk[2] - z_norm * direction[2]
            };


            double[] t_perp = new double[]{
                    other.direction[0] - dot * direction[0],
                    other.direction[1] - dot * direction[1],
                    other.direction[2] - dot * direction[2]
            };

            double m = Line3D.magnitude(t_perp);
            double stalk_distance;
            double l_perp = m * (other.length - l_c);
            if (m == 0) {
                stalk_distance = Line3D.magnitude(r_perp);
            } else {
                //normalize.
                t_perp[0] = t_perp[0] / m;
                t_perp[1] = t_perp[1] / m;
                t_perp[2] = t_perp[2] / m;

                stalk_distance = Line3D.distance(r_perp, t_perp, l_perp, new double[]{0, 0, 0});
            }


            return stalk_distance < cap_distance ? stalk_distance : cap_distance;


        } else {
            ////no stalk space, pure cap.
            return Line3D.distance(
                    other.position,
                    other.direction,
                    other.length,
                    new double[]{position[0] - direction[0] * h, position[1] - direction[1] * h, position[2] - h * direction[2]}
            );
        }
    }

    /**
     * Finds the locations of the closest approach along the rods. returns the coordinates
     * as position along the length of the filament.
     *
     * @param other other rod to find location.
     * @return {s, s_o} s - position on this rod. s_o position along other rod.
     */
    public double[] intersections(Rod other){
        double dot = Line3D.dot(direction, other.direction);
        double l_parallel = Math.abs(dot)*other.length;

        //double[] parallel = new double[]{ dot*direction[0], dot*direction[1], dot*direction[2]};
        double[] r = Line3D.difference(other.position, position);
        double z_parallel = Line3D.dot(direction, r);

        double h = 0.5*length;

        double o = z_parallel + l_parallel*0.5;
        double p = z_parallel - l_parallel*0.5;

        double[] ret;
        if(p>h){
            //no stalk space, pure cap.

            ret = new double[]{
                    h,
                    Line3D.closestApproachPosition(
                            other.position,
                            other.direction,
                            other.length,
                            new double[]{position[0] + direction[0]*h, position[1] + direction[1]*h, position[2] + h*direction[2]})
            };

        } else if(o>h && p>-h){

            //top cap space and some stalk space.
            double l_c = (o - h)*other.length/l_parallel;

            double delta = other.length*0.5 - l_c*0.5;

            delta = dot<0?-delta:delta;

            double[] new_center = new double[]{
                    other.position[0] + other.direction[0]*delta,
                    other.position[1] + other.direction[1]*delta,
                    other.position[2] + other.direction[2]*delta
            };

            double cap_distance = Line3D.distance(
                    new_center,
                    other.direction,
                    l_c,
                    new double[]{position[0] + direction[0]*h, position[1] + direction[1]*h, position[2] + h*direction[2]}
            );



            //next step... find region left in stalk.

            //delta is in the opposite direction of the previous delta.
            delta = dot<0?l_c/2:-l_c/2;

            double[] stalk_center = new double[]{
                    other.position[0] + other.direction[0]*delta,
                    other.position[1] + other.direction[1]*delta,
                    other.position[2] + other.direction[2]*delta
            };

            double[] r_stalk = Line3D.difference(stalk_center, position);

            z_parallel = Line3D.dot(direction, r_stalk);

            double[] r_perp = new double[]{
                    r_stalk[0] - z_parallel*direction[0],
                    r_stalk[1] - z_parallel*direction[1],
                    r_stalk[2] - z_parallel*direction[2]
            };

            double[] t_perp = new double[]{
                    other.direction[0] - dot*direction[0],
                    other.direction[1] - dot*direction[1],
                    other.direction[2] - dot*direction[2]
            };

            double m = Line3D.magnitude(t_perp);
            double stalk_length = other.length - l_c;
            double l_perp = m*stalk_length;

            double stalk_distance;

            if(m == 0){
                stalk_distance = Line3D.magnitude(r_perp);
            } else {
                //normalize.
                t_perp[0] = t_perp[0] / m;
                t_perp[1] = t_perp[1] / m;
                t_perp[2] = t_perp[2] / m;

                stalk_distance = Line3D.distance(r_perp, t_perp, l_perp, new double[]{0, 0, 0});
            }

            if(stalk_distance<=cap_distance){
                //if m==0 then use s_stalk is zero, because it can be anywhere along the stalk.
                double s_stalk = 0;
                if(m!=0) {
                    double stalk_perp = Line3D.closestApproachPosition(
                            r_perp,
                            t_perp,
                            l_perp,
                            new double[]{0, 0, 0});

                    //scale the stalk perpendicular s to the actual stalk s .
                    s_stalk = stalk_perp / l_perp * stalk_length;
                }

                //displace to the other filament coordinate.
                double other_s = s_stalk + delta;

                double stalk_parallel = h - p;

                //moves to the center of the stalk projection.
                double my_s = (h + p)*0.5;
                if(m!=0) {
                    if(dot<0){
                        my_s -= s_stalk*stalk_parallel/stalk_length;
                    } else {
                        my_s += s_stalk * stalk_parallel / stalk_length;
                    }
                }
                ret = new double[]{my_s, other_s};

            } else{

                ret = new double[]{
                        h,
                        Line3D.closestApproachPosition(
                                other.position,
                                other.direction,
                                other.length,
                                getPoint(h)
                        )
                };

            }

        } else if(o>h && p<-h){
            //All three regions are occupied.

            //top cap length.
            double l_top = (o-h)*other.length/l_parallel;

            //bottom cap space.
            double l_bottom = -(p + h)*other.length/l_parallel;

            double l_stalk = other.length - l_top - l_bottom;

            double top_delta = other.length*0.5 - l_top*0.5;
            double bottom_delta = l_bottom*0.5 - other.length*0.5;
            double stalk_delta = (l_bottom - l_top)*0.5;

            //move in opposite directions if dot is negative.
            top_delta = dot<0?-top_delta:top_delta;
            bottom_delta = dot<0?-bottom_delta:bottom_delta;
            stalk_delta = dot<0?-stalk_delta:stalk_delta;

            double[] top_center = new double[]{
                    other.position[0] + other.direction[0]*top_delta,
                    other.position[1] + other.direction[1]*top_delta,
                    other.position[2] + other.direction[2]*top_delta
            };



            double top_distance = Line3D.distance(
                    top_center,
                    other.direction,
                    l_top,
                    new double[]{position[0] + direction[0]*h, position[1] + direction[1]*h, position[2] + h*direction[2]}
            );



            //next step... find region left in stalk.

            //delta is in the opposite direction of the cap region delta.

            double[] bottom_center = new double[]{
                    other.position[0] + other.direction[0]*bottom_delta,
                    other.position[1] + other.direction[1]*bottom_delta,
                    other.position[2] + other.direction[2]*bottom_delta
            };


            double bottom_distance = Line3D.distance(
                    bottom_center,
                    other.direction,
                    l_bottom,
                    new double[]{position[0] - direction[0]*h, position[1] - direction[1]*h, position[2] - h*direction[2]}
            );

            double[] stalk_center = new double[]{
                    other.position[0] + other.direction[0]*stalk_delta,
                    other.position[1] + other.direction[1]*stalk_delta,
                    other.position[2] + other.direction[2]*stalk_delta
            };

            double[] r_stalk = Line3D.difference(stalk_center, position);

            z_parallel = Line3D.dot(direction, r_stalk);

            double[] r_perp = new double[]{
                    r_stalk[0] - z_parallel*direction[0],
                    r_stalk[1] - z_parallel*direction[1],
                    r_stalk[2] - z_parallel*direction[2]
            };


            double[] t_perp = new double[]{
                    other.direction[0] - dot*direction[0],
                    other.direction[1] - dot*direction[1],
                    other.direction[2] - dot*direction[2]
            };

            double m = Line3D.magnitude(t_perp);
            double stalk_distance;
            double l_perp = m*(l_stalk);
            if(m == 0){
                stalk_distance = Line3D.magnitude(r_perp);
            } else {
                //normalize.
                t_perp[0] = t_perp[0] / m;
                t_perp[1] = t_perp[1] / m;
                t_perp[2] = t_perp[2] / m;

                stalk_distance = Line3D.distance(r_perp, t_perp, l_perp, new double[]{0, 0, 0});
            }
            double caps = top_distance<bottom_distance?top_distance:bottom_distance;
            if(stalk_distance<=caps){

                double s_stalk = 0;
                //leave it zero if it is just a point in perpendicular space.
                if(m!=0) {

                    //find the s of the line in perpendicular space
                    double stalk_perp = Line3D.closestApproachPosition(
                            r_perp,
                            t_perp,
                            l_perp,
                            new double[]{0, 0, 0});

                    //scale the stalk perpendicular to .
                    s_stalk = stalk_perp / l_perp * l_stalk;
                }


                double other_s = s_stalk + stalk_delta;
                double stalk_parallel = 2*h;

                double my_s = 0.0;
                if(m!=0) {
                    if(dot>0) {
                        my_s += s_stalk * stalk_parallel / l_stalk;
                    } else{
                        my_s -= s_stalk * stalk_parallel / l_stalk;
                    }
                }
                return new double[]{my_s, other_s};


            }else if(top_distance<bottom_distance){
                return new double[]{
                        h,
                        Line3D.closestApproachPosition(
                                other.position,
                                other.direction,
                                other.length,
                                new double[]{position[0] + direction[0]*h, position[1] + direction[1]*h, position[2] + h*direction[2]})
                };

            } else{
                return new double[]{
                        -h,
                        Line3D.closestApproachPosition(
                                other.position,
                                other.direction,
                                other.length,
                                new double[]{position[0] - direction[0]*h, position[1] - direction[1]*h, position[2] - h*direction[2]})
                };

            }
            //caps<stalk_distance?caps:stalk_distance;

        } else if(o<=h && p>-h){
            //only stalk region
            double[] r_perp = new double[]{
                    r[0] - z_parallel*direction[0],
                    r[1] - z_parallel*direction[1],
                    r[2] - z_parallel*direction[2]
            };
            double[] t_perp = new double[]{
                    other.direction[0] - dot*direction[0],
                    other.direction[1] - dot*direction[1],
                    other.direction[2] - dot*direction[2]
            };
            double m = Line3D.magnitude(t_perp);
            double l_perp = m*other.length;


            //if m==0 then use s_stalk is zero, because it can be anywhere along the stalk.
            double s_stalk = 0;
            if(m!=0) {
                //normalize.
                t_perp[0] = t_perp[0]/m;
                t_perp[1] = t_perp[1]/m;
                t_perp[2] = t_perp[2]/m;


                double stalk_perp = Line3D.closestApproachPosition(
                        r_perp,
                        t_perp,
                        l_perp,
                        new double[]{0, 0, 0});

                //scale the stalk perpendicular s to the actual stalk s .
                s_stalk = stalk_perp / l_perp * other.length;
            }

            //displace to the other filament coordinate.
            double other_s = s_stalk;

            double stalk_parallel = o - p;

            //moves to the center of the stalk projection.
            double my_s = (o + p)*0.5;
            if(m!=0) {
                if(dot>0) {
                    my_s += s_stalk * stalk_parallel / other.length;
                } else{
                    my_s -= s_stalk * stalk_parallel/other.length;
                }
            }
            ret = new double[]{my_s, other_s};

        } else if(o>-h){
            //bottom cap and some stalk.
            double l_c = -(p + h)*other.length/l_parallel;

            double delta = other.length*0.5 - l_c*0.5;

            //move in opposite direction as the first case.
            delta = dot>0?-delta:delta;

            double[] new_center = new double[]{
                    other.position[0] + other.direction[0]*delta,
                    other.position[1] + other.direction[1]*delta,
                    other.position[2] + other.direction[2]*delta
            };

            double cap_distance = Line3D.distance(
                    new_center,
                    other.direction,
                    l_c,
                    new double[]{position[0] - direction[0]*h, position[1] - direction[1]*h, position[2] - h*direction[2]}
            );



            //next step... find region left in stalk.

            //delta is in the opposite direction of the cap region delta.
            delta = dot>0?l_c/2:-l_c/2;

            double[] stalk_center = new double[]{
                    other.position[0] + other.direction[0]*delta,
                    other.position[1] + other.direction[1]*delta,
                    other.position[2] + other.direction[2]*delta
            };

            double[] r_stalk = Line3D.difference(stalk_center, position);

            z_parallel = Line3D.dot(direction, r_stalk);

            double[] r_perp = new double[]{
                    r_stalk[0] - z_parallel*direction[0],
                    r_stalk[1] - z_parallel*direction[1],
                    r_stalk[2] - z_parallel*direction[2]
            };


            double[] t_perp = new double[]{
                    other.direction[0] - dot*direction[0],
                    other.direction[1] - dot*direction[1],
                    other.direction[2] - dot*direction[2]
            };

            double m = Line3D.magnitude(t_perp);
            double stalk_length = other.length - l_c;
            double l_perp = m*stalk_length;
            double stalk_distance;
            if(m == 0){
                stalk_distance = Line3D.magnitude(r_perp);
            } else {
                //normalize.
                t_perp[0] = t_perp[0] / m;
                t_perp[1] = t_perp[1] / m;
                t_perp[2] = t_perp[2] / m;

                stalk_distance = Line3D.distance(r_perp, t_perp, l_perp, new double[]{0, 0, 0});
            }

            if(stalk_distance<=cap_distance){
                //if m==0 then use s_stalk is zero, because it can be anywhere along the stalk.
                double s_stalk = 0;
                if(m!=0) {
                    double stalk_perp = Line3D.closestApproachPosition(
                            r_perp,
                            t_perp,
                            l_perp,
                            new double[]{0, 0, 0});

                    //scale the stalk perpendicular s to the actual stalk s .
                    s_stalk = stalk_perp / l_perp * stalk_length;
                }

                //displace to the other filament coordinate.
                double other_s = s_stalk + delta;

                double stalk_parallel = o + h;

                //moves to the center of the stalk projection.
                double my_s = (-h + o)*0.5;
                if(m!=0) {
                    if(dot<0){
                        my_s += -s_stalk * stalk_parallel / stalk_length;
                    } else {
                        my_s += s_stalk * stalk_parallel / stalk_length;
                    }
                }
                ret = new double[]{my_s, other_s};
            } else{
                ret = new double[]{
                        -h,
                        Line3D.closestApproachPosition(
                                other.position,
                                other.direction,
                                other.length,
                                new double[]{position[0] - direction[0]*h, position[1] - direction[1]*h, position[2] - h*direction[2]}
                        )
                };
            }



        } else{
            ////no stalk space, pure cap.
            ret = new double[]{
                    -h,
                    Line3D.closestApproachPosition(
                            other.position,
                            other.direction,
                            other.length,
                            new double[]{position[0] - direction[0]*h, position[1] - direction[1]*h, position[2] - h*direction[2]}
                    )
            };
        }

        return ret;

    }

    /**
     * Treats this rod as a line segment and finds the shortest distance to the supplied point.
     * @param point
     * @return distance from the rod to point.
     */
    public double closestApproach(double[] point){
        return Line3D.distance(position, direction, length, point);
    }


    /**
     * Get the intersections, if any of the point and radius.
     *
     * @param point
     * @param radius
     * @return
     */
    public double[] getIntersections(double[] point, double radius){
        return Line3D.sphereBounds(position, direction, length, point, radius);
    }


    /**
     * Uses the total force and total torque to update the position and direction of the rod.
     *
     */
    public void update(double dt){

        double force_long = Line3D.dot(force, direction);
        if(Double.isNaN(force_long)){
            System.out.println("rod update force is NaN broken");
        }
        position[0] = position[0] + dt*(force_long*direction[0]/alpha_longitudinal + (force[0] - force_long*direction[0])/alpha_perpendicular);
        position[1] = position[1] + dt*(force_long*direction[1]/alpha_longitudinal + (force[1] - force_long*direction[1])/alpha_perpendicular);
        position[2] = position[2] + dt*(force_long*direction[2]/alpha_longitudinal + (force[2] - force_long*direction[2])/alpha_perpendicular);

        double T = Line3D.magnitude(torque);

        if(T>0) {
            double omega = T / alpha_rotational;
            double theta = dt * omega;
            torque[0] = torque[0] / T;
            torque[1] = torque[1] / T;
            torque[2] = torque[2] / T;

            double[] tmp = Line3D.smallAngleRotate(torque, theta, direction);
            direction[0] = tmp[0];
            direction[1] = tmp[1];
            direction[2] = tmp[2];
        }


        forces.clear();
        updateBounds();
    }

    /**
     * See Box3D.updateBounds(Rod)
     *
     * For quickly checking intersecting rods.
     *
     */
    public void updateBounds(){
        bounds.updateBounds(this);
    }

    /**
     * Adds a new force to the list of forces.
     *
     * @param force {fx, fy, fz, s} the force and location of force.
     */
    synchronized public void applyForce(double[] force){
        for(double s: force){
            if(Double.isNaN(s)){
                throw new IllegalArgumentException("Force is NaN!");
            }
        }
        forces.add(force);
    }

    /**
     * Calculates the position along this rod, given the coordinate along the axis.
     * @param s s=0 is the center, s=l/2 or s=-l/2 are the ends.
     *
     * @return {x, y, z}
     */
    public double[] getPoint(double s){
        return new double[]{position[0] + s*direction[0], position[1] + s*direction[1], position[2] + s*direction[2]};
    }

    /**
     * Checks to other rod to see if there is a collision. If there is a collision then a force is applied to both
     * rods.
     *
     * @param other
     * return separation distance or -1 if bounding box does not cont
     */
    public double collide(Rod other){
        if(!bounds.intersects(other.bounds)){
            return -1;
        }
        double minimum = 0.5*(other.diameter + diameter);
        double separation = closestApproach(other);
        if(separation<minimum){
            //System.out.println("touching" + separation);
            //touching!
            double[] intersections = intersections(other);
            double[] a = getPoint(intersections[0]);
            double[] b = other.getPoint(intersections[1]);
            double[] ab = Line3D.difference(b, a);
            double mag = Line3D.magnitude(ab);
            double interference = minimum - separation;
            if(mag==0){

                //TODO
                System.out.println("don't do this!");

            } else{
                double factor = interference*repulsion/mag;
                applyForce(new double[]{
                    -ab[0]*factor,
                    -ab[1]*factor,
                    -ab[2]*factor,
                    intersections[0]
                });

                other.applyForce(new double[]{
                        ab[0]*factor,
                        ab[1]*factor,
                        ab[2]*factor,
                        intersections[1]
                });

            }


        }
        return separation;

    }


    /**
     * Get the forces on both sides, front and back of the slice. The front is when f[3]>s and
     * the back is when f[3]<=s.
     *
     * @param s location of cut
     * @return {back, front}.
     */
    public List<double[]> getTension(double s){
        double[] front = new double[3];
        double[] back = new double[3];
        for(double[] force: forces){
            double dot = Line3D.dot(direction, force);
            if(force[3]>s){
                front[0] += dot*abs(direction[0]);
                front[1] += dot*abs(direction[1]);
                front[2] += dot*abs(direction[2]);
            } else{
                back[0] += -dot*abs(direction[0]);
                back[1] += -dot*abs(direction[1]);
                back[2] += -dot*abs(direction[2]);

            }
        }
        return Arrays.asList(back, front);
    }

    /**
     * Get the forces on both sides, front and back of the slice. The front is when f[3]>s and
     * the back is when f[3]<=s.
     *
     * @param s location of cut
     * @return {back, front}.
     */
    public List<double[]> internalForce(double s){
        double[] front = new double[3];
        double[] back = new double[3];
        for(double[] force: forces){

            if(force[3]>s){
                front[0] += force[0];
                front[1] += force[1];
                front[2] += force[2];
            } else{
                back[0] += force[0];
                back[1] += force[1];
                back[2] += force[2];

            }
        }
        return Arrays.asList(back, front);
    }

    /**
     * for debugging purposes. Can be used to draw forcers.
     *
     * @param painter
     */
    public void drawForces(Painter3D painter){
        double scale = 1;
        painter.setWidth(1);
        for(double[] force: forces){
            double[] origin = getPoint(force[3]);
            double[] end = new double[]{
                    origin[0] + force[0]*scale,
                    origin[1] + force[1]*scale,
                    origin[2] + force[2]*scale
            };

            painter.drawLine(origin, end);


        }

    }

    /**
     * Sums the accumulated forces and torques.
     *
     * @return magnitude of the sum of forces, out of equilibrium forces.
     *
     */
    synchronized public double prepareForces(){
        torque = new double[]{0,0,0};
        force = new double[]{0,0,0};

        for(double[] f: forces){

            force[0] += f[0];
            force[1] += f[1];
            force[2] += f[2];
            double[] t = Line3D.cross(direction, f);
            torque[0] += t[0]*f[3];
            torque[1] += t[1]*f[3];
            torque[2] += t[2]*f[3];


        }

        return Line3D.magnitude(force) + Line3D.magnitude(torque);

    }

    /**
     * clears all of the applied forces.
     */
    public void clearForces(){
        forces.clear();
    }

    /**
     * Sets the position of the supplied rod to have the same values as the supplied double.
     *
     * @param r rod that will be updated.
     * @param d position it will be set to {x, y, z}
     */
    public static void setPosition(Rod r, double[] d){
        System.arraycopy(d, 0, r.position, 0, 3);
    }

    /**
     * Sets the direction of the supplied rod to have the same values as the supplied double.
     *
     * @param r rod that will be updated.
     * @param d direction it will be set to, {nx, ny, nz}
     */
    public static void setDir(Rod r, double[] d){
        System.arraycopy(d, 0, r.direction, 0, 3);
    }

}
