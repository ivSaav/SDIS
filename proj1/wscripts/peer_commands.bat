@echo off

set version=%1
if [%1]==[] set version="2.0"

start cmd /k "cd build && rmiregistry"
start cmd /k java -cp src/build main.g06.Peer %version% 1 ap 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
start cmd /k java -cp src/build main.g06.Peer %version% 2 ap2 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
start cmd /k java -cp src/build main.g06.Peer %version% 3 ap3 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
start cmd /k java -cp src/build main.g06.Peer %version% 4 ap4 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
