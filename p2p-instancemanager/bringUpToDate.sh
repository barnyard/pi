cd ../freepastry
ant clean publish
cd ../p2p-core
ant -Dtest.skip=y clean publish
cd ../p2p-app
ant -Dtest.skip=y clean publish
cd ../p2p-networkmanager
ant -Dtest.skip=y clean publish
cd ../p2p-instancemanager

