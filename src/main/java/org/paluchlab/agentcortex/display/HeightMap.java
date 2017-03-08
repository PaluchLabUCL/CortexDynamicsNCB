package org.paluchlab.agentcortex.display;

import org.paluchlab.agentcortex.agents.Rod;
import org.paluchlab.agentcortex.geometry.Box3D;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 *
 * A wait to plot the height of agents.
 *
 * Created on 7/1/14.d
 */
public class HeightMap{
    double scale;
    double origin_x, origin_y;
    double[] heights;
    double OFFSET = 100;
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    int height = 600;
    int width = 600;

    /**
     * New height map with the simulation box used to set scales. Creates a huge array for storing height values.
     *
     * @param simulation size of simulation for scaling in the x-y direction.
     */
    public HeightMap(Box3D simulation){
        heights = new double[height*width];
        scale = width/simulation.getWidth();
        origin_x = simulation.getOriginX();
        origin_y = simulation.getOriginY();
    }

    /**
     * Draws a rod, with a gradient specific to the height along the rod.
     *
     * @param rod
     */
    public void drawRod(Rod rod){
        double proj = Math.sqrt(rod.direction[0]*rod.direction[0] + rod.direction[1]*rod.direction[1])*rod.length;
        int steps = (int)(proj*scale);
        if(steps<2){
            //just draw a circle.
            return;
        }
        double[] front = rod.getPoint(rod.length*0.5);
        double[] back = rod.getPoint(-rod.length*0.5);

        double dx = (front[0] - back[0])/(steps-1);
        double dy = (front[1] - back[1])/(steps-1);
        double dz = (front[2] - back[2])/(steps-1);
        int stroke = (int)(rod.diameter*scale);
        stroke = stroke>0?stroke:1;
        int center = stroke/2;
        double nx = -dy;
        double ny = dx;
        double mag = 1/Math.sqrt(nx*nx + ny*ny);
        nx = nx*mag;
        ny = ny*mag;

        double sm1 = stroke - 1;
        sm1 = sm1==0?1:sm1;

        double f = center==0?rod.diameter:rod.diameter*sm1/(sm1*center - center*center);

        for(int j = 0; j<stroke; j++) {
            double t = f*(j - j*j*1.0/(stroke-1));

            int last_x = (int) (scale * (back[0]  - origin_x ) + nx*(j - center));
            int last_y =  (int) (scale * (back[1]  - origin_y ) + ny*(j - center));

            for (int i = 0; i < steps; i++) {

                int x = (int) (scale * (back[0] + dx * i - origin_x ) + nx*(j - center));
                int y = (int) (scale * (back[1] + dy * i - origin_y ) + ny*(j - center));

                if (x < 0 || x >= width || y < 0 || y >= height) {
                    last_x = x;
                    last_y = y;
                    continue;
                }
                double h = back[2] + dz * i + OFFSET + t;

                if (heights[x + width * y] < h) {
                    heights[x + width * y] = h;
                    if (h < min) min = h;
                    if (h > max) max = h;
                }

                if((last_x -x)!=0&&(last_y-y)!=0){

                    if(last_x>=0&&last_x<width) {
                        if (heights[last_x + width * y] < h) {
                            heights[last_x + width * y] = h;
                        }
                    }

                    if(last_y>=0&&last_y<height) {
                        if (heights[x + width * last_y] < h) {
                            heights[x + width * last_y] = h;
                        }
                    }
                }
                last_x = x;
                last_y = y;

            }
        }
    }
    final static int BACKGROUND = new Color(0, 0, 50).getRGB();

    /**
     * Creates a buffered image and maps the height map to colors.
     *
     * @return
     */
    public BufferedImage getImage(){
        BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double factor = 1/(max - min);
        for(int x = 0; x<width; x++){
            for(int y = 0; y<height; y++){
                double v = heights[x + y*width];
                if(v>0) {
                    int s = (int) (255 * (heights[x + y * width] - min) * factor);
                    s = s<0?0:s>255?s=255:s;
                    Color c = new Color(s, s, s);
                    ret.setRGB(x, height - y - 1, c.getRGB());
                } else{
                    ret.setRGB(x,height - y - 1, BACKGROUND);
                }
            }
        }
        return ret;
    }

}
