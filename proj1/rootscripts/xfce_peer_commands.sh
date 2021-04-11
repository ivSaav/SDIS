#! /usr/bin/bash

argc=$#
version="2.0"
if (( argc > 0 ))
then
	version=$1
fi

xfce4-terminal -e "cd build && rmiregistry"
xfce4-terminal -e "cd src/build && java main.g06.Peer ${version} 1 ap 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446"
xfce4-terminal -e "cd src/build && java main.g06.Peer ${version} 2 ap2 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446"
xfce4-terminal -e "cd src/build && java main.g06.Peer ${version} 3 ap3 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446"
xfce4-terminal -e "cd src/build && java main.g06.Peer ${version} 4 ap4 225.0.0.0:4444 230.0.0.0:4445 235.0.0.0:4446"
