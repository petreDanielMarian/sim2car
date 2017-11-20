package test.utils;

import java.util.Vector;
import java.util.concurrent.Callable;
import model.threadpool.ThreadPool;
import org.junit.Test;


import junit.framework.TestCase;

public class TestThreadPool extends TestCase {

	/**
	 * each task adds an integer (0) to the Vector object
	 * @throws InterruptedException 
	 */
	
	@Test
	public void testThreadPoolTasks() {
		
		
		int tasksNr = 10;
		/* Vector is thread safe */
		Vector<Integer> e = new Vector<Integer>();
		/* ThreadPool.resetPool(); */
		ThreadPool tp = ThreadPool.getInstance();
		
		for (int i = 0; i < tasksNr; ++i) {
			Runnable r = new TestRunnable(e);
			tp.submit(r);
		}
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		
		assertTrue("Thread pool still has unfinished tasks", tp.shutdownNow().isEmpty());
		assertTrue("Thread pool did not do any work", e.size() == tasksNr);
	}
	
	/**
	 * test if a worker executes a task until completion
	 */
	
	@Test
	public void testThreadPoolBehaviour() {
		
		int tasksNr = 2;
		//Vector is thread safe
		Vector<Integer> e = new Vector<Integer>();
		/* ThreadPool.resetPool(); */
		ThreadPool tp = ThreadPool.getInstance();
		
		tp.setCorePoolSize(1);
		
		for (int i = 0; i < tasksNr; ++i) {
			Runnable r = new TestRunnableBehaviour(e, i);
			tp.submit(r);
		}
		
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		boolean allTasks = false;
		boolean switchedOnce = false;
		System.out.println("vector size test 2: " + e.size());
		for (int i = 0; i < e.size() - 1; ++i) {
			
			if (e.get(i) != e.get(i + 1) && !switchedOnce) {
				
				switchedOnce = true;
			}
			else if (e.get(i) != e.get(i + 1)) {
				
				allTasks = true;
				break;
			}
		}
		
		assertTrue("a worker thread does not execute a runnable untill it is finished", !allTasks);
	}
	
	/**
	 * test if a worker comes back to finish a waiting task which has been notified 
	 */
	
	@Test
	public void testThreadPoolWaitBehavior() {

		//Vector is thread safe
		Vector<Integer> e = new Vector<Integer>();
		/* ThreadPool.resetPool(); */
		ThreadPool tp = ThreadPool.getInstance();
		
		tp.setCorePoolSize(2);
		
		TestRunnableWaitBehaviour r1 = new TestRunnableWaitBehaviour(e, 0);
		TestRunnableWaitBehaviour r2 = new TestRunnableWaitBehaviour(e, 1);
		r1.setPoolNeighbor(r2);
		r2.setPoolNeighbor(r1);
		tp.submit(r1);
		tp.submit(r2);
		
		tp.shutdown();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("vector size test 2: " + e.size());
		
		
		assertFalse("a task can notify another waiting task", e.size() == 20);
	}
	
	/**
	 * test ThreadPool isEmpty() method
	 */
	@Test
	public void testIsEmpty() {

		int tasksNr = 100;
		//Vector is thread safe
		Vector<Integer> e = new Vector<Integer>();
		/* ThreadPool.resetPool(); */
		ThreadPool tp = ThreadPool.getInstance();
		tp.setCorePoolSize(1);
		for (int i = 0; i < tasksNr; ++i) {
			Runnable r = new TestRunnableIntensive(e);
			tp.submit(r);
		}
		
		assertTrue("Thread pool should not be empty", !tp.isEmpty());
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		assertTrue("thread pool is not empty", tp.isEmpty());
	}
}


class TestRunnableIntensive implements Runnable {

	private Vector<Integer> e;
	
	public TestRunnableIntensive(Vector<Integer> e) {
		
		this.e = e;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		for (int i = 0; i < 1000; ++i) {
			int x = 3 * i;
			e.add(x);
		}
	}
}




class TestRunnableWaitBehaviour implements Callable<Object> {

	final Object mutex = new Object();
	private Vector<Integer> e;
	private int x;
	private TestRunnableWaitBehaviour poolNeighbor;
	
	public TestRunnableWaitBehaviour(Vector<Integer> e, int x) {
		this.x = x;
		this.e = e;
	}
	
	public void setPoolNeighbor(TestRunnableWaitBehaviour poolNeighbor) {
		
		this.poolNeighbor = poolNeighbor;
	}
	
	public Object call() {
		boolean waitOnce = false;
		for (int i = 0; i < 10; ++i) {
			
			System.out.println(x);
			e.add(x);
			if (x == 0 && !waitOnce) {
				waitOnce = true;
				//synchronized (mutex) {
					System.out.println("Going to wait (lock held by "
				+ Thread.currentThread().getName() + ")");
				
					try {
						mutex.wait();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				//}
			}
			
		}
		
		System.out.println("Done waiting (lock held by "
		+ Thread.currentThread().getName() + ")");

		
		if (x == 1) {
			synchronized (mutex) {
				
				poolNeighbor.mutex.notify();
			}
			
		}
		return waitOnce;

	}
	
}



class TestRunnable implements Runnable {

	private Vector<Integer> e;
	
	public TestRunnable(Vector<Integer> e) {
		
		this.e = e;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		e.add(0);
	}
}


class TestRunnableBehaviour implements Runnable {

	private Vector<Integer> e;
	private int x;
	
	public TestRunnableBehaviour(Vector<Integer> e, int x) {
		this.x = x;
		this.e = e;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		for (int i = 0; i < 1000000; ++i) {
			
			System.out.println(x);
			e.add(x);
		}
	}	
}