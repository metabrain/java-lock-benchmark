import java.util.concurrent.locks.*;


abstract class Worker extends Thread {
	public static volatile boolean testing = false ;
	public double steps = 0 ;
	abstract void go() ;

	@Override
	public void run() {
		System.out.println("run");
		while(testing==false) {
			//System.out.println(0);
		}
		System.out.println("1");

		while(testing==true) {go();steps++;}

		return ;
	}

	int calc = 0 ;
	public static int compute(int calc) {
		int result = calc ;
		for(int i=0;i<Main.cycles;i++)
			result+=calc*(3+4*calc)|(calc*17);
		return result ;
	}
}

class SyncWorker extends Worker {
	static Object singleton = new Object();
	@Override
	void go() {
		synchronized (singleton) {
			calc = compute(calc);
		}
	}
}

class ReentrantWorker extends Worker {
	private static ReentrantLock _lock = new ReentrantLock(false) ;
	@Override
	void go() {
		ReentrantWorker._lock.lock();
		calc = compute(calc);
		ReentrantWorker._lock.unlock();
	}
}

class RWWorker extends Worker {
	private static ReentrantReadWriteLock _lock = new ReentrantReadWriteLock(false) ;
//	private Lock _readlock = _lock.readLock() ;
	@Override
	void go() {
//		_readlock.lock();
//		calc = compute(calc);
//		_readlock.unlock();
		_lock.readLock().lock();
		calc = compute(calc);
		_lock.readLock().unlock();
	}
}


public class Main {

	public static int threads = 4 ;
	public static int time = 1000 ;
	public static int cycles = 100000 ;

	public static void benchmark(String clazz) throws Exception {
		Class c = Class.forName(clazz);
		Worker[] bt = new Worker[threads];

		//Spawn and start
		for (int i = 0; i < bt.length; i++) {
			bt[i] = (Worker) c.newInstance();
			bt[i].start();
		}

		//Start benchmark
		Worker.testing = true;

		//Count time till end of benchmark
		long wstart = System.currentTimeMillis();
		Thread.sleep(time);

		//Stop benchmarking
		Worker.testing = false;
		long wend = System.currentTimeMillis();


		//Asset total iterations on all threads and join threads
		double iterations = 0 ;
		for (int i = 0; i < bt.length; i++) {
			iterations += bt[i].steps ;
			bt[i].join(100);
//			bt[i].join();
			try {
			bt[i].stop();
			bt[i].interrupt();
			} catch (Exception e) {}
		}

		//Print results
		System.out.println("RESULTS:");
		System.out.println("  Test duration (ms)   = " + (wend - wstart));
		System.out.println("  Nb iterations        = " + iterations);
		System.out.println("  Nb iterations/1000        = " + iterations/1000);
		System.out.println("  Stats                = " );
		for (int i = 0; i < bt.length; i++)
			System.out.println("\tThread[" + i + "] -> " + bt[i].steps);

		System.out.printf(
				"(for batch statistics collector) name: [%s|cycles:%d] iteratios/sec: [%.0f] threads: [%d]",
				c.getName(),cycles,iterations/(wend-wstart),threads);
	}

	public static void main(String[] args) throws Exception {

		if(args.length!=3) {
			System.out.println("Usage: <time> <n_threads> <cycles>(atomic operation dificulty)");
		}
		time = Integer.parseInt(args[0]);
		threads = Integer.parseInt(args[1]);
		cycles = Integer.parseInt(args[2]);

		benchmark("SyncWorker");
		benchmark("ReentrantWorker");
		benchmark("RWWorker");
	}

}
