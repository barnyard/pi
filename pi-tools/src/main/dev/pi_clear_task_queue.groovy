/*
 * To run from Groovy Shell
 * telnet localhost 6789
 * The following script gives you the number of tasks in a PiQueue
 */

piIdBuilder = appCtx['piIdBuilder']
factory = appCtx['dhtClientFactory']
parser = appCtx['koalaJsonParser']

import com.bt.pi.app.common.resource.PiQueue
import com.bt.pi.core.entity.TaskProcessingQueue
import com.bt.pi.core.continuation.UpdateResolver

qid = piIdBuilder.getPiQueuePId(PiQueue.REMOVE_INSTANCE_FROM_USER).forLocalAvailabilityZone()
factory.createBlockingWriter().update(qid, null, new UpdateResolver<TaskProcessingQueue>() { public TaskProcessingQueue update(TaskProcessingQueue existingEntity, TaskProcessingQueue requestedEntity) { existingEntity.tasks.clear(); return existingEntity;}})