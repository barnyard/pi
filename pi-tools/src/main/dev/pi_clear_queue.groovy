/*
 * To run from Groovy Shell
 * telnet localhost 6789
 */

// koalaNode = appCtx['koalaNode']
// pastryNode = appCtx['koalaNode'].getPastryNode()
priority = appCtx['koalaNode'].getPastryNode().getVars().get(rice.pastry.socket.SocketPastryNodeFactory.PRIORITY_TL)
for (i in priority.nodesWithPendingMessages()) { println "Node:" + i + ", QueueLength:" + priority.queueLength(i) + ", BytesPending:" + priority.bytesPending(i) }

// for (i in priority.nodesWithPendingMessages()) { println priority.getPendingMessages(i) }

// Uncomment the following line to clear the queue state. Only do this if you know what you are doing :>
// for (i in priority.nodesWithPendingMessages()) { priority.getEntityManager(i).clearState() }

