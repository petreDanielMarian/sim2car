package model.threadpool;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import model.parameters.Globals;

import com.beust.jcommander.Parameter;

/**
 * Singleton class, uses a private ScheduledThreadPoolExecutor to execute tasks
 */
public class ThreadPool {

	
	@Parameter(names = {"--corePoolSize"}, description = "number of threads to keep in the pool", arity = 1)
    public static int corePoolSize = Runtime.getRuntime().availableProcessors();
	
    @Parameter(names = {"--maxPoolSize"}, description = "maximum pool size", arity = 1)
	public static int maxPoolSize = 10000;
	private ScheduledThreadPoolExecutor executor;
	
	/* singleton pattern this class instance */
	private static ThreadPool _instance = null;
	
	/** Logger used by this class */
	private final Logger logger;
	
	/**
	 * private constructor
	 */
	private ThreadPool() {
		/**
		 * default number of threads in the pool - the runtime available processors
		 */
		System.out.println(" Get cores size is " + corePoolSize);
		this.executor = new ScheduledThreadPoolExecutor(corePoolSize, new Sim2CarThreadFactory());
		this.executor.setMaximumPoolSize(maxPoolSize);
		
		this.logger = Logger.getLogger(ThreadPool.class.getName());
	}
	
	/**
	 * 
	 * @return the ThreadPool instance
	 */
	public static ThreadPool getInstance() {
		
		if (_instance == null) {
			
			
			_instance = new ThreadPool();
		}
		
		return _instance;
	}
	
	/**
	 * this method should never be used in production
	 * it is used only for unit tests
	 */
	public static void resetPool() {
		
		_instance = null;
	}
	
	
	
	/**
	 * 
	 * @return - the scheduledThreadPoolExecutor
	 */
	public ScheduledThreadPoolExecutor getExecutor() {
		
		return this.executor;
	}
	
	/**
	 * submits a task for execution
	 * @param task - the task to submit
	 * @return - a Future representing pending completion of that task
	 */
	public Future<?> submit(Runnable task) {
		
		if (Globals.threadPoolLogging)
			logger.log(Level.INFO, "new Runnable task has been submitted");
		return this.executor.submit(task);
	}
	
	
	/**
	 * submits a task for execution
	 * @param task - the task to submit
	 * @return - a Future representing pending completion of that task
	 */
	public <T> Future<T> submit(Callable<T> task) {
		
		if (Globals.threadPoolLogging)
			logger.log(Level.INFO, "new Callable task has been submitted");
		return this.executor.submit(task);
	}
	
	
	/**
	 * Initiates an orderly shutdown in which previously submitted tasks are executed,
	 * but no new tasks will be accepted.
	 */
	public void shutdown() {
		if (Globals.threadPoolLogging)
			logger.log(Level.INFO, "pool shutdown() at " + new Date());
		this.executor.shutdown();
	}
	
	/**
	 * Attempts to stop all actively executing tasks and halts the processing of waiting tasks
	 * @return - list of the tasks that were awaiting execution. 
	 */
	public List<Runnable> shutdownNow() {
		if (Globals.threadPoolLogging)
			logger.log(Level.INFO, "pool shutdownNow() at " + new Date());
		return this.executor.shutdownNow();
	}
	
	/**
	 *
	 * @return boolean - checks if the workers have tasks to execute or not
	 */
	public boolean isEmpty() {
		
		return this.executor.getActiveCount() == 0;
	}
	
	/**
	 * sets the number of worker threads
	 * @param size - the desired number of workers
	 */
	public void setCorePoolSize(int size) {
		
		this.executor.setCorePoolSize(size);
	}
	
	/**
	 * 
	 * @return the number of worker threads
	 */
	public int getCorePoolSize() {
		
		return this.executor.getCorePoolSize();
	}
	public void waitForThreadPoolProcessing() {
		while(!this.isEmpty()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				logger.log(Level.WARNING, e.getMessage());
			}
		}
	}
}