package everypony.sweetieBot.bus;

import everypony.sweetieBot.U;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pony Event Broadcast Service.
 *
 * @author cab404
 */
public class B {

    private static final Map<Object, List<Method>> handlers = new ConcurrentHashMap<>();

    public static void register(Object listener) {
        List<Method> methods = new ArrayList<>();

        for (Method method : listener.getClass().getMethods())
            if (method.isAnnotationPresent(Bus.Handler.class)) {
                if (method.getParameterTypes().length != 1)
                    throw new RuntimeException(method.getName() + " should take exactly one arg!");
                methods.add(method);
            }

        if (!methods.isEmpty())
            handlers.put(listener, methods);
        else
            U.w("No handlers found!");

    }


    public static void unregister(Object listener) {
        handlers.remove(listener);
    }


    public static void post(Object event) {
        int invocations = 0;

        for (Map.Entry<Object, List<Method>> e : handlers.entrySet())
            for (Method handler : e.getValue())
                if (handler.getParameterTypes()[0].isAssignableFrom(event.getClass()))
                    try {
                        handler.invoke(e.getKey(), event);
                        invocations++;
                    } catch (IllegalAccessException | InvocationTargetException e1) {
                        throw new RuntimeException(e1);
                    }

        if (invocations == 0)
            throw new RuntimeException("No handlers found!");

    }


}
