package client;

import java.io.IOException;
import java.io.OutputStream;
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
		byte[] buf = new byte[64];
		Scanner scan = new Scanner(System.in);
		end: while(true){
			System.out.print("command:");
			String tmp = scan.next();
			switch( tmp ){
			case "e":
			case "exit":
				break end;
			case "n":
			case "name":
				System.out.print("name:");
				tmp = scan.next();
				buf[0] = 0x01;
				byte[] name = tmp.getBytes("UTF-8");
				buf[1] = (byte)name.length;
				System.arraycopy(name, 0, buf, 2, name.length);
				send( buf, 2+name.length );
				break;
			case "j":
			case "join":
				buf[0] = 0x02;
				buf[1] = 0x00;
				send( buf, 2 );
				break;
			case "r":
			case "ready":
				buf[0] = 0x03;
				buf[1] = 0x00;
				send( buf, 2 );
				break;
			case "d":
			case "debug":
				buf[0] = 0x09;
				buf[1] = 0x00;
				send( buf, 2 );
				break;
			}
		}
		scan.close();
	}
	
	static void send( byte[] b, int len ) throws IOException{
		OutputStream os = socket.getOutputStream();
		os.write(b,0,len);
		os.flush();
	}

}
