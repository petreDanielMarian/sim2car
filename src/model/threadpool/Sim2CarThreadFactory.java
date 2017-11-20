package model.threadpool;
import java.util.Date;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * sets the name of the threads
 */
class Sim2CarThreadFactory implements ThreadFactory {

	
    private final String name;

    private Logger logger;
    
    protected Sim2CarThreadFactory() {
    	this.logger = Logger.getLogger(Sim2CarThreadFactory.class.getName());
    	this.name = "Sim2CarWorker";
    }

    public Thread newThread(Runnable r) {
    	
    	final Thread t = new Thread(r, this.name);
    	logger.log(Level.INFO, "new " + this.name + ", started: " + new Date());
        return t;
    }
}