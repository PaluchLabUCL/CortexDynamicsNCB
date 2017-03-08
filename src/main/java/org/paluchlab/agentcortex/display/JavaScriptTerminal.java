package org.paluchlab.agentcortex.display;

import javax.script.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.paluchlab.agentcortex.CortexModel;

/**
 * For interacting with the model from the gui, without using button commands.
 *
 * Created on 6/3/14.
 */
public class JavaScriptTerminal {
    final ScriptEngine engine;
    JTextPane display, input;
    List<String> history = new LinkedList<String>();
    JFrame frame;
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(10);
    List<String> commandHistory = new ArrayList<>();
    int previous = 0;
    public JavaScriptTerminal(CortexModel model){

        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        engine = factory.getScriptEngine();


        Bindings bindings = engine.createBindings();
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        engine.put("model", model);
        new Thread(()->{
            Thread.currentThread().setName("java script loop");
            while(!Thread.interrupted()){
                try {
                    Runnable r = queue.take();
                    try{
                        r.run();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void setDisplay(SwingDisplay sd){
        engine.put("display", sd);
    }

    public void buildUI(){
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.PAGE_AXIS));

        display = new JTextPane();
        display.setEditable(false);
        //display.setPreferredSize(new Dimension(600, 100));
        JScrollPane display_pane = new JScrollPane(display);
        display_pane.setPreferredSize(new Dimension(600, 100));
        root.add(display_pane);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BorderLayout());
        input = new JTextPane();
        JButton button = new JButton("eval");
        JavaScriptTerminal ref = this;
        button.addActionListener(event -> {

            String s = input.getText();
            if(s.isEmpty()){
                return;
            }
            commandHistory.add(s);
            previous = 0;
            input.setText("");
            queue.add(() -> ref.evaluateExpression(s));

        });

        JButton clear = new JButton("clear history");
        clear.addActionListener(evt -> queue.add(() -> {
            EventQueue.invokeLater(() -> display.setText(""));
            history.clear();
        }));

        JButton previousCommand = new JButton("previous command");
        previousCommand.addActionListener(evt->{
            if(commandHistory.size()>0) {
                input.setText(commandHistory.get(commandHistory.size() - previous-1));
                previous = (previous + 1)%commandHistory.size();
            }
        });
        JPanel butt = new JPanel();

        butt.add(button);
        butt.add(clear);
        butt.add(previousCommand);
        bottom.add(butt, BorderLayout.SOUTH);
        JTabbedPane panel = new JTabbedPane();

        bottom.add(new JScrollPane(input), BorderLayout.CENTER);
        root.add(bottom);
        frame = new JFrame("javascript terminal");
        frame.setContentPane(root);
        frame.pack();
    }

    public void setVisible(boolean state){

        //doesn't exist, but asking it to hide?
        if( (!state) && frame==null){
            return;
        }

        if(frame==null){
            buildUI();
        }
        frame.setVisible(state);

    }

    private void evaluateExpression(String s){
        commandHistory.add(s);
        String[] lines = s.split("\n");
        for(String line: lines){
            history.add(line + '\n');
        }

        EventQueue.invokeLater(() -> display.setText(""));

        try{
            engine.eval(s);
        } catch (ScriptException e) {

            StackTraceElement[] elements = e.getStackTrace();
            history.add(e.getMessage() + '\n');
            if(elements.length>0){

                history.add(elements[0].toString() + '\n');

            }

        }
        final StringBuilder build = new StringBuilder();
        for(String string: history){
            build.append(string);
        }

        EventQueue.invokeLater(() -> display.setText(build.toString()));

    }
}
