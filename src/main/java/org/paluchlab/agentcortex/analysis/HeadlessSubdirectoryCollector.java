package org.paluchlab.agentcortex.analysis;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * The purpose of this class is to take a directory of directories. Each directory will be a specific
 * simulation condition that will be analyzed using the same process as the graphing directory collector. It will
 * run headless so that a results plot can be produced.
 *
 * The results are written to the file, "general-output.txt"
 *
 * Created on 2/22/15.
 */
public class HeadlessSubdirectoryCollector {

    /**
     * Looks in the cwd for directories. Each directory that is found, is checked for lock files. If that directory has
     * any lock-files it will be analysed.
     *
     *
     * @param args the first argument passed can be used to set the number of threads used for analysis.
     */
    public static void main(String[] args){

        Path p = Paths.get("./");
        File cwd = p.toFile();
        File[] files = cwd.listFiles();
        Collection<Callable<double[]>> tasks = new ArrayList<>();
        int dirs = 0;
        int locks = 0;
        for(File file: files){

            if(file.isDirectory() && !file.equals(cwd)){
                String[] lock_file_names = file.list((dir, name)->name.endsWith("lock"));
                if(lock_file_names.length>0){

                    tasks.add(() -> {
                        DirectoryCollector dc = new DirectoryCollector();
                        return dc.plotDirectories(file, lock_file_names);
                    });

                    dirs++;
                    locks+=lock_file_names.length;
                }
            }


        }

        System.out.printf("%d directories with: %d total lock files.\n", dirs, locks);
        int threads = 2;
        if(args.length>0){
            try{
                threads = Integer.parseInt(args[0]);
            } catch(NumberFormatException e){

            }
        }


        ExecutorService service = Executors.newFixedThreadPool(threads);
        final List<Future<double[]>> results = new ArrayList<>();
        List<double[]> resultValues = new ArrayList<>(dirs);

        for(Callable<double[]> task: tasks){
            results.add(service.submit(task));
        }

        while(results.size()>0){
            Iterator<Future<double[]>> submittedTasks = results.iterator();
            while(submittedTasks.hasNext()){
                try {
                    double[] values = submittedTasks.next().get(100, TimeUnit.MILLISECONDS);
                    resultValues.add(values);
                    submittedTasks.remove();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                } catch (ExecutionException e) {
                    //broken, but keep working.
                    e.printStackTrace();
                    submittedTasks.remove();
                } catch (TimeoutException e) {
                    //expected.
                }
            }

            service.shutdown();
        }

        resultValues.sort((d1, d2)->Double.compare(d1[0], d2[0]));
        try (BufferedWriter out = Files.newBufferedWriter(new File(cwd, "general-output.txt").toPath(), Charset.forName("UTF8"))) {
            out.write(DirectoryCollector.header);
            for(double[] row: resultValues){
                for (double d : row) {
                        out.write(d + "\t");
                }
                out.write('\n');
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}