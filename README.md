# CortexDynamicsNCB
Agent based simulation for generating tension in an actin cortex.

## Dependencies
These are the build dependencies required for building and testing the project.

* [Java](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) At least version 8.
* [ImageJ](https://imagej.nih.gov/ij/) For saving images.
* [LightWeightGraphing](https://github.com/odinsbane/light-weight-graphing) For creating graphs.
* [Junit 4](http://junit.org/junit4/) For running tests.

## Running a Simulation

There are three ways to run the simulation. 

- With a graphical display, primarily for debugging.
- Headless for long term and remote execution.
- Restarting a previously executed simulation.

#### Headless simulation
Simulations can take a long time to run, so the primary method to run a simulation is to use the [HeadlessSimulation](src/main/java/org/paluchlab/agentcortex/simulations/HeadlessSimulation.java) class.
```
java -cp CortexDynamics-1.0.jar org.paluchlab.agentcortex.simulations.HeadlessSimulation parameters.txt
```
The `parameters.txt` is optional, it is used to initialize the simulation parameters. 
We have included a sample [parameters.txt](samples/parameters.txt).

The program will immediately try to create two files: `12345678.lock` and `12345678-simulation.txt`.\* After the simulation has been running a sufficient amount of time, two more files will be written: `12345678-rods.dat` and `12345678-interactions.dat`. An explanation of the [output files](#output-files) can be found below.

\**The `12345678` is a label and will most likely be a different number.*

Once the interactions and rod files have been written the simulation can be interrupted or waited until completion.

#### Graphical Simulation.
To start a graphical simulation use the `ActinMeshApp` class.
```
java -cp CortexDynamics-1.0.jar org.paluchlab.agentcortex.ActinMeshApp
```


From the GUI you can load simulations, and or run simulations with graphics.

To run a simulation you first need to initialize it, click on the button `initialize`. Then to simulated, click the button `simulate`.
 
Previously saved simulations can be loaded by selecting **'load simulation'** from the **'file'** menu. A file dialog will open and allow you to select a `.lock` file and view the simulation.

#### Restarting a Simulation.

To restart or continue a previously executed simulation. First delete the lock file, then run:

```
java -cp CortexDynamics-1.0.jar org.paluchlab.agentcortex.simulations.RestoreAndWrite 12345678-simulation.txt
```

That will recreate the lock file and start running the simulation from where it left off.

_If there was an error in writing the files originally [RemoveBrokenTimePoints](src/main/java/org/paluchlab/agentcortex/io/RemoveBrokenTimePoints.java) might be able to repair the file._

#### Analysing Simulation Output

##### Create a movie of a saved simulation, run:
```
java -cp CortexDynamics-1.0.jar org.paluchlab.agentcortex.analysis.CreateMovieFromLock
```
The user will be prompted to select a `.lock` file.

##### Interactively plot values from a directory of saved simulation, run
```
 java -cp CortexDynamics-1.0.jar org.paluchlab.agentcortex.analysis.GraphingDirectoryCollector
 ```
The user will be prompted to select a `.lock` file in a directory that they wish to analyse. All of the lock files
 in that directory will be analysed and the output will be plotted.

##### Tension for a collection of directories.
```
java -cp CortexDynamics-1.0.jar org.paluchlab.agentcortex.analysis.HeadlessSubdirectoryCollector
```
This will look for directories in the current directory that contain lock-files and perform analysis and 
write the results to `genera-output.txt`

##### Analyse the tension by parts.
```
java -cp CortexDynamics-1.0.jar org.paluchlab.agentcortex.analysis.TensionByParts directory1 directory2
```
Each argument, eg directory1, directory2, will be scanned for directories that 
 contain lock-files in them. The output will be written to `directory1-parts.txt` or `dot-parts.txt` if 
 the argument is `.`
## Navigating the source code

The simplest way to understand the flow of the code is to start from [HeadlessSubdirectoryCollector](src/main/java/org/paluchlab/agentcortex/simulations/HeadlessSimulation.java).

The main method for that programs goes through the following steps.

- Creates a new model.
- Loads a parameter file (if provided) which sets constants to the desired output.
- Creates a simulation writer.
- Initializes the simulation.
- Writes the simulation data, now that actin filament lengths are known.
- Loops through the simulation a number of times determined by the parameter file.
  - Steps the simulation one time step.
  - Saves the interaction and rod data.
- exits.



# Output Files
The naming of the files are important, a simulation has four file and a **tag** for an identifier. 
 - {tag}.lock 
 - {tag}-simulation.txt
 - {tag}-rods.dat
 - {tag}-interactions.dat
 
In the previous example the tag was `12345678`. The files can be renamed by changing the tag name in each file.

The `.lock` file just indicates the simulation is running/has been run. 

The `-simulation.txt` is a text file that contains the parameters used for the running simulation, and the length of actin filaments.

The `-interactions.dat` file is a binary file that contains the information for interactions.
 
The `-rods.dat` file is a binary file that contains the positions and directions of all the actin and myosin motors.
