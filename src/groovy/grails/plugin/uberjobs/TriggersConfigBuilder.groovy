package grails.plugin.uberjobs

/**
 * Source is mainly taken from the original jesque plugin for grails
 * https://github.com/michaelcameron/grails-jesque/blob/master/grails-jesque/src/groovy/grails/plugin/jesque/TriggersConfigBuilder.groovy
 */
class TriggersConfigBuilder extends BuilderSupport {

    private Integer triggerNumber = 0
    private GrailsUberJobClass jobClass

    private static final String TRIGGER_CRON_EXPRESSION = "cronExpression"
    private static final String TRIGGER_QUEUE_NAME_FIELD = "queueName"
    private static final String TRIGGER_QUEUE_ARGUMENTS = "args"
    private static final String TRIGGER_NAME_FIELD = "name"

    def triggers = [:]

    public TriggersConfigBuilder(GrailsUberJobClass jobClass) {
        super()
        this.jobClass = jobClass
    }

    public build(closure) {
        closure.delegate = this
        closure.call()
        return triggers
    }

    protected void setParent(parent, child) {}

    protected createNode(name) {
        createNode(name, null, null)
    }

    protected createNode(name, value) {
        createNode(name, null, value)
    }

    protected createNode(name, Map attributes) {
        createNode(name, attributes, null)
    }

    protected Object createNode(name, Map attributes, Object value) {
        def trigger = createTrigger(name, attributes)
        triggers[trigger.triggerAttributes.name] = trigger
        trigger
    }

    private prepareCommonTriggerAttributes(Map triggerAttributes) {
        // set trigger name
        if (triggerAttributes[TRIGGER_NAME_FIELD] == null)
            triggerAttributes[TRIGGER_NAME_FIELD] = "${jobClass.fullName}${triggerNumber++}"

        // set trigger queue (if not specified, we simply use the jobs queue name
        if (triggerAttributes[TRIGGER_QUEUE_NAME_FIELD] == null) {
            triggerAttributes[TRIGGER_QUEUE_NAME_FIELD] = jobClass.defaultQueueName
        }

        // set attributes
        if (triggerAttributes[TRIGGER_QUEUE_ARGUMENTS] != null
                && !(triggerAttributes[TRIGGER_QUEUE_ARGUMENTS] in List))
            throw new Exception("If ${TRIGGER_QUEUE_ARGUMENTS} exists, it must be a list")
    }

    public Expando createTrigger(name, Map attributes) {
        def triggerAttributes = new HashMap(attributes)

        prepareCommonTriggerAttributes(triggerAttributes)

        def triggerType = name

        switch (triggerType) {
            case 'cron':
                prepareCronTriggerAttributes(triggerAttributes)
                break
            default:
                throw new Exception("Invalid format")
        }

        new Expando(triggerAttributes: triggerAttributes)
    }

    private def prepareCronTriggerAttributes(Map triggerAttributes) {
        if (!triggerAttributes[TRIGGER_CRON_EXPRESSION])
            throw new Exception("Cron trigger must have 'cronExpression' attribute")

        if (!CronExpression.isValidExpression(triggerAttributes[TRIGGER_CRON_EXPRESSION].toString()))
            throw new Exception("Cron expression '${triggerAttributes[TRIGGER_CRON_EXPRESSION]}' in the job class ${jobClass.fullName} is not a valid cron expression")
    }
}
