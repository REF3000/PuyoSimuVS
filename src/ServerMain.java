import java.io.*;
import java.net.*;

public class ServerMain {

	private static ServerSocket server;
	private static MatchingManager  gm;
	
	/**
	 * args[0]番ポート で接続待ちして、接続があったらGameManagerにsocketを投げる
	 * 引数なしのデフォルトは2424
	 */
	public static void main(String[] args) throws IOException {
		int port = 2427;
		if( args.length>0 ) port = Integer.parseInt(args[0]); 
		
		server = new ServerSocket(port);
		gm     = new MatchingManager();
		while(true){ // TODO:ここ無限ループだけど、実装これで本当にいいのか確認する。
			Socket socket = server.accept();
			gm.addSocket( socket );
		}		
	}
	
	@Override
	protected void finalize() throws Throwable {
	    try{
	        super.finalize();
	    }finally{
	    	server.close();
	    }
	}
}
