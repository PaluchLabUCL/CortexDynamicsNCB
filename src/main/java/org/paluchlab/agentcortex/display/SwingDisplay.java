package org.paluchlab.agentcortex.display;

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.io.SaveDialog;
import ij.process.ColorProcessor;
import lightgraph.Graph;
import org.paluchlab.agentcortex.CortexModel;
import org.paluchlab.agentcortex.ModelController;
import org.paluchlab.agentcortex.agents.Agent;
import org.paluchlab.agentcortex.agents.Rod;
import org.paluchlab.agentcortex.geometry.Box3D;
import org.paluchlab.agentcortex.io.SimulationReader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base JFrame for displaying and controlling a simulation. Mostly for debug purposes.
 *
 * Created on 4/28/14.
 */
public class SwingDisplay implements Runnable{
    private String CWD=".";
    ModelController controller;
    ImagePanel xy_panel, yz_panel, xz_panel, map_panel;
    JCheckBox draw_actin, draw_myosin, draw_crosslinkers, record_images;
    Color background = Color.BLACK;
    JFrame frame;
    SimulationViewer viewer;

    /**
     * Controls the supplied model by creating a new controller.
     *
     * @param model
     */
    public SwingDisplay(CortexModel model){
        this.controller = new ModelController(model);
    }


    ImageStack recording_stack = new ImageStack(600, 600);

    /**
     * Builds gui, and adds listeners to buttons.
     */
    public void run(){
        frame = new JFrame();
        Container content = frame.getContentPane();

        JPanel buttons = new JPanel();

        JButton simulate = new JButton("simulate");
        simulate.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent evt){
           controller.simulate(new Runnable(){public void run(){
               draw();}});
        }});
        JButton initialize = new JButton("initialize");
        initialize.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent evt){
            while(recording_stack.getSize()>0)
                recording_stack.deleteLastSlice();
            controller.initialize();
            controller.submit(new Runnable(){public void run(){
                draw();}});
        }});



        JButton draw_forces = new JButton("draw forces");
        draw_forces.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                controller.submit(
                        new Runnable() {
                            public void run() {
                                drawForces();
                            }
                        });
            }
        });
        final JPanel view = new JPanel();
        final CardLayout cards = new CardLayout();
        view.setLayout(cards);

        JButton xy_view = new JButton("x-y view");
        xy_view.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                cards.show(view, "x-y");
            }
        });

        JButton yz_view = new JButton("y-z view");
        yz_view.addActionListener(
            new ActionListener(){
                  public void actionPerformed(ActionEvent evt){
                      cards.show(view, "y-z");
                  }
              }
        );

        JButton xz_view = new JButton("x-z view");
        xz_view.addActionListener(
            new ActionListener(){
                public void actionPerformed(ActionEvent evt){
                    cards.show(view, "x-z");
                }
            }
        );

        JButton map_view = new JButton("heigh map");
        map_view.addActionListener(
            new ActionListener(){
               public void actionPerformed(ActionEvent evt){
                   cards.show(view, "map");
               }
           }
        );

        JButton tension_scan = new JButton("tension scan");
        tension_scan.addActionListener((evt)->controller.scanTensionMeasurement());

        JButton thickness_scan = new JButton("plot intensity");
        thickness_scan.addActionListener((evt)->controller.plotThickness());

        buttons.setLayout(new GridLayout(3, 3));
        buttons.add(simulate);
        buttons.add(initialize);
        buttons.add(xy_view);
        buttons.add(yz_view);
        buttons.add(xz_view);
        buttons.add(map_view);
        buttons.add(draw_forces);
        buttons.add(tension_scan);
        buttons.add(thickness_scan);
        content.add(buttons, BorderLayout.SOUTH);



        JPanel options = new JPanel();

        draw_actin = new JCheckBox("actin");
        draw_actin.setSelected(true);

        draw_myosin = new JCheckBox("myosin");
        draw_myosin.setSelected(true);

        draw_crosslinkers = new JCheckBox("xlinkers");
        draw_crosslinkers.setSelected(true);

        record_images = new JCheckBox("record images");
        record_images.setSelected(false);

        options.add(draw_actin);
        options.add(draw_myosin);
        options.add(draw_crosslinkers);
        options.add(record_images);


        options.setLayout(new BoxLayout(options, BoxLayout.PAGE_AXIS));

        content.add(options, BorderLayout.EAST);

        xy_panel = new ImagePanel();
        yz_panel = new ImagePanel();
        xz_panel = new ImagePanel();
        map_panel = new ImagePanel();


        view.add(xy_panel, "x-y");
        view.add(yz_panel, "y-z");
        view.add(xz_panel, "x-z");
        view.add(map_panel, "map");
        view.setMinimumSize(new Dimension(600, 600));
        view.setPreferredSize(new Dimension(600, 600));
        view.setMaximumSize(new Dimension(1000, 1000));

        content.add(view, BorderLayout.CENTER);
        //content.add(xy_panel, BorderLayout.CENTER);

        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("file");
        bar.add(file);

        JMenuItem save_images = new JMenuItem("save images");
        save_images.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent evt){saveImages();}});

        file.add(save_images);

        JMenuItem load_simulation = new JMenuItem("load simulation");
        load_simulation.addActionListener((evt)->loadSimulation());
        file.add(load_simulation);

        JMenu script = new JMenu("script");
        bar.add(script);

        JMenuItem showTerminal = new JMenuItem("show terminal");
        script.add(showTerminal);
        showTerminal.addActionListener((evt)->controller.showTerminal().setDisplay(this));

        frame.setJMenuBar(bar);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    /**
     * Creates a file dialog, and loads a simulation from a lock file if selected.
     *
     */
    private void loadSimulation(){
        JFileChooser c = new JFileChooser();
        c.setDialogTitle("select lock file to load simulation from.");
        if(CWD!=null){
            c.setCurrentDirectory(new File(CWD));
        }
        c.showOpenDialog(frame);
        File out = c.getSelectedFile();

        if(out!=null){
            CWD = c.getCurrentDirectory().getAbsolutePath();
        }

        if(viewer==null){
            viewer = new SimulationViewer(controller, this);
            viewer.prepareGui();
        }
        SimulationReader sim = SimulationReader.fromLockFile(out);
        viewer.setVisible();
        viewer.setSimulationReader(sim);
        controller.setConstants(sim.getConstants());
        controller.setSimulationModel(sim);

    }

    /**
     * for refreshing images.
     *
     */
    public void drawImages(){
        controller.submit(this::draw);
    }

    /**
     * redraws all of the image panels.
     */
    public void draw(){
        Box3D bounds = new Box3D(new double[]{0,0,0}, new double[]{controller.getConstants().WIDTH, controller.getConstants().WIDTH, 3*controller.getConstants().THICKNESS});
        ArrayList<Agent> drawing =  new ArrayList<>();

        if(draw_myosin.isSelected()) {
            drawing.addAll(controller.getMyosins());
        }

        if(draw_actin.isSelected()) {
            drawing.addAll(controller.getActin());
        }

        if(draw_crosslinkers.isSelected()){
            drawing.addAll(controller.getCrosslinkers());
        }

        final ProjectionPainter painter1 = new XYProjection();
        BufferedImage ret = new BufferedImage(600, 600, BufferedImage.TYPE_4BYTE_ABGR);
        painter1.setBounds(bounds);
        painter1.depthSort(drawing);

        Graphics2D g2d = ret.createGraphics();
        g2d.setColor(background);
        g2d.fillRect(0,0, 600, 600);

        painter1.setGraphics(g2d);
        for(Agent agent: drawing){
            agent.draw(painter1);
        }

        g2d.dispose();

        xy_panel.setImage(ret);
        xy_panel.repaint();
        if(record_images.isSelected()){
            recording_stack.addSlice(new ColorProcessor(ret));
        }
        final ProjectionPainter painter2 = new YZProjection();
        ret = new BufferedImage(600, 600, BufferedImage.TYPE_4BYTE_ABGR);
        painter2.setBounds(bounds);
        painter2.depthSort(drawing);

        g2d = ret.createGraphics();
        g2d.setColor(background);
        g2d.fillRect(0,0, 600, 600);

        painter2.setGraphics(g2d);

        for(Agent agent: drawing){
            agent.draw(painter2);
        }

        g2d.dispose();

        yz_panel.setImage(ret);
        yz_panel.repaint();
        if(record_images.isSelected()){
            recording_stack.addSlice(new ColorProcessor(ret));
        }
        ProjectionPainter painter3 = new XZProjection();
        ret = new BufferedImage(600, 600, BufferedImage.TYPE_4BYTE_ABGR);
        painter3.setBounds(bounds);
        painter3.depthSort(drawing);

        g2d = ret.createGraphics();
        g2d.setColor(background);
        g2d.fillRect(0,0, 600, 600);

        painter3.setGraphics(g2d);

        for(Agent agent: drawing){
            agent.draw(painter3);
        }
        g2d.dispose();

        xz_panel.setImage(ret);
        xz_panel.repaint();
        if(record_images.isSelected()){
            recording_stack.addSlice(new ColorProcessor(ret));
        }

        HeightMap map = new HeightMap(bounds);
        List<Rod> rods = new ArrayList<>(drawing.size());
        rods.addAll(controller.getActin());
        rods.addAll(controller.getMyosins());
        for(Rod rod: rods){
            map.drawRod(rod);
        }
        ret = map.getImage();

        map_panel.setImage(ret);
        map_panel.repaint();
    }

    /**
     * redraws, but also draws the forces.
     *
     */
    public void drawForces(){
        controller.prepareForces();
        ArrayList<Agent> drawing =  new ArrayList<>();

        if(draw_myosin.isSelected()) {
            drawing.addAll(controller.getMyosins());
        }

        if(draw_actin.isSelected()) {
            drawing.addAll(controller.getActin());
        }

        if(draw_crosslinkers.isSelected()){
            drawing.addAll(controller.getCrosslinkers());
        }
        Box3D bounds = new Box3D(new double[]{0,0,0}, new double[]{controller.getConstants().WIDTH, controller.getConstants().WIDTH, 3*controller.getConstants().THICKNESS});

        final ProjectionPainter painter1 = new XYProjection();
        painter1.setBounds(bounds);

        BufferedImage ret = new BufferedImage(600, 600, BufferedImage.TYPE_4BYTE_ABGR);

        painter1.depthSort(drawing);

        Graphics2D g2d = ret.createGraphics();
        g2d.setColor(background);
        g2d.fillRect(0,0, 600, 600);

        painter1.setGraphics(g2d);

        for(Agent agent: drawing){
            agent.draw(painter1);
            agent.drawForces(painter1);
        }

        g2d.dispose();

        xy_panel.setImage(ret);
        xy_panel.repaint();
        if(record_images.isSelected()){
            recording_stack.addSlice(new ColorProcessor(ret));
        }
        final ProjectionPainter painter2 = new YZProjection();
        ret = new BufferedImage(600, 600, BufferedImage.TYPE_4BYTE_ABGR);

        painter2.depthSort(drawing);
        painter2.setBounds(bounds);

        g2d = ret.createGraphics();
        g2d.setColor(background);
        g2d.fillRect(0,0, 600, 600);

        painter2.setGraphics(g2d);

        for(Agent agent: drawing){
            agent.draw(painter2);
            agent.drawForces(painter2);
        }


        g2d.dispose();

        yz_panel.setImage(ret);
        yz_panel.repaint();
        if(record_images.isSelected()){
            recording_stack.addSlice(new ColorProcessor(ret));
        }
        ProjectionPainter painter3 = new XZProjection();
        ret = new BufferedImage(600, 600, BufferedImage.TYPE_4BYTE_ABGR);

        painter3.depthSort(drawing);
        painter3.setBounds(bounds);

        g2d = ret.createGraphics();
        g2d.setColor(background);
        g2d.fillRect(0,0, 600, 600);

        painter3.setGraphics(g2d);

        for(Agent agent: drawing){
            agent.draw(painter3);
            agent.drawForces(painter3);
        }

        g2d.dispose();

        xz_panel.setImage(ret);
        xz_panel.repaint();
        if(record_images.isSelected()){
            recording_stack.addSlice(new ColorProcessor(ret));
        }

        controller.clearForces();

    }

    /**
     * Starts this on the EDT.
     *
     */
    public void start(){
        EventQueue.invokeLater(this);
    }

    /**
     * Saves images, if recording images while simulating.
     *
     */
    void saveImages(){
        if(recording_stack.getSize()>0){
            SaveDialog sd = new SaveDialog("save images", CWD, "recording.tif", ".tif");
            String d = sd.getDirectory();
            String n = sd.getFileName();
            if(n!=null){
                CWD = d;
                ImagePlus plus = new ImagePlus("animation",recording_stack);
                FileSaver saver = new FileSaver(plus);
                saver.saveAsTiffStack(new File(d, n).getAbsolutePath());
            }
        }
    }
}

/**
 * A gui used for navigating saved simulation data. When a simulation is opened this will allow a users to navigate
 * between time points or create a movie.
 *
 */
class SimulationViewer{
    ModelController controller;
    JFrame frame;
    SimulationReader reader;
    int index = 0;
    SwingDisplay parent;
    JLabel indexLabel;
    String labelFormat= "%d/%d";
    JCheckBox record;
    List<JButton> buttons = new ArrayList<>();
    SimulationViewer(ModelController mc, SwingDisplay p){
        frame = new JFrame("History Viewer");
        parent = p;
        controller=mc;
    }

    /**
     * Prepare UI.
     */
    public void prepareGui(){
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));

        JButton nextTimePoint = new JButton("next");
        nextTimePoint.addActionListener((evt)->next());
        JButton previousTimePoint = new JButton("previous");
        previousTimePoint.addActionListener((evt)->previous());
        JButton play = new JButton("play");
        record = new JCheckBox("record graphs");

        play.addActionListener((evt)->play());
        content.add(nextTimePoint);
        content.add(previousTimePoint);
        content.add(play);
        content.add(record);

        indexLabel = new JLabel("0/0");
        content.add(indexLabel);
        frame.setContentPane(content);
        frame.pack();
        buttons.add(nextTimePoint);
        buttons.add(previousTimePoint);
        buttons.add(play);
    }

    /**
     * Goes through each time point and displays the image.
     *
     */
    private void play(){
        for(JButton button: buttons){
            button.setEnabled(false);
        }
        controller.clearHistory();
        Graph g = controller.getTensionGraph();
        g.setXRange(0, reader.getPointCount()*0.5);
        g.setYRange(0, 1);
        g.setBackground(Color.BLACK);
        g.setAxisColor(Color.WHITE);
        g.setXTicCount(6);
        g.setYTicCount(2);
        g.setXLabel("Time(s)");
        g.setYLabel("Surface Tension");
        g.resizeGraph(400, 400);
        for(int i = 0; i<reader.getPointCount(); i++){
            controller.setTimePoint(reader.getTimePoint(i));
            parent.drawImages();
            controller.measureTension();
            final int count = i;

            if(record.isSelected()) {
                controller.submit(() -> {

                    try {
                        g.savePng(new File(String.format("tension-%d.png", count)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                });
            }
        }
        controller.submit(()->{
            for(JButton button: buttons){
                button.setEnabled(true);
            }
        });
    }

    /**
     * navigate time points. Previous or wrap.
     */
    private void previous() {
        if(reader!=null){
            if(index>0){
                index = index -1;
            } else{
                index = reader.getPointCount()-1;
            }
            updateDisplay();
        }
    }

    /**
     * navigate time point, next or wrap.
     *
     */
    public void next(){
        if(reader!=null){

            index = (index + 1)%reader.getPointCount();
            updateDisplay();
        }
    }

    /**
     * refresh and redraw.
     */
    void updateDisplay(){
        indexLabel.setText(String.format("%d/%d", index+1, reader.getPointCount()));
        controller.setTimePoint(reader.getTimePoint(index));
        controller.submit(parent::drawImages);
    }

    /**
     * use a new reader.
     *
     * @param reader
     */
    public void setSimulationReader(SimulationReader reader){
        this.reader=reader;
        index = 0;
        updateDisplay();

    }

    /**
     * show this.
     */
    public void setVisible(){
        frame.setVisible(true);
    }

}

/**
 * Simple class for displaying an image in a JPanel.
 */
class ImagePanel extends JPanel{
    Image image;
    public void setImage(BufferedImage img){
        image = img;
        setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
    }
    @Override
    public void paintComponent(Graphics graphics){
        if(image!=null){
            graphics.drawImage(image, 0, 0, this);
        }
    }
}
