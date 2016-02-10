package client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ClientMain {
	
	static Socket socket;

	public static void main(String[] args) throws UnknownHostException, IOException {
		String hostname = "localhost";
		int    port     = 2424;
		if( args.length>0 ) port = Integer.parseInt(args[0]); 
		socket = new Socket(hostname, port);
		
		Scanner scan = new Scanner(System.in);
		end: while(true){
			String buf = scan.next();
			switch( buf ){
			case "e":
			case "exit":
				break end;
			}
		}
		
	}

}
