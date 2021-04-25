package org.simple.software;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;


// Java program to illustrate
// ThreadPool
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WoCoServer {

	public static final char SEPARATOR = '$';
	private static ExecutorService pool;
	private ConcurrentHashMap<Integer, StringBuilder> buffer;
	private ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> results;
	private static ConcurrentHashMap<Integer, ConcurrentHashMap<String, Double>> measurements;

	/**
	 * Performs the word count on a document. It first converts the document to
	 * lower case characters and then extracts words by considering "a-z" english characters
	 * only (e.g., "alpha-beta" become "alphabeta"). The code breaks the text up into
	 * words based on spaces.
	 * @param line The document encoded as a string.
	 * @param wc A ConcurrentHashMap to store the results in.
	 */
	public static void doWordCount(String line, ConcurrentHashMap<String, Integer> wc) {
		String ucLine = line.toLowerCase();
		StringBuilder asciiLine = new StringBuilder();

		char lastAdded = ' ';
		for (int i=0; i<line.length(); i++) {
			char cc = ucLine.charAt(i);
			if ((cc>='a' && cc<='z') || (cc==' ' && lastAdded!=' ')) {
				asciiLine.append(cc);
				lastAdded = cc;
			}
		}

		String[] words = asciiLine.toString().split(" ");
		for (String s : words) {
			if (wc.containsKey(s)) {
				wc.put(s, wc.get(s)+1);
			} else {
				wc.put(s, 1);
			}
		}
	}

	/**
	 * Constructor of the server.
	 */
	public WoCoServer() {
		buffer = new ConcurrentHashMap<Integer, StringBuilder>();
		results = new ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>>();
		measurements = new ConcurrentHashMap<Integer, ConcurrentHashMap<String, Double>>();

	}

	/**
	 * This function handles data received from a specific client (TCP connection).
	 * Internally it will check if the buffer associated with the client has a full
	 * document in it (based on the SEPARATOR). If yes, it will process the document and
	 * return true, otherwise it will add the data to the buffer and return false
	 * @param clientId
	 * @param dataChunk
	 * @return A document has been processed or not.
	 */
	public boolean receiveData(int clientId, String dataChunk, boolean cMode) {

		StringBuilder sb;
		if (!results.containsKey(clientId)) {
			results.put(clientId, new ConcurrentHashMap<String, Integer>());
		}

		if (!buffer.containsKey(clientId)) {
			sb = new StringBuilder();
			buffer.put(clientId, sb);
		} else {
			sb = buffer.get(clientId);
		}

		sb.append(dataChunk);
		if (dataChunk.indexOf(WoCoServer.SEPARATOR)>-1) {
			//we have at least one line

			String bufData = sb.toString();

			int indexNL = bufData.indexOf(WoCoServer.SEPARATOR);

			String line = bufData.substring(0, indexNL);
			String rest = (bufData.length()>indexNL+1) ? bufData.substring(indexNL+1) : null;

			if (indexNL==0) {
				System.out.println("SEP@"+indexNL+" bufdata:\n"+bufData);
			}

			if (rest != null) {
				System.out.println("more than one line: \n"+rest);
				try {
					System.in.read();
				} catch (IOException e) {
					e.printStackTrace();
				}
				buffer.put(clientId, new StringBuilder(rest));
			} else {
				buffer.put(clientId, new StringBuilder());
			}

			//word count in line
			ConcurrentHashMap<String, Integer> wc = results.get(clientId);

			if (cMode==true) {
				Timer t = new Timer();
				line = cleanUpHTML(line, wc);
				double time = t.check();
				addMeasurements(clientId, "time spent cleaning the document (removing tags)[s] ", time);
			} else {
				addMeasurements(clientId, "time spent cleaning the document (removing tags)[s] ", 0.0);
			}

			Timer t = new Timer();
			doWordCount(line, wc);
			double time = t.check();
			addMeasurements(clientId, "time spent performing the word count[s] ", time);

			return true;
		} else {
			return false;
		}

	}

	public void addMeasurements(Integer clientId, String measurementName, Double time) {
		ConcurrentHashMap<String, Double> ms = measurements.get(clientId);
		ms.put(measurementName, time);
	}

	public String cleanUpHTML(String htmlString, ConcurrentHashMap<String, Integer> wc) {
		List<String> allMatches = new ArrayList<String>();
		Pattern p = Pattern.compile(Pattern.quote("title=\"") + "(.*?)" + Pattern.quote("\""));
		Matcher m = p.matcher(htmlString);
		while (m.find()) {
			allMatches.add(m.group(1));
		}
		htmlString = htmlString.replaceAll("<.*?>" , "").replaceAll(".*>", "").replaceAll("<.*", ""); // replace tags
		htmlString = htmlString + String.join(" ", allMatches);
		return htmlString;
	}

	/**
	 * Returns a serialized version of the word count associated with the last
	 * processed document for a given client. If not called before processing a new
	 * document, the result is overwritten by the new one.
	 * @param clientId
	 * @return
	 */
	public String serializeResultForClient(int clientId) {
		if (results.containsKey(clientId)) {
			StringBuilder sb = new StringBuilder();
			ConcurrentHashMap<String, Integer> hm = results.get(clientId);
			for (String key : hm.keySet()) {
				sb.append(key+",");
				sb.append(hm.get(key)+",");
			}
			results.remove(clientId);
			sb.append("\n");
      System.out.println("Response " +  sb.substring(0));
			return sb.substring(0);
		} else {
			return "";
		}
	}

	public String serializeMeasurementsForClients() {
		StringBuilder sb = new StringBuilder();
		for (Integer key : measurements.keySet()) {
			sb.append("\nClient " + key + ": ");
			sb.append( measurements.get(key) + ",");
			sb.append("\n");
		}
		return sb.substring(0);
	}

	public String serializeAverageForClients() {
		StringBuilder sb = new StringBuilder();
		ArrayList<Double> list = new ArrayList<Double>();
		ArrayList<Double> measurementsTotal = new ArrayList<Double>(Arrays.asList(0.0, 0.0, 0.0, 0.0));
		Double totalTime = 0.0;
		Double totalTask1 = 0.0;
		Double totalTask2 = 0.0;
		Double totalTask3 = 0.0;
		Double totalTask4 = 0.0;

		for (Integer clientId : measurements.keySet()) {
			ConcurrentHashMap<String, Double> res = measurements.get(clientId);

			for(String key: res.keySet()){
				totalTime += res.get(key);
			}
			totalTask1 += res.get("time spent until the entire document has been received[s] ");
			totalTask2 += res.get("time spent cleaning the document (removing tags)[s] ");
			totalTask3 += res.get("time spent performing the word count[s] ");
			totalTask4 += res.get("time spent serializing the results[s] ");
		}

		sb.append("Total time: " +  totalTime + "\n" +
		"(task1) Total time spent until the entire document has been received: " +totalTask1 + " or " + getPercentage(totalTime, totalTask1) + "%"
		+ "\n" + "(task2) Total time spent cleaning the document: " + totalTask2 + " or " + getPercentage(totalTime, totalTask2) + "%"
		+ "\n" + "(task3) Total time spent performing the word count: " + totalTask3 + " or " + getPercentage(totalTime, totalTask3) + "%"
		+ "\n" + "(task4) Total time spent serializing the results: " + totalTask4 + " or " + getPercentage(totalTime, totalTask4) + "%" + "\n");

		return sb.substring(0);
	}

	public Double getPercentage(double total, double time) {
		if (time == 0.0) return 0.0;
		return (double) time * 100 / total;
	}

	public static void main(String[] args) throws IOException {

		if (args.length!=4) {
			System.out.println("Usage: <listenaddress> <listenport> <cleaning> <threadcount>");
			System.exit(0);
		}

		String lAddr = args[0];
		int lPort = Integer.parseInt(args[1]);
		boolean cMode = Boolean.parseBoolean(args[2]);
		int threadCount = Integer.parseInt(args[3]);

		WoCoServer server = new WoCoServer();
		Selector selector = Selector.open();

		ServerSocketChannel serverSocket = ServerSocketChannel.open();
		InetSocketAddress myAddr = new InetSocketAddress(lAddr, lPort);

		serverSocket.bind(myAddr);
		serverSocket.configureBlocking(false);

		int ops = serverSocket.validOps();
		SelectionKey selectKey = serverSocket.register(selector, ops, null);

		// Infinite loop..
		// Keep server running
		ByteBuffer bb = ByteBuffer.allocate(1024*1024);
		ByteBuffer ba;

		if (threadCount>1) {
			pool = Executors.newFixedThreadPool(threadCount);
			System.out.println("THREADS: " + threadCount);
		}

		while (true) {
			selector.select();
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = readyKeys.iterator();

			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();

				if (key.isAcceptable()) {
					SocketChannel client = serverSocket.accept();

					client.configureBlocking(false);

					client.register(selector, SelectionKey.OP_READ);
					System.out.println("Connection Accepted: " + client.getLocalAddress() + "\n");
				} else if (key.isReadable()) {
            System.out.println("Key is readable: " + key.isReadable() + "\n");
            SocketChannel client = (SocketChannel) key.channel();
						int clientId = client.hashCode();

						if (threadCount>1) {
								//TODO: will have to implement multithreading
								Runnable r1 = new Runnable(){
									public void run(){

											ByteBuffer bb = ByteBuffer.allocate(1024*1024);
											ByteBuffer ba;

											if (!measurements.containsKey(clientId)) measurements.put(clientId, new ConcurrentHashMap<String, Double>());
											Timer t1 = new Timer();

											bb.rewind();
											int readCnt = 0;
											try {
												readCnt = client.read(bb);
												}
											catch(IOException e) {
													e.printStackTrace();
											}

											// Put this in a thread
											if (readCnt>0) {

													// String below contains the html doc as string
													String result = new String(bb.array(),0, readCnt);

													double time = t1.check();
													server.addMeasurements(clientId, "time spent until the entire document has been received[s] ", time);

													boolean hasResult = server.receiveData(clientId, result, cMode);
                          System.out.println(hasResult);
													if (hasResult) {
															Timer t2 = new Timer();
															ba = ByteBuffer.wrap(server.serializeResultForClient(clientId).getBytes());
															double endTime = t2.check();
															server.addMeasurements(clientId, "time spent serializing the results[s] ", endTime);
																try {
																	client.write(ba);
                                  System.out.println("Sent to client");
																}
																catch(IOException e) {
																		e.printStackTrace();
																}
													}
                          return;
											}
											else {
													// String stats = server.serializeMeasurementsForClients();
													// System.out.println("Client stats: " + stats);
													// System.out.println("\n");

													// String avgStats = server.serializeAverageForClients();
													// System.out.println("Average stats: " + avgStats);

													key.cancel();
                          // System.out.println("Executed " + key);

													// if (threadCount>1) pool.shutdown();
											}
                      return;
									}
								};
								// passes the Task objects to the pool to execute (Step 3)
								pool.execute(r1);
						}
            //  else {
						// 	// Create measurements for client
						// 	if (!measurements.containsKey(clientId)) measurements.put(clientId, new ConcurrentHashMap<String, Double>());
						// 	Timer t1 = new Timer();

						// 	bb.rewind();
						// 	int readCnt = client.read(bb);

						// 	// Put this in a thread
						// 	if (readCnt>0) {
						// 		// String below contains the html doc as string
						// 		String result = new String(bb.array(),0, readCnt);

						// 		double time = t1.check();
						// 		server.addMeasurements(clientId, "time spent until the entire document has been received[s] ", time);

						// 		boolean hasResult = server.receiveData(clientId, result, cMode);

						// 		if (hasResult) {
						// 			Timer t2 = new Timer();
						// 			ba = ByteBuffer.wrap(server.serializeResultForClient(clientId).getBytes());
						// 			double endTime = t2.check();
						// 			server.addMeasurements(clientId, "time spent serializing the results[s] ", endTime);
						// 			client.write(ba);
						// 		}
						// 	}
						// 	else {
						// 			String stats = server.serializeMeasurementsForClients();
						// 			System.out.println("Client stats: " + stats);
						// 			System.out.println("\n");

						// 			String avgStats = server.serializeAverageForClients();
						// 			System.out.println("Average stats: " + avgStats);

						// 			key.cancel();
						// 			if (threadCount>1) pool.shutdown();
						// 			// System.exit(0);
						// 	}
						// }
				}
        // pool.shutdown();
        // while (!pool.isTerminated()) {
        // }
        // System.out.println("Finished all threads");
        iterator.remove();

			}
		}
	}
}
