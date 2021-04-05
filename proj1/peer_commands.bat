@echo off
cd src/out
start cmd /k rmiregistry
start cmd /k java main.g06.Peer 1.0 1 ap 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
start cmd /k java main.g06.Peer 1.0 2 ap2 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
start cmd /k java main.g06.Peer 1.0 3 ap3 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446
cd ../..