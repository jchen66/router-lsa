package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  
  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];
  public short sospfType;
  ServerSocket serverSocket;
  boolean isStart;
  Timer timer;
 
  
  public Router(Configuration config) throws IOException {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    rd.processIPAddress = config.getString("socs.network.router.processIP");
    rd.processPortNumber = config.getShort("socs.network.router.port");
    lsd = new LinkStateDatabase(rd); 
    isStart = false;
    serverSocket = new ServerSocket(rd.processPortNumber);
    
    ServerTask server = new ServerTask();
    Thread thread = new Thread(server);
    thread.start();
    
    timer = new Timer();
    timer.scheduleAtFixedRate(new HeartbeatTask(), 0, 1000);
//    HeartbeatTaskRun heart = new HeartbeatTaskRun();
//    Thread thread1 = new Thread(heart);
//    thread1.start();
  }
  
//  public class HeartbeatTaskRun extends TimerTask {
//	 
//	@Override
//	public void run() {
//		// TODO Auto-generated method stub
//		
//		 for (int j = 0; j < 4; j++) {
//				if ((ports[j] != null) && ports[j].router2 != null && (ports[j].router2.status == RouterStatus.TWO_WAY) && !(ports[j].isAlive)) {
//					try {System.out.println("uuuuu" + j);
//						processDisconnect((short) j);
//					} catch (SocketTimeoutException e) { 
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
//	}
//	  
//  }
  
  public class HeartbeatTask extends TimerTask {
	  
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for (int j = 0; j < 4; j++) {
				if ((ports[j] != null) && ports[j].router2 != null && (ports[j].router2.status == RouterStatus.TWO_WAY)) {
					ports[j].isAlive = false;
					try {//System.out.println("ooooo");
						ClientTask client2 = new ClientTask(ports[j].router2.processIPAddress, ports[j].router2.processPortNumber, ports[j].router2.simulatedIPAddress, (short) 3);
						Thread thread2 = new Thread(client2);
						thread2.start();
						synchronized(this) {
						    try {
						        wait(100);
						    } catch (InterruptedException e) { }
						}
//						
						if (ports[j] != null && !ports[j].isAlive) {
							System.out.println(ports[j].router2.simulatedIPAddress + " is down!");
							processDisconnect((short) j);							
						}
					} catch (SocketTimeoutException e) {
						// TODO Auto-generated catch block 
						
						try {
							System.out.println(ports[j].router2.simulatedIPAddress + " is down!");
							processDisconnect((short) j);
							
						} catch (SocketTimeoutException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						e.printStackTrace();
					}
					
			  }
			synchronized(this) {
			    try {
			        wait(4000);
			    } catch (InterruptedException e) { }
			}
//				Timer timer2 = new Timer();
//				 timer2.schedule(new HeartbeatTaskRun(), 3000);
				 
//			for (j = 0; j < 4; j++) {
//				if ((ports[j] != null) && ports[j].router2 != null && (ports[j].router2.status == RouterStatus.TWO_WAY) && !(ports[j].isAlive)) {
//					try {
//						processDisconnect((short) j);System.out.println("d~~~");
//					} catch (SocketTimeoutException e) { 
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}
		}

	}
  }
  public class ServerTask implements Runnable {
	public ServerTask() throws SocketTimeoutException {
		
	}
  	@Override
  	public void run() {
  		while(true) {
  			try {
				Socket server = serverSocket.accept();
				DataInputStream in = new DataInputStream(server.getInputStream());
				ObjectInputStream oin = new ObjectInputStream(in);
				SOSPFPacket packIn = (SOSPFPacket) oin.readObject();
				//oin.close();
				if (packIn.sospfType == 0) {
					System.out.println("received HELLO from " + packIn.srcIP + ";");
					boolean ifAdded = false;
					  for (int i = 0; i < 4; i++) {
						  if ((ports[i] != null) && ports[i].router2 != null && (ports[i].router2.simulatedIPAddress.equals(packIn.srcIP))) {
							  ifAdded = true;
							  if (ports[i].router2.status == RouterStatus.INIT) {
								  ports[i].router2.status = RouterStatus.TWO_WAY;
								  System.out.println("set " + packIn.srcIP + " state to TWO_WAY;");
								  LinkDescription ld = new LinkDescription();
								  ld.linkID = packIn.srcIP;
								  ld.portNum = i;
								  ld.tosMetrics = ports[i].weight;
								  LSA lsa = lsd._store.get(rd.simulatedIPAddress);
								  lsa.links.add(ld);
								  lsa.lsaSeqNumber++;
								  //lsd._store.remove(rd.simulatedIPAddress);
								  lsd._store.put(rd.simulatedIPAddress,lsa);
								  Vector<LSA> lsaArray = new Vector<LSA>();
								  for (String keyID : lsd._store.keySet()) {
										 LSA lsa2 = lsd._store.get(keyID);
										 lsaArray.add(lsa2);
								  }
								  for (int j = 0; j < 4; j++) {
									  if ((ports[j] != null) && ports[j].router2 != null && (ports[j].router2.simulatedIPAddress.equals(packIn.srcIP))) {
										  ClientTask client2 = new ClientTask(ports[j].router2.processIPAddress, ports[j].router2.processPortNumber, ports[j].router2.simulatedIPAddress, (short) 1, lsaArray);
										  Thread thread2 = new Thread(client2);
										  thread2.start();
									  }
								  }
							  }
							  else if (ports[i].router2.status == RouterStatus.TWO_WAY) {
								  //continue;
								  break;
							  }
							  else {
								  ports[i].router2.status = RouterStatus.TWO_WAY;
								  System.out.println("set " + packIn.srcIP + " state to TWO_WAY;");
								  ClientTask client = new ClientTask(packIn.srcProcessIP, packIn.srcProcessPort, packIn.srcIP, (short) 0, ports[i].weight);
								  Thread thread = new Thread(client);
								  thread.start();
								  LinkDescription ld = new LinkDescription();
								  ld.linkID = packIn.srcIP;
								  ld.portNum = i;
								  ld.tosMetrics = ports[i].weight;
								  LSA lsa = lsd._store.get(rd.simulatedIPAddress);
								  lsa.links.add(ld);
								  lsa.lsaSeqNumber++;
								  //lsd._store.remove(rd.simulatedIPAddress);
								  lsd._store.put(rd.simulatedIPAddress,lsa);
								  Vector<LSA> lsaArray = new Vector<LSA>();
								  for (String keyID : lsd._store.keySet()) {
										 LSA lsa2 = lsd._store.get(keyID);
										 lsaArray.add(lsa2);
								  }
								  for (int j = 0; j < 4; j++) {
									  if ((ports[j] != null) && ports[j].router2 != null && (ports[j].router2.simulatedIPAddress.equals(packIn.srcIP))) {
										  ClientTask client2 = new ClientTask(ports[j].router2.processIPAddress, ports[j].router2.processPortNumber, ports[j].router2.simulatedIPAddress, (short) 1, lsaArray);
										  Thread thread2 = new Thread(client2);
										  thread2.start();
									  }
								  }
							  }
						  }
					  }
					  if (ifAdded == false) {
						  boolean isFull = true;
						  for (int i = 0; i < 4; i++) {
							  if (ports[i] == null) {
								  isFull = false;
								  RouterDescription rd2 = new RouterDescription();
								  rd2.processIPAddress = packIn.srcProcessIP;
								  rd2.processPortNumber = packIn.srcProcessPort;
								  rd2.simulatedIPAddress = packIn.srcIP;
								  ports[i] = new Link(rd, rd2,packIn.weight);
								  ports[i].router2.status = RouterStatus.INIT;
								  System.out.println("set " + packIn.srcIP + " state to INIT;");
								  ClientTask client = new ClientTask(packIn.srcProcessIP, packIn.srcProcessPort, packIn.srcIP, (short) 0, packIn.weight);
								  Thread thread = new Thread(client);
								  thread.start();
								  break;
								  				  
							  }
						  }
						  if (isFull == true) {
							  System.out.print("Failed! All the ports are occupied! Cannot attach more routers!");  
							  continue;
						  }

						  
					  }
					  
				}
				else if (packIn.sospfType == 1) {
					boolean isNew = false;
					for (int i = 0; i < packIn.lsaArray.size(); i++) {
						LSA lsa = packIn.lsaArray.get(i);
						if (lsd._store.containsKey(lsa.linkStateID)) {
							LSA oldLsa = lsd._store.get(lsa.linkStateID);
							if (lsa.lsaSeqNumber > oldLsa.lsaSeqNumber) {//System.out.println("!!!!");
								isNew = true;
								lsd._store.remove(lsa.linkStateID);
								lsd._store.put(lsa.linkStateID, lsa);	
							}
						}
						else {
							lsd._store.put(lsa.linkStateID, lsa);	
							isNew = true;
						}	
					}
					if (isNew == true) {
						  Vector<LSA> lsaArray = new Vector<LSA>();
						  for (String keyID : lsd._store.keySet()) {
								 LSA lsa2 = lsd._store.get(keyID);
								 lsaArray.add(lsa2);
						  }
						for (int j = 0; j < 4; j++) {
							if ((ports[j] != null && ports[j].router2 != null && !(ports[j].router2.simulatedIPAddress.equals(packIn.srcIP))) /*&& (ports[i].router2.status == RouterStatus.TWO_WAY)*/) {
								ClientTask client2 = new ClientTask(ports[j].router2.processIPAddress, ports[j].router2.processPortNumber, ports[j].router2.simulatedIPAddress, (short) 1, lsaArray);
								Thread thread2 = new Thread(client2);
								thread2.start();
							}
						}
					}
				}
//				else if (packIn.sospfType == 2) {System.out.println("2222");
//				boolean isNew1 = false;
//				if (lsd._store.containsKey(packIn.routerID)) {							
//						isNew1 = true;
//						lsd._store.remove(packIn.routerID);
//					
//				}
//				if (isNew1 == true) {
//					for (int j = 0; j < 4; j++) {
//						if ((ports[j] != null && ports[j].router2 != null && !(ports[j].router2.simulatedIPAddress.equals(packIn.srcIP))) /*&& (ports[i].router2.status == RouterStatus.TWO_WAY)*/) {
//							ClientTask client2 = new ClientTask(ports[j].router2.processIPAddress, ports[j].router2.processPortNumber, ports[j].router2.simulatedIPAddress, (short) 2, packIn.routerID);
//							Thread thread2 = new Thread(client2);
//							thread2.start();
//						}
//					}
//				}
//			}
				
			else if (packIn.sospfType == 3) {//System.out.println("3333");
				for (int j = 0; j < 4; j++) {
					if (ports[j] != null && ports[j].router2 != null && ports[j].router2.simulatedIPAddress.equals(packIn.srcIP)) {
						ClientTask client2 = new ClientTask(packIn.srcProcessIP,packIn.srcProcessPort, packIn.srcIP, (short) 4);//System.out.println("3434");
						Thread thread2 = new Thread(client2);
						thread2.start();
						break;
					}
				}
				
			}
			else if (packIn.sospfType == 4) {//System.out.println("4444");
				for (int j = 0; j < 4; j++) {
					if (ports[j] != null && ports[j].router2 != null && ports[j].router2.simulatedIPAddress.equals(packIn.srcIP)) {
						ports[j].isAlive = true;//System.out.println("here");
						break;
					}
				}
			}
			
			//server.close();	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
  		}
  	}

  }
  
  public class ClientTask implements Runnable {
	  	private String dsProcessIPAddress;
	  	private int dsProcessPortNumber;
	  	private String dsSimulatedIPAddress;
	  	private Short sospfType;
	  	private Short weight;
	  	private Vector<LSA> lsaArray;
		private String routerID;

		public ClientTask(String dsProcessIPAddress, int dsProcessPortNumber, String dsSimulatedIPAddress, Short sospfType, Short weight) throws SocketTimeoutException {
			this.dsProcessIPAddress = dsProcessIPAddress;
			this.dsProcessPortNumber = dsProcessPortNumber;
			this.dsSimulatedIPAddress = dsSimulatedIPAddress;
			this.sospfType = sospfType;
			this.weight = weight;
		}
	  	public ClientTask(String dsProcessIPAddress, int dsProcessPortNumber, String dsSimulatedIPAddress, Short sospfType, Vector<LSA> lsaArray) throws SocketTimeoutException {
			this.dsProcessIPAddress = dsProcessIPAddress;
			this.dsProcessPortNumber = dsProcessPortNumber;
			this.dsSimulatedIPAddress = dsSimulatedIPAddress;
			this.sospfType = sospfType;
			this.lsaArray = lsaArray;	
		}
	  	public ClientTask(String dsProcessIPAddress, int dsProcessPortNumber, String dsSimulatedIPAddress, Short sospfType, String routerID) throws SocketTimeoutException {
			this.dsProcessIPAddress = dsProcessIPAddress;
			this.dsProcessPortNumber = dsProcessPortNumber;
			this.dsSimulatedIPAddress = dsSimulatedIPAddress;
			this.sospfType = sospfType;
			this.routerID = routerID;	
		}
	  	public ClientTask(String dsProcessIPAddress, int dsProcessPortNumber, String dsSimulatedIPAddress, Short sospfType) throws SocketTimeoutException {
			this.dsProcessIPAddress = dsProcessIPAddress;
			this.dsProcessPortNumber = dsProcessPortNumber;
			this.dsSimulatedIPAddress = dsSimulatedIPAddress;
			this.sospfType = sospfType;
			this.routerID = routerID;	
		}
	  	
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
						 Socket	client = new Socket(dsProcessIPAddress, dsProcessPortNumber);		
						 //client.setSoTimeout(3000);
						 OutputStream outToServer = client.getOutputStream();  
						 DataOutputStream out = new DataOutputStream(outToServer);
						 ObjectOutputStream oout = new ObjectOutputStream(out);
						 SOSPFPacket packOut = new SOSPFPacket();
						 packOut.srcProcessIP = rd.processIPAddress;
						 packOut.srcProcessPort = rd.processPortNumber;
						 packOut.srcIP = rd.simulatedIPAddress;
						 packOut.dstIP = dsSimulatedIPAddress;
						 packOut.sospfType = sospfType;
						 if (sospfType == 0) packOut.weight = weight;
						 else if (sospfType == 1) {
							 packOut.lsaArray = lsaArray;
						 }
						 else if (sospfType == 2) {
							 packOut.routerID = routerID;
						 } 
						 oout.writeObject(packOut);
						 outToServer.close();
						 oout.close();
						 out.close();
						 client.close();
					
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				catch (ConnectException e) {
					// TODO Auto-generated catch block
					for (int j = 0; j < 4; j++) {
						if (ports[j] != null && ports[j].router2 != null && ports[j].router2.simulatedIPAddress.equals(dsSimulatedIPAddress)) {
							try {
								System.out.println(dsSimulatedIPAddress + " is down!");
								processDisconnect((short) j);
								
							} catch (SocketTimeoutException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							break;
						}
					}
					//e.printStackTrace();
				}catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
			// }
		}
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {
	  String result = lsd.getShortestPath(destinationIP); 
      System.out.println(result);
  } 

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
 * @throws SocketTimeoutException 
   */
  private void processDisconnect(short portNumber) throws SocketTimeoutException {
	  //for (LinkDescription linkDescription : lsd._store.get(rd.simulatedIPAddress).links) {
	  for (Iterator<LinkDescription> iterator = lsd._store.get(rd.simulatedIPAddress).links.iterator(); iterator.hasNext(); ) {
		  if (iterator.next().portNum == portNumber) {
			  iterator.remove();
			  //lsd._store.get(rd.simulatedIPAddress).links.remove(linkDescription);
			  lsd._store.get(rd.simulatedIPAddress).lsaSeqNumber++;
		  }
	  }
	  //lsd._store.remove(ports[portNumber].router2.simulatedIPAddress);  
	  ports[portNumber] = null;
//	  for (int j = 0; j < 4; j++) {
//			if ((ports[j] != null) && ports[j].router2 != null && (ports[j].router2.status == RouterStatus.TWO_WAY)) {
//				ClientTask client2 = new ClientTask(ports[j].router2.processIPAddress, ports[j].router2.processPortNumber, ports[j].router2.simulatedIPAddress, (short) 2, ports[portNumber].router2.simulatedIPAddress);
//				Thread thread2 = new Thread(client2);
//				thread2.start(); 
//			}
//		}
	  Vector<LSA> lsaArray = new Vector<LSA>();
	  for (String keyID : lsd._store.keySet()) {
			 LSA lsa2 = lsd._store.get(keyID);
			 lsaArray.add(lsa2);
	  }
	  for (int j = 0; j < 4; j++) {
		  if ((ports[j] != null) && ports[j].router2 != null && (ports[j].router2.status == RouterStatus.TWO_WAY)) {
			  ClientTask client2 = new ClientTask(ports[j].router2.processIPAddress, ports[j].router2.processPortNumber, ports[j].router2.simulatedIPAddress, (short) 1, lsaArray);
			  Thread thread2 = new Thread(client2);
			  thread2.start();
		  }
	  }
	  
	  
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to identify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP, short weight) {
	  if (simulatedIP.equals(rd.simulatedIPAddress)) {
		  System.out.println("Failed! Cannot add to itself!");
		  return;
	  }
	  boolean ifAdded = false;
	  for (int i = 0; i < 4; i++) {
		  if ((ports[i] != null) && ports[i].router2 != null && (ports[i].router2.simulatedIPAddress.equals(simulatedIP))) {
			  ifAdded = true;
		  }
	  }
	  if (ifAdded == true) { 
		  System.out.println("Failed! This router has been already attached!");
		  return;
	  }
	  boolean isFull = true;
	  for (int i = 0; i < 4; i++) {
		  if (ports[i] == null) {
			  isFull = false;
			  RouterDescription rd2 = new RouterDescription();
			  rd2.processIPAddress = processIP;
			  rd2.processPortNumber = processPort;
			  rd2.simulatedIPAddress = simulatedIP;
			  ports[i] = new Link(rd, rd2, weight);
			  break;
		  }
	  }
	  if (isFull == true) System.out.print("Failed! All the ports are occupied! Cannot attach more routers!");
  }

  /**
   * broadcast Hello to neighbors
 * @throws IOException 
 * @throws UnknownHostException 
   */
  private void processStart() throws UnknownHostException, IOException {
	  isStart = true;
	  for (int i = 0; i < 4; i++) {
			
		  if ((ports[i] != null) && ports[i].router2 != null && (ports[i].router2.status != RouterStatus.TWO_WAY)) {
			  ClientTask client = new ClientTask(ports[i].router2.processIPAddress, ports[i].router2.processPortNumber, ports[i].router2.simulatedIPAddress, (short) 0, ports[i].weight);
			  Thread thread = new Thread(client);
			  thread.start();
		  }
	  }
	 
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
 * @throws IOException 
 * @throws UnknownHostException 
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) throws UnknownHostException, IOException {
	 
	  if (isStart) {
		  processAttach(processIP, processPort, simulatedIP, weight);
		  processStart();
	  }
	  else {
		  System.out.println("Have not performed start yet!");
		  return;
	  }
  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
	  for (int i = 0; i < 4; i++) {
		  if ((ports[i] != null) && ports[i].router2 != null && (ports[i].router2.status == RouterStatus.TWO_WAY)) {
			  System.out.println(ports[i].router2.simulatedIPAddress);
		  }
	  }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {
	  System.exit(0);
  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.startsWith("connect ")) { 
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
          //invalid command
          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
