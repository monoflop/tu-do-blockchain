#!/bin/bash
cd ..
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar -v --directory testingNodes/node1' &
sleep 1.0
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar -v --directory testingNodes/node2' &
sleep 0.5
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar -v --directory testingNodes/node3' &
sleep 0.5
xterm -e bash -c 'java -jar target/tu-blockchain-node-1.0-SNAPSHOT.jar -v --directory testingNodes/node4' &
