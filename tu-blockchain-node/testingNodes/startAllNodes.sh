#!/bin/bash
cd ..
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar --directory testingNodes/node1 node' &
sleep 0.5
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar --directory testingNodes/node2 node' &
sleep 0.5
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar --directory testingNodes/node3 node' &
sleep 0.5
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar --directory testingNodes/node4 node' &
