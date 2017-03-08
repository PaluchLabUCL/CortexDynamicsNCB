package org.paluchlab.agentcortex;

import lightgraph.Graph;
import org.paluchlab.agentcortex.agents.ActinFilament;
import org.paluchlab.agentcortex.agents.Crosslinker;
import org.paluchlab.agentcortex.agents.MyosinMotor;
import org.paluchlab.agentcortex.display.JavaScriptTerminal;
import org.paluchlab.agentcortex.io.SimulationReader;
import org.paluchlab.agentcortex.io.TimePoint;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class for 'controlling' the model. It starts a single thread that is used for modifying, or executing the simulation.
 *
 * Most of the methods here just call the model version of the same method.
 *
 * Created on 10/22/14.
 */
public class ModelController {
    CortexModel model;
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    /**
     * Starts the main thread.
     * @param m
     */
    public ModelController(CortexModel m){
        model = m;
        startMainThread();
    }

    /**
     * Starts a thread and sets it's name to model loop. Things that modify the model, are posted to this thread. Useful
     * for interacting with the GUI.
     *
     */
    private void startMainThread(){
        new Thread(()->{
            Thread.currentThread().setName("model-loop");
            while(!Thread.currentThread().isInterrupted()){
                try {
                    queue.take().run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Calls model.simulate on the model-loop.
     *
     * @param callback runnable that is called after execution.
     */
    public void simulate(final Runnable callback){
        submit(()->model.simulate(callback));
    }

    /**
     * adds runnable r to the queue.
     *
     * @param r
     */
    public void submit(Runnable r){
        queue.offer(r);
    }

    /**
     * Initializes the simulation.
     */
    public void initialize(){
        submit(()->model.initializeSimulation());
    }

    /**
     * Shows a javascript terminal that can be used to control the simulation.
     *
     * @return
     */
    public JavaScriptTerminal showTerminal() {

        return model.showTerminal();

    }

    /**
     * @return current constants.
     */
    public ModelConstants getConstants(){
        return model.constants;
    }

    /**
     * @return all myosin motors.
     */
    public List<MyosinMotor> getMyosins() {
        return model.getMyosins();
    }

    /**
     * @return all actin filaments.
     */
    public List<ActinFilament> getActin() {
        return model.getActin();
    }

    /**
     *
     * @return all crosslinkers.
     */
    public Collection<Crosslinker> getCrosslinkers() {
        return model.getCrosslinkers();
    }

    /**
     * Prepares forces, primarily used when drawing forces.
     *
     */
    public void prepareForces(){
        submit(model::prepareForces);
    }

    /**
     * Clears forces, primarily used when drawing forces.
     */
    public void clearForces(){
        submit(model::prepareForces);
    }

    /**
     * blocks until the queue is empty.
     *
     * @throws InterruptedException
     */
    public void waitForCompletion() throws InterruptedException {
        final Object lock=new Object();
        synchronized(lock){
            submit(()->{
                synchronized(lock){lock.notifyAll();}
            });
            lock.wait();
        };
    }

    /**
     * see CortexModel for documentation.
     */
    public void scanTensionMeasurement(){
        submit(()->model.scanTensionMeasurement());
    }

    /**
     * see CortexModel for documentation.
     */
    public void setTimePoint(TimePoint timePoint) {
        submit(()->model.setTimePoint(timePoint));
    }

    /**
     * see CortexModel for documentation.
     */
    public void setConstants(ModelConstants constants) {
        submit(()->model.setConstants(constants));
    }

    /**
     * Sets the model for the loaded simulation and all of the interactions to be the displayed model.
     *
     * @param sim
     */
    public void setSimulationModel(SimulationReader sim) {
        submit(()->{
            sim.setModel(model);
        });

    }

    /**
     * see CortexModel for documentation.
     */
    public void plotThickness(){
        submit(model::plotThickness);
    }

    /**
     * see CortexModel for documentation.
     */
    public void clearHistory() {
        submit(model::clearHistory);
    }

    /**
     * Returns the tension plot.
     */
    public Graph getTensionGraph() {
        return model.graphMachine.getTensionPlot();
    }

    /**
     * see CortexModel for documentation.
     */
    public void measureTension() {
        submit(model::measureTension);
    }
}
