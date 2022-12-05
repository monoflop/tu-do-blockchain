#!/bin/bash
cd ..
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar -v --directory testingNodes/node2 node' &
sleep 0.5
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar -v --directory testingNodes/node3 node' &
sleep 0.5
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar -v --directory testingNodes/node4 node' &
