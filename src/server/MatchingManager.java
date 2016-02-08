package server;
import java.io.*;
import java.net.*;
import java.util.LinkedList;

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

	private Socket      m_socket;     // 
	private InputStream m_in;         // 
	private byte[]      m_buffer;     // 読み取ったデータ用バッファ
	private int         m_data_size;  // 読み取った/読み取るデータサイズ
	private eStatus     m_status;

	private String      m_name = "NO_NAME";

	private GameManager     m_game_manager = null;
	private MatchingManager m_matching_manager;
	private int m_id;

	public Connection( Socket socket, MatchingManager mm ) throws IOException{
		m_socket = socket;
		m_in     = m_socket.getInputStream();
		m_buffer = new byte[BUFFER_SIZE];
		resetStatus();
		m_matching_manager = mm;
	}
	void resetStatus(){
		m_data_size = HEADER_SIZE;
		m_status = eStatus.WAITING;
	}
	public void setGameManager( GameManager gm, int id ){
		m_game_manager = gm;
		m_id = id;
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
		// WAITINGはhead部の処理
		// それ以外はbody部の処理
		switch( m_status ){
		case WAITING:
			doWaiting();
			break;
		case SET_NAME:
			doSetName();
			break;
		case DEBUG:
			doDebug();
			break;
		default:
			resetStatus();
			break;
		}
		System.out.println(">process end.");
	}

	void doWaiting(){
		// head部の処理
		// body部があればbreak 無ければreturn
		switch( m_buffer[0] ){
		case 1:
			m_status = eStatus.SET_NAME;
			break;
		case 2:
			doJoinRandom();
			return;
		case 3:
			doSetReady();
			return;
		case 9:
			doDebug();
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
		m_name = new String( m_buffer, 0, m_data_size );
		resetStatus();
	}
	public String getPlayerName(){
		return m_name;
	}
	void doJoinRandom(){
		m_matching_manager.addConnectionRandom(this);
		resetStatus();
	}
	void doSetReady(){
		if( m_game_manager==null ) return;
		m_game_manager.setReady(m_id);
		resetStatus();
	}

	void doDebug(){
		System.out.print("debug:");
		//System.out.println(m_name);
		System.out.println( m_game_manager.getEnemyName(m_id) );
		resetStatus();
	}
	
	void send( byte[] data ) throws IOException{
		OutputStream out = m_socket.getOutputStream();
		out.write( data );
		out.flush();
	}
	public void sendMatchingNotice() throws IOException{
		byte[] buf = new byte[2];
		buf[0] = 0x01;
		buf[1] = 0x02;
		send( buf );
	}
}

/**
 * ゲームを管理するクラス
 */
class GameManager extends Thread {
	
	Game m_game;
	
	GameManager( Connection con1, Connection con2 ) throws IOException{
		con1.setGameManager( this, 1 );
		con2.setGameManager( this, 2 );
		m_game = new Game( con1.getPlayerName(), con2.getPlayerName() );
		con1.sendMatchingNotice();
		con2.sendMatchingNotice();
	}

	public void run(){
		try {
			while(true){
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public String getEnemyName( int id ){
		if( id==1 ) return m_game.getName(2);
		return m_game.getName(1);
	}
	public void setReady( int id ){
		m_game.setReady( id, true );
	}
}

/**
 * マッチングを管理するクラス
 */
public class MatchingManager extends Thread {

	LinkedList<Connection> m_random_connection;
	
	public MatchingManager(){
		m_random_connection = new LinkedList<Connection>();
	}
	
	/**
	 * 新しい接続を追加する。
	 * 接続はその後Protocolに則って通信を行う。
	 * @param socket
	 * @throws IOException 
	 */
	public void addSocket( Socket socket ) throws IOException{
		System.out.println( "Connect:"+socket );
		Connection con = new Connection( socket, this );
		con.start();
	}

	/**
	 * random_connectionに２つ以上登録されていれば
	 * マッチングしてゲームを開始する。
	 */
	public void run(){
		try {
			while(true){
				Thread.sleep(100);
				if( m_random_connection.size() >= 2 ){
					Connection con1 = m_random_connection.poll();
					Connection con2 = m_random_connection.poll();
					// TODO:ここで接続が生きてるか確認したほうがいいかも？
					System.out.println("Matching成立");
					GameManager gm = new GameManager( con1, con2 );
					gm.start();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * randomに名乗りをあげる
	 * @param con
	 */
	public void addConnectionRandom( Connection con ){
		m_random_connection.add(con);
	}
	
}
