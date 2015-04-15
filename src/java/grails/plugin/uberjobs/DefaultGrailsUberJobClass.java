package grails.plugin.uberjobs;

import groovy.lang.Closure;
import org.codehaus.groovy.grails.commons.AbstractInjectableGrailsClass;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;

import java.util.HashMap;
import java.util.Map;

class DefaultGrailsUberJobClass extends AbstractInjectableGrailsClass implements GrailsUberJobClass {

    public static final String JOB = "Job";

    private Map triggers;

    public DefaultGrailsUberJobClass(Class clazz) {
        super(clazz, JOB);
    }

    private Map evaluateTriggers() {
        // registering additional triggersClosure from 'triggersClosure' closure if present
        Closure triggersClosure = (Closure) GrailsClassUtils.getStaticPropertyValue(getClazz(), "triggers");

        TriggersConfigBuilder builder = new TriggersConfigBuilder(this);

        if (triggersClosure != null) {
            builder.build(triggersClosure);
            return (Map)builder.getTriggers();
        } else {
            return new HashMap();
        }
    }

    public Map getTriggers() {
        if( triggers == null )
            triggers = evaluateTriggers();
        return triggers;
    }

    @Override
    public String getDefaultQueueName() {
        String queue = (String)getPropertyValue(GrailsUberJobClassProperty.QUEUE);
        if( queue == null )
            queue = getName() + "Queue";
        return queue;
    }

    @Override
    public boolean isSingletonJob() {
        Boolean result = (Boolean)getPropertyValue(GrailsUberJobClassProperty.WORKER_POOL);
        return result != null && result;
    }

    @Override
    public int getMinDelay() {
        return 0;
    }

    @Override
    public String toString() {
        return "Job > " + getName();
    }
}
