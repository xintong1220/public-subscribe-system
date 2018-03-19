package pssystem;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Notification;
import javax.management.NotificationListener;

public class Server implements NotificationListener {
    
    private final int TOPIC_NUM = 3;

	private ArrayList<Member> memberList; // list for active member

	private ArrayList<Member> deadList; // list for dead member

	private int t_gossip; // gossip every t_gosssip ms

	public int t_cleanup; // start leader election after t_cleanup timeout

	private Random random;

	private DatagramSocket server;

	private String myAddress; // Ip + Port

	private Member me;

	private String[] serverList = {"127.0.0.1:2222", "127.0.0.1:2223", "127.0.0.1:2224"}; // all the servers we have

	private Map<Integer, Set<String>> subscribers; // HashMap for subscribers. key: topic ID; value: each String for one client, "IP:Port"
    
    /**
	 * Setup the client's lists, gossiping parameters, and parse the startup config file.
	 * @throws SocketException
	 * @throws InterruptedException
	 * @throws UnknownHostException
	 */
	public Server(int port) throws SocketException, InterruptedException, UnknownHostException {
		
		/* when shut down the server, log a message */
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				System.out.println(" Don't let me go...");
			}
		}));

		memberList = new ArrayList<Member>();
		deadList = new ArrayList<Member>();
		t_gossip = 5000;
		t_cleanup = 30000;
		random = new Random();
		
		subscribers = new HashMap<>();
		for(int i = 0; i < TOPIC_NUM; i++) {
            subscribers.put(i, new HashSet<>());
        }
        
		String myIpAddress = InetAddress.getLocalHost().getHostAddress();
		this.myAddress = myIpAddress + ":" + port;

		/* loop over the initial hosts, and find ourselves */
		for (String host : serverList) {

			Member member = new Member(host, 0, this, t_cleanup);

			if(host.contains("2222")) {
				member.setLeader(true); // Initially we choose 2222 as the leader
			}

			if(host.contains(String.valueOf(port))) {
				// save our own Member class so we can increment our heartbeat later
				me = member;
				port = Integer.parseInt(host.split(":")[1]);
				this.myAddress = myIpAddress + ":" + port;
				System.out.println("I am " + me + "I am " + (member.isLeader()? "" : "not ") + "leader");
			}
			memberList.add(member);
		}

		System.out.println("Original Member List");
		System.out.println("---------------------");
		for (Member member : memberList) {
			System.out.println(member);
		}

		server = new DatagramSocket(port);
	}

	/**
	 * Performs the sending of the membership list, after we have
	 * incremented our own heartbeat.
	 */
	private void sendMembershipList() {

		this.me.setHeartbeat(me.getHeartbeat() + 1);

		synchronized (this.memberList) {
			try {
				Member member = getRandomMember();

				if(member != null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(this.memberList);
					byte[] buf = baos.toByteArray();

					String address = member.getAddress();
					String host = address.split(":")[0];
					int port = Integer.parseInt(address.split(":")[1]);

					InetAddress dest;
					dest = InetAddress.getByName(host);

					System.out.println();
					System.out.println();
					System.out.println("Sending to " + dest);
					System.out.println("---------------------");
					for (Member m : memberList) {
						System.out.println(m + (m.isLeader() ? "(leader)" : ""));
					}
					System.out.println("---------------------");
					System.out.println();
					System.out.println();
                    
					// send membership info
					DatagramSocket socket = new DatagramSocket();
					DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
					socket.send(datagramPacket);
				}
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
    
    private void sendSubscriberMap() {
        
        this.me.setHeartbeat(me.getHeartbeat() + 1);
        
        synchronized (this.subscribers) {
            try {
                Member member = getRandomMember();
    
                if(member != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(subscribers);
                    byte[] buf = baos.toByteArray();
        
                    String address = member.getAddress();
                    String host = address.split(":")[0];
                    int port = Integer.parseInt(address.split(":")[1]);
        
                    InetAddress dest;
                    dest = InetAddress.getByName(host);
        
                    System.out.println();
                    System.out.println();
                    System.out.println("Sending to " + dest);
                    System.out.println("---------------------");
                    System.out.println(subscribers.toString());
                    System.out.println("---------------------");
                    System.out.println();
                    System.out.println();
        
                    // send membership info
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
                    socket.send(datagramPacket);
                }
                
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
	
	/* Ring election method for leader election */
	private void election(String vote, String initiator) {
		System.out.println("vote new candidate leader:" + vote + ", initiator: " + initiator);
		synchronized (this.memberList) {
			if(this.memberList.size() == 1 && !me.isLeader()) {
				me.setLeader(true);
				System.out.println("I am " + me + ", I am the new leader");
				return;
			}
		}
		try {
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(vote + "#" + initiator);
			byte[] buf = baos.toByteArray();
			
			String nextIp = "";
			//find next member in the ring
			synchronized (this.memberList) {
				int id = this.memberList.indexOf(this.me);
				if(id == this.memberList.size() - 1) {
					nextIp = this.memberList.get(0).getAddress();
				} else {
					nextIp = this.memberList.get(id + 1).getAddress();
				}
				
				String host = nextIp.split(":")[0];
				int port = Integer.parseInt(nextIp.split(":")[1]);
				
				InetAddress dest;
				dest = InetAddress.getByName(host);
				
				DatagramSocket socket = new DatagramSocket();
				DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
				socket.send(datagramPacket);
				socket.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Find a random peer from the local membership list.
	 * Ensure that we do not select ourselves, and keep
	 * trying 10 times if we do.  Therefore, in the case
	 * where this client is the only member in the list,
	 * this method will return null
	 * @return Member random member if list is greater than 1, null otherwise
	 */
	private Member getRandomMember() {
		Member member = null;
		
		if(this.memberList.size() > 1) {
			int tries = 10;
			do {
				int randomNeighborIndex = random.nextInt(this.memberList.size());
				member = this.memberList.get(randomNeighborIndex);
				if(--tries <= 0) {
					member = null;
					break;
				}
			} while(member.getAddress().equals(this.myAddress));
		}
		else {
//			System.out.println("I am alone in this world.");
		}
		
		return member;
	}
	
	/**
	 * The class handles gossiping the membership list.
	 * This information is important to maintaining a common
	 * state among all the nodes, and is important for detecting
	 * failures.
	 */
	private class MembershipGossiper implements Runnable {
		
		private AtomicBoolean keepRunning;
		
		public MembershipGossiper() {
			this.keepRunning = new AtomicBoolean(true);
		}
		
		@Override
		public void run() {
			while(this.keepRunning.get()) {
				try {
					TimeUnit.MILLISECONDS.sleep(t_gossip);
					sendMembershipList();
					sendSubscriberMap();
				} catch (InterruptedException e) {
					// TODO: handle exception
					// This membership thread was interrupted externally, shutdown
					e.printStackTrace();
					keepRunning.set(false);
				}
			}
			
			this.keepRunning = null;
		}
		
	}
	
	/**
	 * This class handles the passive cycle, where this client
	 * has received an incoming message. 
	 */
	private class AsychronousReceiver implements Runnable {
		
		private AtomicBoolean keepRunning;
		
		public AsychronousReceiver() {
			keepRunning = new AtomicBoolean(true);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			while(keepRunning.get()) {
				try {
                    byte[] buf = new byte[10240];
					DatagramPacket p = new DatagramPacket(buf, buf.length);
					server.receive(p);
					
					// extract the member arraylist out of the packet
					ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
					ObjectInputStream ois = new ObjectInputStream(bais);
					
					Object readObject = ois.readObject();
					
					// process the message received from other server
					if(readObject instanceof ArrayList<?>) {
						ArrayList<Member> list = (ArrayList<Member>) readObject;
						memberListProcessor(list);
					} else if (readObject instanceof String) {
						String msg = (String) readObject;
						System.out.println("Received message from subscriber: " + msg);
						if(msg.contains("#")) {
							electionMsgProcessor(msg);
						} else {
							newSubscriberProcessor(msg);
						}
					} else if(readObject instanceof Map<?, ?>) {
						HashMap<Integer, Set<String>> map = (HashMap<Integer, Set<String>>) readObject;
						subscriberProcessor(map);
					}
					
				} catch (IOException e) {
					e.printStackTrace();
					keepRunning.set(false);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		
		private void memberListProcessor(ArrayList<Member> list) {
			System.out.println();
			System.out.println();
                    System.out.println("---------------------");
            System.out.println("Received member list:");
			System.out.println("---------------------");
            for (Member member : list) {
                System.out.println(member + (member.isLeader() ? "(leader)" : ""));
            }
			System.out.println("---------------------");
            System.out.println();
            System.out.println();
            // Merge our list with the one we just received
            mergeLists(list);
        }
        
        private void electionMsgProcessor(String msg) {
            String candidate_leader = msg.split("#")[0];
            String initiator = msg.split("#")[1];
    
            if(candidate_leader.compareTo(Server.this.myAddress) < 0) {
                Server.this.election(Server.this.myAddress, initiator);
            } else if(candidate_leader.compareTo(Server.this.myAddress) > 0) {
                Server.this.election(candidate_leader, initiator);
            } else {
                Server.this.me.setLeader(true);
                System.out.println("I am " + me + ", I am the new leader");
            }
        }
        
        private void subscriberProcessor(HashMap<Integer, Set<String>> map) {
            System.out.println();
            System.out.println();
            System.out.println("Received subscriber map:");
            System.out.println("---------------------");
            System.out.println(map.toString());
            System.out.println("---------------------");
            System.out.println();
            System.out.println();
            // Merge our map with the one we just received
            mergeMaps(map);
        }
        
        private void newSubscriberProcessor(String msg) {
		    String[] clientMsg = msg.split(" ");
		    String ip = clientMsg[0];
		    for(int i = 1; i < clientMsg.length; i++) {
		        int topic = Integer.valueOf(clientMsg[i]);
		        if(subscribers.containsKey(topic)) {
		            subscribers.get(topic).add(ip);
                } else {
		            System.out.println("Subscriber requires a topic that does not exist. The topic is " + topic);
                }
            }
            System.out.println("latest subscriber map: ");
            System.out.println("---------------------");
            System.out.println(subscribers.toString());
            System.out.println("---------------------");
        }

		/**
		 * Merge remote list (received from peer), and our local member list.
		 * Simply, we must update the heartbeats that the remote list has with
		 * our list. 
		 */
		private void mergeLists(ArrayList<Member> remoteList) {

			synchronized (Server.this.deadList) {

				synchronized (Server.this.memberList) {

					for (Member remoteMember : remoteList) {
						if(Server.this.memberList.contains(remoteMember)) {
							Member localMember = Server.this.memberList.get(Server.this.memberList.indexOf(remoteMember));

							if(remoteMember.getHeartbeat() > localMember.getHeartbeat()) {
								// update local list with latest heartbeat
								localMember.setHeartbeat(remoteMember.getHeartbeat());
								// and reset the timeout of that member
								localMember.resetTimeoutTimer();
							}
						}
						else {
							// the local list does not contain the remote member

							// the remote member is either brand new, or a previously declared dead member
							// if its dead, check the heartbeat because it may have come back from the dead

							if(Server.this.deadList.contains(remoteMember)) {
								Member localDeadMember = Server.this.deadList.get(Server.this.deadList.indexOf(remoteMember));
								if(remoteMember.getHeartbeat() > localDeadMember.getHeartbeat()) {
									// it's back
									Server.this.deadList.remove(localDeadMember);
									Member newLocalMember = new Member(remoteMember.getAddress(), remoteMember.getHeartbeat(), Server.this, t_cleanup);
									newLocalMember.setLeader(remoteMember.isLeader());
									Server.this.memberList.add(newLocalMember);
									newLocalMember.startTimeoutTimer();
								} // else ignore
							}
							else {
								// brand spanking new member - welcome
								Member newLocalMember = new Member(remoteMember.getAddress(), remoteMember.getHeartbeat(), Server.this, t_cleanup);
								newLocalMember.setLeader(remoteMember.isLeader());
								Server.this.memberList.add(newLocalMember);
								newLocalMember.startTimeoutTimer();
							}
						}
					}
					Collections.sort(Server.this.memberList, ((a, b)->(a.getAddress().compareTo(b.getAddress()))));
				}
			}
		}
		
		/* If a new subscriber does not in the local subscribers map, add it into the map. */
		private void mergeMaps(HashMap<Integer, Set<String>> remoteMap) {
            synchronized (Server.this.subscribers) {
                for(Map.Entry<Integer, Set<String>> entry : remoteMap.entrySet()) {
                    for(String sub : entry.getValue()) {
                        if(!subscribers.get(entry.getKey()).contains(sub)) {
                            subscribers.get(entry.getKey()).add(sub);
                        }
                    }
                }
            }
        }
	}

	/**
	 * Starts the client.  Specifically, start the various cycles for this protocol.
	 * Start the gossip thread and start the receiver thread.
	 * @throws InterruptedException
	 */
	private void start() throws InterruptedException, IOException {

		// Start all timers except for me
		for (Member member : memberList) {
			if(member != me) {
				member.startTimeoutTimer();
			}
		}

		// Start the two worker threads
		ExecutorService executor = Executors.newCachedThreadPool();
		//  The receiver thread is a passive player that handles
		//  merging incoming membership lists from other neighbors.
		executor.execute(new AsychronousReceiver());
		//  The gossiper thread is an active player that 
		//  selects a neighbor to share its membership list
		executor.execute(new MembershipGossiper());

		// Potentially, you could kick off more threads here
		//  that could perform additional data synching

		// keep the main thread around
		while(true) {
			TimeUnit.SECONDS.sleep(10);
            
			// send messages to client subscribe to a certain topic
            BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in));
            String s = "";
            try {
                s = systemIn.readLine();
                int topic = Integer.valueOf(s.split(":")[0]);
                String msg = s.split(":")[1];
                
                // publish message to all the subscribers subscribed such topic
                for(String subscriber : subscribers.get(topic)) { // String subscriber: "ip:port", "172.20.194.209:2222"
                    InetAddress ipAddr = InetAddress.getByName(subscriber.split(":")[0]);
                    int portAddr = Integer.parseInt(subscriber.split(":")[1]);
                    publishMessage(msg, ipAddr, portAddr);
                }
                
            } catch (IOException e) {
                e.printStackTrace();
            }
            
		}
	}
	
	private void publishMessage(String message, InetAddress ipAddr, int portAddr) throws IOException {
		System.out.println("sending " + message + " to IP: " + ipAddr + ", PORT: " + portAddr);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        byte[] buf = baos.toByteArray();
        
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, ipAddr, portAddr);
        
        socket.send(datagramPacket);
        socket.close();
    }

	public static void main(String[] args) throws InterruptedException, IOException {
		Server server = new Server(Integer.valueOf(args[0]));
		server.start();
	}

	/**
	 * All timers associated with a member will trigger this method when it goes
	 * off.  The timer will go off if we have not heard from this member in t_cleanup time.
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {

		Member deadMember = (Member) notification.getUserData();

		System.out.println("Dead member detected: " + deadMember);

		synchronized (this.memberList) {
			this.memberList.remove(deadMember);
		}

		synchronized (this.deadList) {
			this.deadList.add(deadMember);
		}
		if (deadMember.isLeader()) {
			election(this.myAddress, this.myAddress);
		}
	}
}
