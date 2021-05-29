For building and running the programs, please use the ANT build script provided (feel free to add more targets but keep the original ones as well). This will allow us to quickly test your application and eventually replicate the experiments you carried out.


As dependencies, you need java and ant. You can install them on an Ubuntu machine as follows:
 * sudo apt-get update
 * sudo apt-get install openjdk-8-jdk openjdk-8-doc openjdk-8-jre-lib
 * sudo apt-get install ant

To build the project, run `ant` in the same folder where the build.xml is. 


To start the server on the "local machine", you can execute
 * java -jar ./jars/WoCoServer.jar 127.0.0.1 12345 false 1 

After starting the server in one terminal, you can start a client on the same machine by executing:
 * java -jar ./jars/WoCoClient.jar 127.0.0.1 12345 10 1 1 

Both executables can be stopped with Control+C (on linux).



