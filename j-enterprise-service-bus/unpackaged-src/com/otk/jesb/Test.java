package com.otk.jesb;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class Test {

	public static void main(String[] args) throws InterruptedException {
		Console.DEFAULT.log("Repeat (yes/no)?");
		String answer;
		Consumer<String> initialConsoleInput = Console.DEFAULT.getPendingInputLineConsumer();
		try {
			BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<String>(1);
			Console.DEFAULT.setPendingInputLineConsumer(line -> {
				try {
					blockingQueue.put(line);
				} catch (InterruptedException e) {
					throw new UnexpectedError(e);
				}
			});
			answer = blockingQueue.take();
		} finally {
			Console.DEFAULT.setPendingInputLineConsumer(initialConsoleInput);
		}
		if(answer.equals("yes")) {
			
		}
	}

}
