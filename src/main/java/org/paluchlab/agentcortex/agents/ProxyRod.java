package org.paluchlab.agentcortex.agents;

/**
 * Copy of the original rod, but the location can be changed. Forces are applied to the original rod.
 * Created by msmith on 10/9/14.
 */
public class ProxyRod extends Rod {
    Rod proxy;
    public ProxyRod( Rod r){
        System.arraycopy(r.position, 0, position, 0, 3);
        System.arraycopy(r.direction, 0, direction, 0, 3);
        length = r.length;
        diameter = r.diameter;
        proxy = r;
        bounds = r.bounds;
    }

    public ProxyRod( Rod r, double[] pos){
        System.arraycopy(pos, 0, position, 0, 3);
        System.arraycopy(r.direction, 0, direction, 0, 3);
        length = r.length;
        diameter = r.diameter;
        proxy = r;
        updateBounds();
    }

    @Override
    public void applyForce(double[] f){
        proxy.applyForce(f);
    }
    
}
