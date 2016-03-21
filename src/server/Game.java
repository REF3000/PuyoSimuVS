package server;

import java.util.ArrayList;

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
		if( x<1 || x>6 || y<1 || y>13 ) return;
		field[ (y-1)*W + (x-1) ] = puyo;
	}
	int set( int x, int puyo ){
		int y = getHeight(x)+1;
		set(x,y,puyo);
		return y;
	}
	int set( Tumo tumo, Action act ){
		int ax = act.pos;
		int sx = ax;
		if( act.dir==1 ) sx++;
		if( act.dir==3 ) sx--;
		int y;
		if( act.dir==2 ){
			set( sx, tumo.second );
			y = set( ax, tumo.first );
		} else {
			y = set( ax, tumo.first );
			set( sx, tumo.second );
		}
		return 13-y;
	}
	int get( int x, int y ){
		if( x<1 || x>6 || y<1 || y>13 ) return -1;
		return field[ (y-1)*W + (x-1) ];
	}
	void init(){
		for(int i=0; i<H*W; ++i ) field[i] = 0;
	}
	void print(){
		System.out.println("Field.print()");
		for( int y=13; y>=1; --y ){
			for( int x=1; x<=6; ++x ){
				System.out.printf("%d",get(x,y));
			}
			System.out.println("");
		}
	}
	int getHeight( int x ){
		for( int y=1; y<=13; ++y ){
			if( get(x,y)==0 ) return y-1;
		}
		return 13;
	}
	int doCountConnection( int x, int y, int puyo, int[] flag ){
		if( !( 1<=x && x<=6 && 1<=y && y<=12 ) ) return 0;
		if( flag[(x-1)+(y-1)*W]==1 ) return 0;
		if( get(x,y)!=puyo ) return 0;
		flag[(x-1)+(y-1)*W] = 1;
		return doCountConnection( x, y+1, puyo, flag ) +
				doCountConnection( x+1, y, puyo, flag ) +
				doCountConnection( x, y-1, puyo, flag ) +
				doCountConnection( x-1, y, puyo, flag ) + 1;
	}
	int doDeleteConnection( int x, int y, int puyo ){
		if( !( 1<=x && x<=6 && 1<=y && y<=12 ) ) return 0;
		if( get(x,y)==9 ){ set(x,y,0); return 0; } // おじゃま削除
		if( get(x,y)!=puyo ) return 0;
		set(x,y,0);
		return doDeleteConnection( x, y+1, puyo ) +
				doDeleteConnection( x+1, y, puyo ) +
				doDeleteConnection( x, y-1, puyo ) +
				doDeleteConnection( x-1, y, puyo ) + 1;
	}
	int countConnection( int x, int y ){
		int puyo = get(x,y);
		if( !(1<=puyo && puyo<=4) ) return 0;
		int flag[] = new int[W*H];
		return doCountConnection( x, y, puyo, flag );
	}
	int deleteConnection( int x, int y ){
		int puyo = get(x,y);
		if( !(1<=puyo && puyo<=4) ) return 0;
		return doDeleteConnection( x, y, puyo );
	}
	void fall(){
		for( int x=1; x<=6; ++x ){
			ArrayList<Integer> a = new ArrayList<Integer>();
			for( int y=1; y<=13; ++y ){
				int p = get(x,y);
				if( p!=0 ) a.add( p );
				set(x,y,0);
			}
			for( int y=1; y<=a.size(); ++y )
				set(x,y,a.get(y-1));
		}		
	}
	boolean canFire(){
		for( int x=1; x<=6; ++x ){
			for( int y=1; y<=12; ++y ){
				int n = countConnection( x, y );
				if( n>=4 ) return true;
			}
		}
		return false;
	}
	void step(){
		for( int x=1; x<=6; ++x ){
			for( int y=1; y<=12; ++y ){
				int n = countConnection( x, y );
				if( n>=4 ) deleteConnection( x, y );
			}
		}
		fall();
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
	private int[] OJAMA_TABLE = {1,4,3,6,2,5};

	private boolean ready[]   = new boolean[2];

	private String  name[]    = new String[2];	
	private Field   field[]   = new Field[2];
	private Action  action[]  = new Action[2];

	private Next next = new Next();

	private int turn;
	private int score[] = new int[2];
	private int status[] = new int[2];
	private ArrayList<Action> history0 = new ArrayList<Action>();
	private ArrayList<Action> history1 = new ArrayList<Action>();
	private int ojama_notice[] = new int[2];
	private int ojama_stock[] = new int[2];
	private int ojama_count[] = new int[2];
	private int ojama_score[] = new int[2];
	private int chain_count[] = new int[2];
	
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
			action[i].id = -1;
			ready[i] = false;
			score[i] = 0;
			status[i] = 0;
			ojama_notice[i] = 0;
			ojama_count[i] = 0;
			chain_count[i] = 0;
			ojama_score[i] = 0;
			ojama_stock[i] = 0;
		}
		turn = 0;
		history0.clear();
		history1.clear();
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
	synchronized public void setAction( int player_id, Action act ){
		System.out.printf("setAction(%d,%d,%d)\n",act.id,act.pos,act.dir);
		action[player_id-1] = act;
	}
	public Next getNext(){
		return next;
	}
	
	Tumo getTumo( int id, int num ){ // num手先のツモを返す
		int cnt = 0; // 現在までのパスでない手数を数える
		for( int i=0; i<getHistory(id).size(); ++i ) if(getHistory(id).get(i).id==1) cnt++;
		return next.get(cnt+num);
	}
	void addScore( int id, int score ){
		this.score[id] += score;
		ojama_score[id] += score;
	}
	private ArrayList<Action> getHistory( int id ){
		if( id==0 ) return history0;
		return history1;
	}
	private void fallOjama( int id ){
		if( ojama_notice[id]<=0 ) return;
		int num = (ojama_notice[id]>30) ? 30 : ojama_notice[id];
		for( int i=0; i<num; ++i ){
			final int P = ojama_count[id]++%6;
			field[id].set(OJAMA_TABLE[P],9); // TODO:OJAMAは定数
		}
		ojama_notice[id] -= num;
	}
	void transmitOjama( int id ){
		if( ojama_stock[id]<=0 ) return;
		ojama_notice[id] += ojama_stock[id];
		ojama_stock[id] = 0;
	}
	void processOjamaScore( int id ){
		int ojama = ojama_score[id]/70;
		ojama_score[id] -= ojama*70;
		// 相殺（notice）
		if( ojama<ojama_notice[id] ){
			ojama_notice[id] -= ojama;
			return;
		}
		ojama -= ojama_notice[id];
		ojama_notice[id] = 0;
		// 相殺（stock）
		if( ojama<ojama_stock[id] ){
			ojama_stock[id] -= ojama;
			return;
		}
		ojama -= ojama_stock[id];
		ojama_stock[id] = 0;
		// 敵おじゃま予告増加
		ojama_stock[(id+1)%2] += ojama;
	}
	void forwardChain( int id ){
		++chain_count[id];
		final int A[] = {0,8,16,32,64,96,128,160,192,224,256,288,320,352,384,416,448,480,512};
		final int B[] = {0,3,6,12,24};
		final int C[] = {0,2,3,4,5,6,7,10};
		int score = 0;
		int bonas = A[chain_count[id]-1];
		int color_flag[] = {0,0,0,0,0};
		for( int x=1; x<=6; ++x ){
			for( int y=1; y<=12; ++y ){
				int n = field[id].countConnection(x,y);
				if( n>=4 ){
					score += n*10;
					color_flag[field[id].get(x,y)] = 1;
					field[id].deleteConnection(x,y);
					bonas += C[(n<11)?(n-4):10];
				}
			}
		}
		int color_cnt=0;
		for(int i=1; i<=4; ++i ) color_cnt+=color_flag[i];
		bonas += B[color_cnt-1];
		if( bonas==0 ) bonas=1;
		addScore( id, score*bonas );
		processOjamaScore( id );
	}
	
	private boolean isDeath( int id ){
		if( field[id].get(3,12)==0 ) return false;
		if( field[id].canFire() ) return false;
		return true;
	}
	public void next(){
		System.out.println("Game.next()");
		boolean[] ojama_trans_flag={false,false};
		for( int id=0; id<2; ++id ){
			ready[id] = false;
			if( action[id].id==-1 ) continue;
			if( status[id]==0 ){ // 非連鎖中
				if( action[id].id==1 ){
					int fall_d = field[id].set(getTumo(id,0),action[id]);
					ArrayList<Action> his = getHistory(id);
					if( !his.isEmpty() && his.get(his.size()-1).id==0 ) fall_d = 0;
					addScore( id, fall_d ); // 前回がパス以外なら落下ボーナス付与
					if( field[id].canFire() ){
						status[id] = 1;
						//continue;
					} else {
						// TODO:窒息判定
						fallOjama( id );
						// TODO:窒息判定2
						if( isDeath( id ) ) status[id] = 2;
					}
				}
			} else {             // 連鎖中
				action[id].id = 0;
				forwardChain(id);
				field[id].fall();
				if( field[id].canFire() ){
					//continue;
				} else {
					if( checkAllClear(id) ) addScore( id, 2100 );
					// TODO:窒息判定
					fallOjama( id );
					ojama_trans_flag[ (id+1)%2 ] = true;
					// TODO:窒息判定2
					if( isDeath( id ) ) status[id] = 2;
					else status[id] = 0;
					chain_count[id] = 0;
				}
			}
			if( id==0 ) history0.add(action[id]);
			if( id==1 ) history1.add(action[id]);
			//field[id].print();
			//System.out.println(score[id]);
		}
		for( int id=0; id<2; ++id ){
			if( ojama_trans_flag[id] ) transmitOjama( id );
		}
		turn++;
	}
	
	boolean checkAllClear( int id ){
		for( int x=1; x<=6; ++x )
			for( int y=1; y<=13; ++y ) if( field[id].get(x,y)!=0 ) return false;
		return true;
	}
	
	/**
	 * ゲームの状態を返す
	 * 0:試合中、1:1P勝利、2:2P勝利、3:引き分け
	 */
	public int getStatus(){
		if( status[0]==2 && status[1]==2 ) return 3;
		if( status[0]==2 ) return 2;
		if( status[1]==2 ) return 1;
		return 0;
	}
	
	public int[] getOjamaTable(){
		return OJAMA_TABLE;
	}
}
