package server;

class Tumo{
	public int first;
	public int second;
}
class Next{
	Tumo[] table = new Tumo[128];
	Next(){
		for( int i=0; i<128; ++i ) table[i] = new Tumo();
		init();
	}
	void init(){
		for(int i=0; i<128; ++i){
			table[i].first  = (int)(Math.random()*4)+1;
			table[i].second = (int)(Math.random()*4)+1;
		}
	}
	Tumo get( int num ){
		return table[num%128];
	}
}
class Field{
	static final int H = 13;
	static final int W = 6;
	public int field[];
	Field(){
		field = new int[H*W];
	}
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
	Action(){
		id = 0; pos = 0; dir = 0;
	}
	Action( int id, int pos, int dir ){
		this.id = id; this.pos = pos; this.dir = dir;
	}
}

/**
 * ゲーム情報を管理してるクラス
 * いろいろガバガバ
 * TODO: 範囲外とかの対応
 */
public class Game {
	
	private String  name[]    = new String[2];	
	private Field   field[]   = new Field[2];
	private Action  action[]  = new Action[2];
	private boolean ready[]   = new boolean[2];
	
	private Next next = new Next();
	
	Game( String p1, String p2 ){
		name[0] = p1;
		name[1] = p2;
		field[0] = new Field();
		field[1] = new Field();
		action[0] = new Action();
		action[1] = new Action();
		init();
	}
	void init(){
		for(int i=0; i<2; ++i){
			field[i].init();
			action[i].id = 0;
			ready[i] = false;
		}
		next.init();
	}
	public String getName( int id ){
		return name[id-1];
	}
	synchronized public void setReady( int id, boolean b ){
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
	synchronized public void setAction( int id, Action act ){
		System.out.printf("setAction(%d,%d,%d)\n",act.id,act.pos,act.dir);
		action[id-1] = act;
	}
	public Next getNext(){
		return next;
	}
	
	public void next(){
		System.out.println("Game.next()");
		
		ready[0] = false;
		ready[1] = false;
	}
}
