import java.io.*;
import java.net.*;

/**
 * Protocolに則り通信を行うクラス
 * スレッドで動作する
 */
class Connection extends Thread {
	static final int BUFFER_SIZE = 256;
	static final int HEADER_SIZE = 2;
	static final int KEEP_ALIVE_MS = 10*60*1000;

	enum eStatus{
		WAITING,
		SET_NAME,
		DEBUG,
	}

	Socket      m_socket;     // 
	InputStream m_in;         // 
	byte[]      m_buffer;     // 読み取ったデータ用バッファ
	int         m_data_size;  // 読み取った/読み取るデータサイズ
	eStatus     m_status;

	byte[]      m_name;

	public Connection( Socket socket ) throws IOException{
		m_socket = socket;
		m_in     = m_socket.getInputStream();
		m_buffer = new byte[BUFFER_SIZE];
		resetStatus();
		m_name   = new byte[0];
	}
	void resetStatus(){
		m_data_size = HEADER_SIZE;
		m_status = eStatus.WAITING;
	}

	public void run(){
		if( m_socket==null ) return;
		try{
			int lim = KEEP_ALIVE_MS;
			while(!m_socket.isClosed()){
				// 読み込めるだけのデータが送られるまで待機
				// また無通信でKEEP_ALIVE_MS時間経ったら終了処理
				if( lim-- < 0 ) break;
				Thread.sleep(1);
				if( !checkAvailable() ) continue; // データの読み込みが可能か調べる
				if( !read() ) break; // データを読み込む
				process();
				lim = KEEP_ALIVE_MS;
			}
		} catch ( Exception e ){
			e.printStackTrace();
		} finally {
			// ソケットのクローズ
			System.out.println("Close:" + m_socket);
			try {
				m_socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 次のデータを読み込めるか調べる
	 * @return 読み込みが可能ならtrue
	 * @throws IOException 
	 */
	boolean checkAvailable() throws IOException{
		int size = m_in.available();
		return (size>=m_data_size);
	}

	/**
	 * データを読み込む
	 * @return 読み込みが成功したらtrue
	 * @throws IOException 
	 */
	boolean read() throws IOException{
		if( m_in.read(m_buffer, 0, m_data_size)==-1 )
			return false;
		return true;
	}

	/**
	 * 受信したデータを処理する
	 */
	void process(){
		switch( m_status ){
		case WAITING:
			doWaiting();
			break;
		case SET_NAME:
			doSetName();
			break;
		case DEBUG:
			doDebug();
		default:
			resetStatus();
			break;
		}
		System.out.println(">process end.");
	}

	void doWaiting(){
		switch( m_buffer[0] ){
		case 1:
			m_status = eStatus.SET_NAME;
			break;
		case 9:
			doDebug();
			resetStatus();
			return;
		case 10:
			m_status = eStatus.DEBUG;
			break;
		default:
			System.out.println("command not found.");
			resetStatus();
			return;
		}
		m_data_size = m_buffer[1];
	}
	void doSetName(){
		System.out.println("SetName");
		m_name = new byte[m_data_size];
		System.arraycopy(m_buffer, 0, m_name, 0, m_data_size);		
		resetStatus();
	}
	void doDebug(){
		System.out.print("debug:");
		for( int i=0; i<m_name.length; ++i ){
			System.out.printf("%02X ",m_name[i]);
		}
		System.out.println();
		resetStatus();
	}
}


public class MatchingManager {

	public MatchingManager(){
	}

	/**
	 * 新しい接続を追加する。
	 * 接続はその後MatchingProtocolに則って通信を行う。
	 * @param socket
	 * @throws IOException 
	 */
	public void addSocket( Socket socket ) throws IOException{
		System.out.println( "Connect:"+socket );
		Connection con = new Connection( socket );
		con.start();
	}

}
