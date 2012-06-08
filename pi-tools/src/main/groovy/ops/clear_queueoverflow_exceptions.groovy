// See the wiki for more details https://collaborate.bt.com/wiki/display/CLOUDOS/I+need+to+clear+QueueOverflow+exceptions+from+a+PI+node

priority = appCtx['koalaNode'].getPastryNode().getVars().get(rice.pastry.socket.SocketPastryNodeFactory.PRIORITY_TL)

for (i in priority.nodesWithPendingMessages()) { println "Node:" + i + ", QueueLength:" + priority.queueLength(i) + ", BytesPending:" + priority.bytesPending(i) }

for (i in priority.nodesWithPendingMessages()) { priority.getEntityManager(i).clearState() }

