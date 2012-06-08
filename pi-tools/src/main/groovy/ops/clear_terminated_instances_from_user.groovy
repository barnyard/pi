builder = appCtx['piIdBuilder']
factory = appCtx['dhtClientFactory']

username = 'user:moserp'
userid = builder.getPId(username)
user = factory.createBlockingReader().get(userid)

list = []

for (instanceId in user.instanceIds)
{
    println instanceId
    id = builder.getPIdForEc2AvailabilityZone(com.bt.pi.app.common.entities.Instance.getUrl(instanceId))
    instance = factory.createBlockingReader().get(id)
    println instance.getState().toString()
    if (instance.getState().toString().equals('TERMINATED'))
    {
        list.add (instanceId)
    }
}

for (instanceId in list)
{
    user.removeInstance(instanceId)
}

println user

factory.createBlockingWriter().put(userid, user)

