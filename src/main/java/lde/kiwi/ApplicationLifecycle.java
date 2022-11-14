package lde.kiwi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ApplicationLifecycle {

    SessionManager sessionManager = new SessionManager();

    String test(){
        return "test";
    }

    void onStart(@Observes StartupEvent ev) {

    }
}
