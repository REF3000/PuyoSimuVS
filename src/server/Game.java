package server;

/**
 * ゲーム情報を管理してるクラス
 * いろいろガバガバ
 */
public class Game {
	
	static final int H = 13;
	static final int W = 6;
	
	class Field{
		public int field[] = new int[H*W];
		// TODO:範囲外の対応
		void set( int x, int y , int puyo ){
			field[ y*W + x ] = puyo;
		}
		int get( int x, int y ){
			return field[ y*W + x ];
		}
		void init(){
			for(int i=0; i<H*W; ++i ) field[i] = 0;
		}
	}
	class Action{
		public int id; // 1:設置 2:パス（未実装） 3:サレンダー（未実装）
		public int pos;
		public int dir;
	}
	
	private String  name[]    = new String[2];	
	private Field   field[]   = new Field[2];
	private Action  action[]  = new Action[2];
	private boolean ready[]   = new boolean[2];
	
	
	Game( String p1, String p2 ){
		name[0] = p1;
		name[1] = p2;
		init();
	}
	void init(){
		for(int i=0; i<2; ++i){
			field[i].init();
			action[i].id = 0;
			ready[i] = false;
		}
	}
	public String getName( int id ){
		return name[id-1];
	}
	public void setReady( int id, boolean b ){
		ready[id-1] = b;
	}
	public boolean getReady( int id ){
		return ready[id-1];
	}
	public Field getField( int id ){
		return field[id-1];
	}
	public Action getAction( int id ){
		return action[id-1];
	}
	
}
