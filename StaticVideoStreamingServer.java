
/***********************************************
 * CMPE 207, Spring 2016                       *
 * TEAM 12                                     *
 **********************************************/

package rtsp207;

import java.net.Socket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import javax.swing.Timer;

import java.net.InetAddress;
import java.net.Socket;
import java.io.*;
import java.net.*;
import java.io.*;
import java.net.*;
import java.util.*;
import static java.lang.System.out;



public class StaticVideoStreamingServer{
	
   static int imageInBytes = 0;
   static int typeOfMjpeg = 26;
   static int FramePeriod = 100;
   static int LengthOfVideo = 1000;
 
   Timer timer;
   private static ServerSocket SocketOfServer = null;
   private static Socket SocketOfClient[] = new Socket[100];
   private static final int maxClientsCount = 10;
   private static final threadOfClient[] threads = new threadOfClient[maxClientsCount];
   

   public static void main(String args[]) throws Exception{
	
	  String s;      
      int choice;
      BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
	   
      System.out.println("1. Unicasting Using Multithreading ");
      System.out.println("2. Multicasting ");
      System.out.println("Enter the choice ");
      s=br.readLine();
      choice=Integer.parseInt(s);
	   
	   
      switch(choice)
      {
	   case 1:
           // Unicast Scenario
		   Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
	       for (NetworkInterface netint : Collections.list(nets))
	           displayInterfaceInfo(netint);
           System.out.println("Enter the Port Number");
           int i = 0;
           int portNumber =Integer.parseInt(br.readLine());
           if (args.length < 1)
               {
                     System.out.println("Usage: java StaticVideoStreamingServer <portNumber>\n"
                                        + "Port Number Used=" + portNumber);
           }
           else
           {
                     portNumber = Integer.valueOf(args[0]).intValue();
           }

           try {
                     SocketOfServer = new ServerSocket(portNumber);
           } catch (IOException e) {
                     System.out.println(e);
           }
           
           System.out.println("Now Client Can Stream video from IP:"+SocketOfServer.getInetAddress().getLocalHost().getHostAddress()+" Port:"+portNumber);
                  

           int a=0;
           while (true) {
           try {
    	
               a++;
               SocketOfClient[a] = SocketOfServer.accept();
               
               System.out.println("New Connection accepted from client: "+SocketOfClient[a].getInetAddress());
               System.out.println("Receiving Port of Client  "+SocketOfClient[a].getPort()+"\n");
               System.out.println();
               StreamVideo video = new StreamVideo("/Users/rdatta/CMPE207/Group12/video.mjpeg");// Path for videofile
               
               //RAKESH : START
               rtspThread rThread = new rtspThread(a, SocketOfClient[a], threads,video);
               rThread.start();
               System.out.println("Created RTSP Thread ID: "+ rThread.getId());
               //RAKESH : END
               
               if (i == maxClientsCount) {
                   PrintStream os1 = new PrintStream(SocketOfClient[a].getOutputStream());
                   os1.println("Server too busy. Try later.");
                   os1.close();
                   SocketOfClient[a].close();
               }
           } catch (IOException e) {
               System.out.println(e);
           }
           }
              
	   case 2:
           //Multicast Scenario
		   System.out.println("InMulticasting");  
		   
           DatagramSocket socket = null;
           DatagramPacket outPacket = null;
           final int PORT = 8888;
           byte[] buf=new byte[65000];
	        
           try
           {
              socket = new DatagramSocket();
	        
	          StreamVideo video= new StreamVideo("./video.mjpeg");
	          InetAddress address = InetAddress.getByName("127.0.0.1");
	          while (true) 
	          {
	              
	              int image_length = video.getnextframe(buf);
	              RTPPactetizer rtp_packet = new RTPPactetizer(typeOfMjpeg, imageInBytes, imageInBytes*FramePeriod, buf,buf.length );
	          
	              int packet_length = rtp_packet.getlength();
	              byte[] packet_bits = new byte[packet_length];
	              rtp_packet.getpacket(packet_bits);
	              outPacket = new DatagramPacket(packet_bits, packet_length, address, PORT);
	              socket.send(outPacket);
	              try 
	              {
	              Thread.sleep(50);
	              } 
	              catch (InterruptedException ie) 
	              {
	              }
	            }
	        } 
	        catch (IOException ioe)
	        {
	          System.out.println(ioe);
	        }
	        
	        
	    break;  
	    
	    default:
	        
	        System.out.println("Wrong option");
	        break;
	    
	    }
  }//Main Ends Here
  
  static void displayInterfaceInfo(NetworkInterface netint) throws SocketException {
      out.printf("Display name: %s\n", netint.getDisplayName());
      out.printf("Name: %s\n", netint.getName());
      Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
      for (InetAddress inetAddress : Collections.list(inetAddresses)) {
          out.printf("InetAddress: %s\n", inetAddress);
      }
      out.printf("\n");
  }
    
}//StaticVideoStreamingServer Class ends here



// Class for RTP Packet Creation
class RTPPactetizer {

	static int SizeOfHeader = 12;
	public int HeaderVersion;
	public int Padding;
	public int Extension;
	public int CC;
	public int Marker;
	public int PayloadType;
	public int SequenceNumber;
	public int TimeStamp;
	public int SyncSource;

	public byte[] header;
	public int payload_size;
	public byte[] payload;

    public RTPPactetizer(int PType, int Framenb, int Time, byte[] data, int data_length) {

        HeaderVersion = 2;
        Padding = 0;
        Extension = 0;
        CC = 0;
        Marker = 0;
        SyncSource = 0;
        SequenceNumber = Framenb;
        TimeStamp = Time;
        PayloadType = PType;

        header = new byte[SizeOfHeader];
        header[1] = (byte) ((Marker << 7) | PayloadType);
        header[2] = (byte) (SequenceNumber >> 8);
        header[3] = (byte) (SequenceNumber);

        for (int i = 0; i < 4; i++)
            header[7 - i] = (byte) (TimeStamp >> (8 * i));

        for (int i = 0; i < 4; i++)
            header[11 - i] = (byte) (SyncSource >> (8 * i));

	    payload_size = data_length;
	    payload = new byte[data_length];
	    payload = data;

 
   }

   public RTPPactetizer(byte[] packet, int packet_size) {

       HeaderVersion = 2;
       Padding = 0;
       Extension = 0;
       CC = 0;
       Marker = 0;
       SyncSource = 0;

	if (packet_size >= SizeOfHeader) {

		header = new byte[SizeOfHeader];
		for (int i = 0; i < SizeOfHeader; i++)
			header[i] = packet[i];

		payload_size = packet_size - SizeOfHeader;
		payload = new byte[payload_size];
		for (int i = SizeOfHeader; i < packet_size; i++)
			payload[i - SizeOfHeader] = packet[i];

		PayloadType = header[1] & 127;
        SequenceNumber = unsigned_int(header[3]) + 256
        * unsigned_int(header[2]);
        TimeStamp = unsigned_int(header[7]) + 256 * unsigned_int(header[6])
        + 65536 * unsigned_int(header[5]) + 16777216
        * unsigned_int(header[4]);
    }
   }


    public int getpayload(byte[] data) {
        for (int i = 0; i < payload_size; i++)
            data[i] = payload[i];
        return (payload_size);
    }


    public int getpayload_length() {
        return (payload_size);
    }

    public int getlength() {
        return (payload_size + SizeOfHeader);
    }


    public int getpacket(byte[] packet) {

        for (int i = 0; i < SizeOfHeader; i++)
            packet[i] = header[i];
        for (int i = 0; i < payload_size; i++)
            packet[i + SizeOfHeader] = payload[i];

        return (payload_size + SizeOfHeader);
    }


    public int gettimestamp() {
        return (TimeStamp);
    }


    public int getsequencenumber() {
        return (SequenceNumber);
    }


    public int getpayloadtype() {
        return (PayloadType);
    }


    public void printheader() {

	for (int i = 0; i < (SizeOfHeader - 4); i++) {
		for (int j = 7; j >= 0; j--)
			if (((1 << j) & header[i]) != 0)
				System.out.print("1");
			else
				System.out.print("0");
		System.out.print(" ");
	}
        System.out.println();
    }


    int unsigned_int(int nb) {
        if (nb >= 0)
            return (nb);
        else
            return (256 + nb);
    }

}//RTPPactetizer Class ends here


//Class to Stream UDP vide files
class StreamVideo {
	FileInputStream fis; 
	int frame_nb; 

 public  StreamVideo(String filename) throws Exception{
   fis = new FileInputStream(filename);
   frame_nb = 0;
 }

 
 public int getnextframe(byte[] frame) throws Exception
 {
   int length = 0;
   String length_string;
   byte[] frame_length = new byte[5];
   fis.read(frame_length,0,5);
   length_string = new String(frame_length);
   length = Integer.parseInt(length_string);
   return(fis.read(frame,0,length));
 }
} 


//Class to create UDP Threads for individual clients
class threadOfClient extends Thread {
	static int i=0;
	static int imageInBytes = 0; 
	static int typeOfMjpeg = 128; 
	static int FramePeriod = 100;
	static int LengthOfVideo = 500; 
	private Socket SocketOfClient = null;
	private StreamVideo video;
	
	private String clientName = null;
	private DataInputStream is = null;
	private PrintStream os = null;
	private final threadOfClient[] threads;
	private int maxClientsCount;

	// RAKESH: START
    private boolean keepStreaming;
    
  public void setStreaming(boolean flag){
	  this.keepStreaming = flag;
  }
   //RAKESH: END
  
  public threadOfClient(Socket SocketOfClient, threadOfClient[] threads, StreamVideo a) {
    this.SocketOfClient = SocketOfClient;
    this.threads = threads;
    maxClientsCount = threads.length;
    video = a;
    this.keepStreaming = false;
  }

  public void run() {
	  int i=0;
	
	  int PORT = SocketOfClient.getPort();
	    DatagramSocket DataSocket = null;
	    DatagramPacket outPacket = null;	 
	    byte[] buf=new byte[65000]; 
	    
	    try {
	      DataSocket = new DatagramSocket();
	      i++;
	      System.out.println("RTP Thread "+i +" Created For Client "+SocketOfClient.getInetAddress());
          
	      while (true) {
	    	  System.out.println("Stream Flag set to :"+keepStreaming);
	    	  if (keepStreaming) {
	    		  int image_length = video.getnextframe(buf);
	      
	    		  InetAddress address = SocketOfClient.getInetAddress();
	    		  RTPPactetizer rtp_packet = new RTPPactetizer(typeOfMjpeg, imageInBytes, imageInBytes*FramePeriod, buf,buf.length );
	    		  int packet_length = rtp_packet.getlength();
	   
	    		  byte[] packet_bits = new byte[packet_length];
	    		  rtp_packet.getpacket(packet_bits);
	    		  outPacket = new DatagramPacket(packet_bits, packet_length, address, PORT);
	    		  DataSocket.send(outPacket);
	    		  i++;
	    		  //Thread.sleep(1000);
	    		  System.out.println("Server sends packet to the client: "+outPacket.getAddress());
	    		  System.out.println("Server sends packet of size : " + outPacket.getLength());
	    		  System.out.println("Data In The Packet : " + outPacket.getData());
	    		  System.out.println("Server sends packet to the port: "+outPacket.getPort());
	    		  System.out.println("Number of Packets sent "+i);
	    		  System.out.println();
	    		  try {
	    			  Thread.sleep(500);
	    		  } catch (InterruptedException ie) {  
	    		  }
	    	  }
          }
	       
	    } catch (IOException ioe) {
	      System.out.println(ioe);
	    } catch (Exception e) {
			e.printStackTrace();
		}
	   
  }
}
  

// RAKESH: START

class rtspThread extends Thread {
	
	private Socket socket = null;
	private StreamVideo video;
    private final threadOfClient[] threads;
    private int sessid;
	
    public rtspThread(int sessID, Socket rtspSocket, threadOfClient[] threads, StreamVideo vid) {
    	this.socket = rtspSocket;
    	this.threads = threads;
        video = vid;
        sessid = sessID;
    }
    
   

    public void run() {
	    
	    try {
	      
	      System.out.println("RTSP Thread Created"); 
	      System.out.println("Client IP:"+ socket.getInetAddress());
	      System.out.println("Client Port "+socket.getPort()); 
	      
	      int port= socket.getPort();
          
          BufferedWriter outStr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
          String msg = port + "\n";
          outStr.write(msg);
          outStr.flush();
	      
	      BufferedReader inputStr = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	      /* Extract the Message headers and parse*/
	      System.out.println("Reached here");
	      // Keep Reading RTSP Packets
	      threadOfClient t = new threadOfClient(this.socket, this.threads, this.video);
	      t.start();
	      int thread_id = (int)t.getId();
		  System.out.println("RTP Thread created; Thread ID: "+thread_id);
	      
	      while (true) {
	    	 
	    	  if (inputStr.ready()) {
	    	    
	    		  System.out.println("Received a msg in RTSP thread");
	    		  String requestLine = inputStr.readLine();	
	    		  String cseqLine = inputStr.readLine();	
	    		  String transportLine = inputStr.readLine();
       
	    		  String [] strTokens = requestLine.split(" ");
	    		  String messageType = strTokens[0];
          
	    		  strTokens = cseqLine.split(" ");
	    		  String seqNum = strTokens[1];
          
	    		  strTokens = transportLine.split(" ");
	    		  String[] members = strTokens[1].split(";");
	    		  boolean multicast = ((members[1] == "multicast")?true:false);
	    		  String clientRTPPort = (members[2].split("="))[1];
        
	    		  int sID = 0;
	    		  if (!messageType.equals("SETUP")){
	    			  String SID = inputStr.readLine();
	    			  strTokens = SID.split(" ");
	            	  sID = Integer.parseInt(strTokens[1]);
	    		  }
          	
	    		  if (sID != this.sessid) {
	    			  System.out.println("ERROR: Session ID mismatch");
	    		  }
       
	    		  /* Print the Incoming Message*/
	    		  System.out.println("=====================================");
	    		  System.out.println("MESSAGE RECIEVED: "+"\n");
	    		  System.out.println(requestLine); 
	    		  System.out.println(cseqLine);
	    		  System.out.println(transportLine);
      
       
	    		  /* Let RTSP Handler take care of this message and send response*/
	    		  System.out.println("=====================================");
	    		  System.out.println("INFO : messageType = "+messageType);
	    		  System.out.println("INFO : seqNum = "+seqNum);
	    		  System.out.println("INFO : sessId = "+sessid);
	    		  System.out.println("INFO : clientRTPPort = "+clientRTPPort);
	    		  System.out.println("INFO : multicast = "+ ((multicast)?"true":"false"));
        
	    	 
	    		  PrintWriter  resp = new PrintWriter(socket.getOutputStream(), true);
	    		  resp.write("RTSP/1.0 200 OK"+"\r\n");
	    		  resp.write("CSeq: "+seqNum+"\r\n");
	    			//resp.write("Date: "+date+NEWLINE);
	    		  resp.write("SessionID: "+sessid+"\r\n");
	    		  resp.flush();
	    			
	    	
	    		  
	    		  switch (messageType) {
	    		  case "SETUP":
	    			        t.setVideoSrc(video_file);
	    			        System.out.println("Thread " + thread_id + " Streaming is Set up");
	    			        System.out.println("Video File to be played: “+ video_file); 
	    			                          break;

	    	  
	    		  case "PLAY": 
	    		  			
	    		  			t.setStreaming(true);
	    		  			System.out.println("Thread "+thread_id+" Streaming Started");
	    		  			break;
	    		  			
	    		 			
	    		  case "PAUSE":	
	    		  	        
	    			        System.out.println("Thread "+thread_id+" Streaming Paused");
	    		  			t.setStreaming(false);
	    		  			break;
	    		  			
	    		  case "TEARDOWN" : 
	    			        System.out.println("Thread "+thread_id+" Streaming destroyed");
	    			        t.setStreaming(false);
	    		  			break;
	    		  			
	    		  default: System.out.println("ERROR: Unknown Message Type");
	    		            break;
	    		  }
	    	  }
	      }
	    
	    } catch (IOException ioe) {
	    	System.out.println(ioe);
	    } catch (Exception e) {
			e.printStackTrace();
		}
	   
}
  
}
//RAKESH: END
  
  
  

  
