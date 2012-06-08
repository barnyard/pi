cd ../freepastry
ant -Divy.mode=offline -Dtest.skip=y publish
cd ../p2p-core
ant -Divy.mode=offline -Dtest.skip=y publish
cd ../p2p-app
ant  -Divy.mode=offline -Dtest.skip=y  publish
cd ../p2p-networkmanager
ant  -Divy.mode=offline -Dtest.skip=y  publish
cd ../p2p-instancemanager
ant  -Divy.mode=offline -Dtest.skip=y  publish
cd ../p2p-volumemanager
ant  -Divy.mode=offline -Dtest.skip=y  publish
cd ../pi-sss
ant  -Divy.mode=offline -Dtest.skip=y  publish
cd ../p2p-imagemanager
ant  -Divy.mode=offline -Dtest.skip=y  publish
cd ../p2p-api
ant  -Divy.mode=offline -Dtest.skip=y  publish
cd ../pi-ops-website
ant  -Divy.mode=offline -Dtest.skip=y  clean publish
cd ../p2p-integration

