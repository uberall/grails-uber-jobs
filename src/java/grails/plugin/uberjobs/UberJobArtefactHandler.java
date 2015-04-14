package grails.plugin.uberjobs;

import org.codehaus.groovy.grails.commons.ArtefactHandlerAdapter;

import java.lang.reflect.Method;

class UberJobArtefactHandler extends ArtefactHandlerAdapter {

    public static final String METHOD_NAME = "perform";
    public static final String TYPE = "UberJob";

    public UberJobArtefactHandler(){
        super(TYPE, GrailsUberJobClass.class, DefaultGrailsUberJobClass.class, null);
    }


    public boolean isArtefactClass(Class clazz) {
        // class shouldn't be null and should end with Job suffix
        if(clazz == null || !clazz.getName().endsWith(DefaultGrailsUberJobClass.JOB))
            return false;

        // and should have a perform() method with any signature
        //Method method = ReflectionUtils.findMethod(clazz, PERFORM, null);
        for( Method method : clazz.getDeclaredMethods() ) {
            if( method.getName().equals(METHOD_NAME) )
                return true;
        }
        return false;
    }
}
