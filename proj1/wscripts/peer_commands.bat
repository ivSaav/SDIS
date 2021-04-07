@echo off
start cmd /k "cd build && rmiregistry"
start cmd /k java -cp build main.g06.Peer 1.0 1 ap 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
start cmd /k java -cp build main.g06.Peer 1.0 2 ap2 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
start cmd /k java -cp build main.g06.Peer 1.0 3 ap3 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
