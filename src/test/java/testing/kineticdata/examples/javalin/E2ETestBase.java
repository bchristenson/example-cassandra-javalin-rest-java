package testing.kineticdata.examples.javalin;

import com.kineticdata.examples.javalin.ExampleApp;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;

public class E2ETestBase {
    
    /*----------------------------------------------------------------------------------------------
     * BEFORE
     *--------------------------------------------------------------------------------------------*/
    
    private static final AtomicBoolean STARTUP = new AtomicBoolean(false);
    private static final AtomicReference<Exception> STARTUP_EXCEPTION = new AtomicReference();
    
    @Before
    public void beforeEach_E2ETestBase() throws Exception {
        // If this is the first time this has been run
        if (STARTUP.compareAndSet(false, true)) {
            // Initialize a completable future to use to wait for the app to finish starting
            CompletableFuture<Void> startupFuture = new CompletableFuture<>();
            // Start the application in another thread
            new Thread(() -> {
                try {
                    ExampleApp.start(() -> startupFuture.complete(null));
                } catch (Exception e) {
                    STARTUP_EXCEPTION.set(e);
                    startupFuture.complete(null);
                }
            }).start();
            // Wait until the server completes startup before beginning tests
            startupFuture.get(10, TimeUnit.SECONDS);
        }
        
        // If there was a problem starting the app, raise the exception again
        if (STARTUP_EXCEPTION.get() != null) {
            throw STARTUP_EXCEPTION.get();
        }
    }
    
    /*----------------------------------------------------------------------------------------------
     * HELPER METHODS
     *--------------------------------------------------------------------------------------------*/
    
    public static String url(String path) {
        return "http://localhost:3000"+path;
    }
    
}
