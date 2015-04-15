package grails.plugin.uberjobs;

import org.codehaus.groovy.grails.commons.InjectableGrailsClass;

import java.util.Map;

interface GrailsUberJobClass extends InjectableGrailsClass {

    public Map getTriggers();

    public String getDefaultQueueName();

    public boolean isSingletonJob();

    public int getMinDelay();

}