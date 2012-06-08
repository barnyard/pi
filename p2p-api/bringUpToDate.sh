cd ../freepastry
ant -Dtest.skip=y publish
cd ../p2p-core
ant -Dtest.skip=y publish
cd ../p2p-app
ant -Dtest.skip=y publish
cd ../p2p-networkmanager
ant -Dtest.skip=y publish
cd ../p2p-instancemanager
ant -Dtest.skip=y publish
cd ../p2p-volumemanager
ant -Dtest.skip=y publish
cd ../pi-sss
ant -Dtest.skip=y publish
cd ../p2p-imagemanager
ant -Dtest.skip=y publish
cd ../p2p-api

