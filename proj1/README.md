# COMPILATION
From the sources root (src) run:
```shell
../scripts/compile.sh
```

# EXECUTION
All commands should be run in compiled sources directory (src/build).
Make sure the rmiregistry is running in the compiled sources directory as well.
If the rmiregistry service is not available, the peers will still run but won't accept testapp commands.


To start a new peer run:
```
../../scripts/peer.sh <version> <peer_id> <svc_access_point> <mc_addr> <mc_port> <mdb_addr> <mdb_port> <mdr_addr> <mdr_port>
<version> - peer's version (1.0 - without enhancements; 2.0 with enhancements)
<service_access_point> - peer's access point
<mc_addr> <mc_port> - address and port of the multicast control channel
<mdb_addr> <mdb_port> - address and port of multicast backup channel
<mdr_addr> <mdr_port> - address and port of the multicast restore channel
```

To start one of the protocols run:
```
../../scripts/test.sh <peer_ap> BACKUP|RESTORE|DELETE|RECLAIM|STATE [<opnd_1> [<optnd_2]]
<peer_ap> - peer's access point
BACKUP|RESTORE|DELETE|RECLAIM|STATE [<opnd_1> [<optnd_2]] - protocol name and respective arguments
```


# TEST CASES
All test cases are run from the project's root (as a simplification)

To compile run:
```shell
rootscripts/comp.sh
```

To setup run:
```shell
rootscripts/setup.sh
```

To start the peers and the rmiregistry:
```shell
rootscripts/xfce_peer_commands.sh  # (for xfce terminals)
rootscripts/gnome_peer_commands.sh 	# (for gnome terminals)
```

To test one of the protocols:
```shell
rootscripts/test.sh <peer_ap> BACKUP|RESTORE|DELETE|RECLAIM|STATE [<opnd_1> [<optnd_2]]
```

# EXAMPLES

To backup file.txt with a replication degree of 2:
```shell
rootscripts/test.sh ap BACKUP file.txt 2
```

To delete file.txt:
```shell
rootscripts/test.sh ap DELETE file.txt
```

To restore file.txt:
```shell
rootscripts/test.sh ap RESTORE file.txt
```

To reclaim all disk space in a peer:
```shell
rootscripts/test.sh ap RECLAIM 0
```


# STORAGE

PeerID/
├─ stored/
│  ├─ fileHash/
│  │  ├─ chunkID
│  │  ├─ chunkID
├─ restored/
│  ├─ restoredFile
├─ serializedBackup

Each peer's information is stored in a folder with its ID in src/build/peers/peerID.
Inside each peer's folder are the stored, restored directories and the peer's serialized backup (for recovery on crash).
In the stored folder there is a directory for each backed up file with all the backed up chunks.
The restored files are kept in the restored folder. 