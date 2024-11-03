package dzervas.classucker;

public class Main {
	public static void main(String[] args) {
		System.out.println("Main application is running.");

		while (true) {
			// Simulate some work
			try {
				Thread.sleep(5000); // Sleep for 5 seconds
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
