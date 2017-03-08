package org.paluchlab.agentcortex.io;

import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.agents.ActinFilament;
import org.paluchlab.agentcortex.agents.MyosinMotor;
import org.paluchlab.agentcortex.interactions.CrosslinkedFilaments;
import org.paluchlab.agentcortex.interactions.MyosinMotorBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All of the data for a single time point.
 *
 * Actin filaments, myosin motors, crosslinker bindings, and crosslinked filaments.
 *
 *
 * Created on 10/21/14.
 */
public class TimePoint {
    final List<ActinFilament> filaments = new ArrayList<>();
    final List<MyosinMotor> motors = new ArrayList<>();
    final List<MyosinMotorBinding> bindings = new ArrayList<>();
    final List<CrosslinkedFilaments> linkers = new ArrayList<>();
    final double time;

    /**
     * Creates a new TimePoint for a
     * @param time
     */
    public TimePoint(double time){
        this.time = time;
    }
    public double getTime(){
        return time;
    }

    public List<ActinFilament> getFilaments(){
        return Collections.unmodifiableList(filaments);
    }

    public List<MyosinMotor> getMotors(){
        return Collections.unmodifiableList(motors);
    }
    public List<MyosinMotorBinding> getBindings(){
        return Collections.unmodifiableList(bindings);
    }
    public List<CrosslinkedFilaments> getLinkers(){
        return Collections.unmodifiableList(linkers);
    }

    public void setModel(CortexModel model){
        for(MyosinMotorBinding binding: getBindings()){
            binding.setModel(model);
        }
        for(CrosslinkedFilaments x: getLinkers()){
            x.setModel(model);
        }
    }

}
