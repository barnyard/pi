Simple PI demo project

Running the demo:

1. Compile the code
   "ant compile" or "sh compile.sh"

2. Execute run_multiple_nodes.sh to start multiple nodes. By default it will start 4 nodes starting with port 5551 so if you have anything running on the same port then you'll have to change the ports in the script.
   sh run_mutiple_nodes.sh
   or sh run_multiple_nodes.sh 6 # to start 6 nodes.
   
   Additionally, to start one node at a time run.sh can be used.
   Ex. sh run.sh 127.0.0.1 5555 127.0.0.1:5555   
   Note: For additional nodes change the first port 5555 to something else.
   
   Finally, if using ubuntu you need to update your /etc/hosts file to use 127.0.0.1 as the ip address. Simply change 127.0.1.1 to
   127.0.0.1 in /etc/hosts.

Making Changes to the project: 

3. Edit/create some source code in src/main/java. 

4. goto 1
