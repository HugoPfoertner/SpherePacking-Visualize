
package sphp;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.applet.*;
import java.util.*;
import java.util.List;

public class SpherePacking extends Applet {


	public class PackingConf {
		PackingConf(int n) {
			coords = new double[n][4];
		}
		double radius;
		double requiredVolume;
		double density;
		double[][] coords;
		List symlist = new LinkedList();
	}

	public class Symmetry {
		Symmetry(double e) {
			eps=e;
		}
		double eps;
		String description="?";
		List oplist = new LinkedList();
	}

	public class PackingView extends Canvas implements Comparator, MouseListener, MouseMotionListener, KeyListener {
		private Image buffer;
		private double lqx=-1;
		private double lqy=-1.5f;
		private double lqz=2; //light

		private int p=1;
		private int n=1;
		private int s=0;
		private int op=0;

		private int si = 0;
		private int sgn = 1;

		private boolean transparent=false;
		private boolean touches=false;
		
		private int mouse_x=-1;
		private int mouse_y=-1;
		boolean moved=false;
		private double zoomf=1.0;
		private int angle_x=0;//angle rel. si, sgn
		private int angle_y=0;
		private int angle_z=0;

		private double[][] coords = confs[0].coords;
		private double[][] squareCoords;
		private double[][] squareCoordsSorted;
		
		private BufferedImage[] sphImages = null;
		private boolean[] sphereVisible = new boolean[]{true};

		public PackingView(int n, int p, int xyz, int s, int op, boolean tr, boolean tc) {
			super();
			setBackground(Color.WHITE);
			addMouseListener(this);
			addMouseMotionListener(this);
			addKeyListener(this);
			reSet(n, p, xyz, s, op, tr, tc);
			
		}

		private void refreshSquareCoords() {
			squareCoords = new double[8][4];
			squareCoordsSorted = new double[8][4];
			for (int i = 0; i < 8; i++) {
				for(int j=0; j<4; j++) {
					squareCoords[i][j] = squareCoordsSorted[i][j] = SQUARE_COORDS[i][j];
				}
			}
			Arrays.sort(squareCoordsSorted, this);
		}

		private void paintSphere(BufferedImage img, int xx, int yy, int r, int color) {
			//System.out.println("xx="+xx+" yy="+yy+" r="+r+" color="+Integer.toHexString(color));
			int x,y;
			int x2,y2,r2=r*r;
			int c;
			double w;
			double wlq = Math.sqrt(lqx*lqx+lqy*lqy+lqz*lqz);
			if(r<0) r=+r;
			Color c0 = new Color(color);
			float[] hsbvals = new float[4];
			for(y=-r;y<r+1;y++) {
				y2=y*y;
				for(x=-r;x<r+1;x++) {
					x2=x*x;
					if(x2+y2<=r2) { //innerhalb des aufsichtskreises
						w=(x*lqx+y*lqy+Math.sqrt(r2-x2-y2)*lqz)
							/r //cosinus des differenzwinkels zur lichtquelle
							/wlq;
						if(w>0.999) img.setRGB(xx+x,yy+y,-1);//reflection
						else {
							if(w<0.4) w=0.4; //diffuse
							int alpha = transparent ? 0x7F000000 : 0xFF000000;
							int rgb = ((int)(c0.getRed()*w)<<16)+((int)(c0.getGreen()*w)<<8)+(int)(c0.getBlue()*w);
							//try for fractal view: rgb = (int)((color&0xFF0000)*w)+(int)((color&0xFF00)*w)+(int)((color&0xFF)*w);
							img.setRGB(xx+x,yy+y,alpha+rgb);
							/* 
							Color col = transparent
								? new Color((int)(c0.getRed()*w),(int)(c0.getGreen()*w),(int)(c0.getBlue()*w),128)
								: new Color((int)(c0.getRed()*w),(int)(c0.getGreen()*w),(int)(c0.getBlue()*w));
							g.setColor(col);
							*/
						}
						//g.drawLine(xx+x,yy+y,xx+x,yy+y); needs much time!!!
					}
					
				}
			}
		}

		//nn is only for symmetries (count of groups)
		private void createSphereImage(int r, int i, int nn) {
			if(sphImages==null) {
				sphImages=new BufferedImage[n];
			}
			sphImages[i] = new BufferedImage(2*r+1,2*r+1,BufferedImage.TYPE_INT_ARGB);
			int col = Color.WHITE.getRGB();
			if(s>0) {
				if(i>0) col = Color.getHSBColor(((float) i-1)/(nn-1),1f-(((float)i%3)/3),1f).getRGB(); 
			} else {
				col = Color.getHSBColor(((float) i)/n,1f,1f).getRGB();
				//(cc&1)*0xFF + ((cc&2)>>1)*0xFF00 + ((cc&4)>>2)*0xFF0000
			}
			paintSphere(sphImages[i], r,r,r, col);
		}

		private void rotate(double[] co, double angle_x, double angle_y, double angle_z) {
			double px = co[0];
			double py = co[1];
			double pz = co[2];
			if(angle_x!=0.0) {
				// Rotation um x-Achse
				double py1 = py * Math.cos(angle_x) - pz * Math.sin(angle_x);
				pz = py * Math.sin(angle_x) + pz * Math.cos(angle_x);
				py = py1;
			}
			if(angle_y!=0.0) {
				// Rotation um y-Achse
				double px1 = px * Math.cos(angle_y) + pz * Math.sin(angle_y);
				pz = -px * Math.sin(angle_y) + pz * Math.cos(angle_y);
				px = px1;
			}
			if(angle_z!=0.0) {
				// Rotation um z-Achse
				double px1 = px * Math.cos(angle_z) - py * Math.sin(angle_z);
				py = py * Math.cos(angle_z) + px * Math.sin(angle_z);
				px = px1;
			}
			co[0]=px;
			co[1]=py;
			co[2]=pz;
		}

		private void rotate(double ax, double ay, double az) {
			for(int q=0; q<n; q++) {
				rotate(coords[q],ax, ay, az);
			}
			for(int i = 0; i < 8; i++) {
				rotate(squareCoords[i],ax, ay, az);
			}
			Arrays.sort(coords, this);
			squareCoordsSorted = (double[][]) squareCoords.clone();
			Arrays.sort(squareCoordsSorted, this);
			moved=true;
		}

		public void rotate(int ax, int ay, int az) {
			angle_x=(angle_x+ax+360)%360;
			angle_y=(angle_y+ay+360)%360;
			angle_z=(angle_z+az+360)%360;
			rotate(ax*Math.PI/180,ay*Math.PI/180,az*Math.PI/180);
		}

		private int getX(int q, int w) {
			return (int) ((coords[q][0]*zoomf+1)*w/2);
		}

		private int getY(int q, int w) {
			return (int) (w-(coords[q][1]*zoomf+1)*w/2);
		}

		public void update(Graphics g) {
			paint(g);
		}

		//returns null if other distance as r
		private double[] wallPoint(int q, double r, int p, int n) {
			double[] x = null;
			double[] qv = coords[q];
			double[] pv = squareCoords[p];
			double[] nv = (double[]) squareCoords[n].clone();
			nv[0]-=pv[0]; nv[1]-=pv[1]; nv[2]-=pv[2];
			double pq_n = (qv[0]-pv[0])*nv[0] + (qv[1]-pv[1])*nv[1] + (qv[2]-pv[2])*nv[2];
			double n_ = 2;//Math.sqrt( nv[0]*nv[0] + nv[1]*nv[1] + nv[2]*nv[2] );//2
			double d = Math.abs(pq_n)/Math.abs(n_);
			if(Math.abs(d-r)<EPS) {
				x = new double[3];
				double a = pq_n/n_/n_;
				x[0]=qv[0]-a*nv[0]; x[1]=qv[1]-a*nv[1]; x[2]=qv[2]-a*nv[2];
			}
			return x;
		}

		public synchronized void paint(Graphics g) {
			// Double-Buffering
			if (buffer == null || buffer.getWidth(null)!=getWidth() || buffer.getHeight(null)!=getHeight()) {
				buffer = createImage(getWidth(), getHeight());
				//buffer = new BufferedImage(getWidth(), getHeight(),BufferedImage.TYPE_4BYTE_ABGR_PRE);
				
				Graphics2D g2D = (Graphics2D) buffer.getGraphics();
				g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,	RenderingHints.VALUE_ANTIALIAS_ON);
				//g2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,	RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
				
			}

			Graphics gBuffer = buffer.getGraphics();
		

			gBuffer.setColor(Color.WHITE);
			gBuffer.fillRect(0,0,getWidth(),getHeight());

			int w=Math.min(getWidth(),getHeight());

			//Raster or Wireframe
			gBuffer.setColor(Color.LIGHT_GRAY);
			if(!moved) {			
				for(int x=1; x<21; x++) {
					gBuffer.drawLine(0,x*w/20-1,w-1,x*w/20-1);
				}
				for(int x=1; x<21; x++) {
					gBuffer.drawLine(x*w/20-1,0,x*w/20-1,w-1);
				}
			} else { //first sqare wireframes
				for (int i = 0; i < 7; i++) {
					double[] edge0 = squareCoordsSorted[i];
					int ei = (int) edge0[3]; //original index
					for (int j = 0; j < 3; j++) {
						double[] edge1 = squareCoords[SQUARE_NB[ei][j]];
						gBuffer.drawLine(
							(int) ((edge0[0]*zoomf+1)*w/2), (int) (w-(edge0[1]*zoomf+1)*w/2),
							(int) ((edge1[0]*zoomf+1)*w/2), (int) (w-(edge1[1]*zoomf+1)*w/2)
						);
					}
				}
			}

			//PackingConf conf=confs[n-1];
			//Arrays.sort(conf.coords, this);
			for(int q=0; q<n; q++) {				
				int oi = (int) coords[q][3]; //original index
				if(sphereVisible[oi-1]) {
					int cc = oi-1;//(oi-1)%6+1;
					int nn = n;
					if(s>0) {
						if(op>0) {
							Symmetry sym = (Symmetry) confs[n-1].symlist.get(s-1);
							int[] oa = (int[]) sym.oplist.get(op-1);
							cc = (oa[oi-1]);//%6+1;
							nn = oa[n];
						} else cc=0;//1
					}

					int xx=getX(q,w);
					int yy=getY(q,w);
				
					double radius = confs[n-1].radius;
					int r=(int) (radius*w*zoomf/2);
					if(sphImages==null || sphImages[cc]==null) {
						//if(transparent) g.drawString("Please wait!",100,100);
						createSphereImage(r,cc,nn);
					}
				
					gBuffer.drawImage(sphImages[cc],xx-r,yy-r,this);//-1
					if(touches) {
						gBuffer.setColor(	transparent ? new Color(0,0,0,128) : Color.BLACK);
						//System.out.println("xx="+xx+" yy="+yy+" r="+r+" w="+w);
						for (int j = 0; j < 3; j++) {
							double[] wp = wallPoint(q, radius, 0, SQUARE_NB[0][j]);
							if(wp!=null) {
								gBuffer.drawLine(
									xx,yy,
									(int) ((wp[0]*zoomf+1)*w/2), (int) (w-(wp[1]*zoomf+1)*w/2)
								);
							}
							wp = wallPoint(q, radius, 7, SQUARE_NB[7][j]);
							if(wp!=null) {
								gBuffer.drawLine(
									xx,yy,
									(int) ((wp[0]*zoomf+1)*w/2), (int) (w-(wp[1]*zoomf+1)*w/2)
								);
							}
						}
						/*				
						if(Math.abs(xx-r)<2) gBuffer.drawLine(xx-10,yy,0,yy);
						if(Math.abs(xx+r-w)<2) gBuffer.drawLine(xx+10,yy,w,yy);
						if(Math.abs(yy-w+r)<2) gBuffer.drawLine(xx,yy+10,xx, w);
						if(Math.abs(yy-r)<2) gBuffer.drawLine(xx,yy-10,xx, 0);
						*/
						gBuffer.setColor(	transparent ? new Color(0xC0,0xC0,0xC0,128) : Color.LIGHT_GRAY);
						for(int qq=q-1;qq>=0;qq--) {
							double d0 = coords[q][0]-coords[qq][0];
							double d1 = coords[q][1]-coords[qq][1];
							double d2 = coords[q][2]-coords[qq][2];
							if(Math.sqrt(d0*d0+d1*d1+d2*d2)-radius*2<EPS) {
								gBuffer.drawLine(xx,yy, getX(qq,w), getY(qq,w));
							}
						}
					}
					gBuffer.setColor(	transparent ? new Color(0,0,0,128) : Color.BLACK);
					gBuffer.drawString(String.valueOf(oi),xx-2,yy+3);
				}
			}

			if(moved) { //lasr sqare wireframe
				gBuffer.setColor(Color.LIGHT_GRAY);
				double[] edge0 = squareCoordsSorted[7];
				int ei = (int) edge0[3]; //original index
				for (int j = 0; j < 3; j++) {
					double[] edge1 = squareCoords[SQUARE_NB[ei][j]];
					gBuffer.drawLine(
						(int) ((edge0[0]*zoomf+1)*w/2), (int) (w-(edge0[1]*zoomf+1)*w/2),
						(int) ((edge1[0]*zoomf+1)*w/2), (int) (w-(edge1[1]*zoomf+1)*w/2)
					);
				}
			}
			g.drawImage(buffer, 0, 0, this);
		}


		public void setBounds(int x, int y, int width, int height) {
			sphImages=null;
			super.setBounds(x, y, width, height);
		}

		public int compare(Object o1, Object o2) {
			double[] da1 = (double[]) o1;
			double[] da2 = (double[]) o2;
			int cmp = Double.compare(da1[2],da2[2]);
			if(cmp==0) {
				cmp = Double.compare(da1[1],da2[1]);
				if(cmp==0) {
					cmp = Double.compare(da1[0],da2[0]);
				}
			}
			return cmp;
		}

		public int getN() {
			return n;
		}

		public int getP() {
			return p;
		}

		public void setN(int n) {
			if(n<1) n=1;
			if(n>confs.length) n=confs.length; 
			this.n = n;
			setP(1);
			sphImages=null;
		}

		public void setP(int p) {
			if(p>0 && p<=n)	{
				this.p = p;
				setXYZ(getXYZ());
			}
		}

		public void reSet(int n, int p, int xyz, int s, int op, boolean tr, boolean tc) {
			if(n<1) n=1;
			if(n>confs.length) n=confs.length; 
			this.n = n;
			if(p<1) p=1;
			if(p>n) p=n;
			this.p = p;
			transparent=tr;
			touches=tc;
			this.s=0; this.op=0;
			setS(s);
			setOp(op);
			moved=true;
			setXYZ(Math.abs(xyz)%6);
		}

		public void setXYZ(int xyz) {
			si=xyz%3; sgn = xyz>2?-1:1;
			double[][] co = new double[n][4];
			coords=confs[n-1].coords;
			for(int q=0;q<n;q++) {
				co[q][3]=coords[q][3];//original index
				if(si==0) {
					co[q][0]=coords[q][0]*sgn;
					co[q][1]=coords[q][1]*sgn;
					co[q][2]=coords[q][2]*sgn;
				}	else if(si==1) {
					co[q][0]=coords[q][1]*sgn;
					co[q][1]=coords[q][2]*sgn;
					co[q][2]=coords[q][0]*sgn;

				}	else if(si==2) {
					co[q][0]=coords[q][2]*sgn;
					co[q][1]=coords[q][0]*sgn;
					co[q][2]=coords[q][1]*sgn;
				}
			}
			Arrays.sort(co, this);
			sphereVisible = new boolean[n];
			for(int q=0;q<p; q++) {
				sphereVisible[(int) co[q][3]-1]=true;
			}
			coords = co;
			angle_x=angle_y=angle_z=0;
			if(moved) {
				zoomf=1.0;
				moved=false;
				sphImages=null;
				refreshSquareCoords();
				mouse_x=mouse_y=-1;
			}
		}

		int getXYZ() {
			int xyz=si;
			if(sgn<0) xyz+=3;
			return xyz;
		}
		public int getS() {
			return s;
		}

		public void setS(int sym) {
			if(sym>confs[n-1].symlist.size()) sym=0; 
			this.s = sym;
			op=0;
			sphImages=null;
		}

		public int getOp() {
			return op;
		}

		public void setOp(int op1) {
			if(s<1||op<0) return; 
			op=op1;
			Symmetry sym = (Symmetry) confs[n-1].symlist.get(s-1);
			if(op>sym.oplist.size()) op=0;
		}


		public boolean isTransparent() {
			return transparent;
		}

		public void setTransparent(boolean transparent) {
			this.transparent = transparent;
			sphImages=null;
		}

		public boolean isTouches() {
			return touches;
		}

		public void setTouches(boolean showTouch) {
			this.touches = showTouch;
		}

		public int getAngle_x() {
			return angle_x;
		}

		public int getAngle_y() {
			return angle_y;
		}

		public int getAngle_z() {
			return angle_z;
		}

		public double getZoomf() {
			return zoomf;
		}

		public void setZoomf(double zoomf) {
			this.zoomf = zoomf;
			moved=true;
		}

		public void mouseClicked(MouseEvent e) {
		}
		public void mouseReleased(MouseEvent e) {
		}
		public void mousePressed(MouseEvent e) {
			mouse_x=e.getX();
			mouse_y=e.getY();
		}
		public void mouseEntered(MouseEvent e) {
		}
		public void mouseExited(MouseEvent e) {
		}
		public void mouseDragged(MouseEvent e) {
			int x=e.getX();
			int y=e.getY();
			if(mouse_x>-1) {
				int ax=(y-mouse_y);
				int ay=(x-mouse_x);
				rotate(ax, ay, 0);
			}
			mouse_x=x;
			mouse_y=y;
			moved=true;
			repaint();
		}
		public void mouseMoved(MouseEvent e) {
		}
		public void keyTyped(KeyEvent e) {
		}
		public void keyPressed(KeyEvent e) {
			//System.out.println(e.paramString());
			int code = e.getKeyCode();
			char ch=Character.toLowerCase(e.getKeyChar());
			int mods= e.getModifiers();
			//System.out.println(KeyEvent.getKeyModifiersText(mods));
			int f = (mods & KeyEvent.CTRL_MASK)!=0 ? 5 : 1;
			int s = (mods & KeyEvent.SHIFT_MASK)!=0 ? -1 : 1;
			/*System.out.println("id="+" 0x"+Integer.toHexString(code)+" s=" + s+
				" f=" + f+
				" mods="+Integer.toBinaryString(mods)+" 0x"+Integer.toHexString(mods)+
				" step="+(Math.PI/32*f*s));*/
			if(ch=='-' || code==KeyEvent.VK_MINUS) {
				zoomf /= 1.0 + 0.125/f;
				sphImages=null;
				moved=true;
			} else if(ch=='+' || code==KeyEvent.VK_PLUS) {
				zoomf *= 1.0 + 0.125/f;
				sphImages=null;
				moved=true;
			} else if(ch=='x' || code==KeyEvent.VK_X) {
				rotate(5*s/f,0,0);
			} else if(ch=='y'|| code==KeyEvent.VK_Y) {
				rotate(0,5*s/f,0);
			} else if(ch=='z'|| code==KeyEvent.VK_Z) {
				rotate(0,0,5*s/f);
			} else if(ch=='2' || code==KeyEvent.VK_2 || code==KeyEvent.VK_DOWN) {
				rotate(5*s/f,0,0);
			} else if(ch=='8' || code==KeyEvent.VK_8 || code==KeyEvent.VK_UP) {
				rotate(-5*s/f,0,0);
			} else if(ch=='6'|| code==KeyEvent.VK_6 || code==KeyEvent.VK_RIGHT) {
				rotate(0,5*s/f,0);
			} else if(ch=='4'|| code==KeyEvent.VK_4|| code==KeyEvent.VK_LEFT) {
				rotate(0,-5*s/f,0);
			} else if(ch=='5'|| code==KeyEvent.VK_CLEAR) {
				rotate(0,0,5*s/f);
			} else if(ch=='p'|| code==KeyEvent.VK_P) {
				final Frame fr = new Frame();
				fr.addWindowListener(new WindowAdapter(){
					public void windowClosing(WindowEvent e) {
						fr.dispose();
					}
				});
				if((mods & KeyEvent.SHIFT_MASK)==0) {
					fr.setTitle("URI Parameters");
					TextField tf = new TextField(getParameters());
					tf.selectAll();				
					fr.add(tf);
				} else {
					fr.setTitle("Applet Parameters");
					TextArea ta = new TextArea(getAppletParameters());
					ta.selectAll();
					fr.add(ta);
				}
				fr.pack();
				fr.show();
			}
			repaint();			
		}
		public void keyReleased(KeyEvent e) {
		}

		public boolean isMoved() {
			return moved;
		}

	}

	public static double EPS = 0.001;

	private static double[][] SQUARE_COORDS = {
		{	-1, -1, -1, 0 },
		{	-1, -1,  1, 1 },
		{	-1,  1, -1, 2 },
		{	-1,  1,  1, 3 },
		{	 1, -1, -1, 4 },
		{	 1, -1,  1, 5 },
		{	 1,  1, -1, 6 },
		{	 1,  1,  1, 7 }
	};

	private static int[][] SQUARE_NB = {
		{	1, 2, 4 },
		{	0, 3, 5 },
		{	0, 3, 6 },
		{	1, 2, 7 },
		{	0, 5, 6 },
		{	1, 4, 7 },
		{ 2, 4, 7 },
		{	3, 5, 6 }
	};


	public static List split(String source, String separator) {
		LinkedList resultList = new LinkedList();
		int pos = 0;
		int pos1 = 0;
		int len = separator.length();
		do {
			pos1 = source.indexOf(separator, pos);
			if (pos1 != -1) {
				resultList.add(source.substring(pos, pos1));
				pos = pos1 + len;
			}
		} while (pos1 != -1);
		resultList.add(source.substring(pos));
		return resultList;
	}


	private PackingConf[] confs = null;
	private TextField nField;
	private PackingView view;
	private Choice xyzC;
	private Button trB;
	private Choice symC;
	private Button symB;
	private Button plusB;
	private Button minusB;
	private Button allB;
	private Checkbox touchesCB;
	private Label infoL;

	public void init() {
		try {
			readData();
			readSymData();
		}  catch (IOException ioe) {
			ioe.printStackTrace();
		}	

		setLayout(new BorderLayout());

		view = createView();
		infoL = new Label();
		infoL.setBackground(Color.LIGHT_GRAY);
		updateInfoLabel();

		view.setSize(getWidth(),getWidth());

		Panel cP = new Panel(new FlowLayout());
		cP.setBackground(Color.LIGHT_GRAY);
		nField = new TextField(String.valueOf(view.getN()));
		cP.add(nField);
		nField.addKeyListener(new KeyListener(){
			public void keyTyped(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode()==KeyEvent.VK_ENTER) {
					apply();
				}
			}
			public void keyReleased(KeyEvent e) {
			}
		});
		Button okB = new Button("apply");
		okB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				apply();
			}
		});
		cP.add(okB);

		allB = new Button("all");
		allB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setP(view.getN());
			}
		});
		allB.setEnabled(false);
		cP.add(allB);


		plusB = new Button("(+)");
		plusB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setP(view.getP()+1);
			}
		});
		plusB.setEnabled(false);
		cP.add(plusB);

		minusB = new Button("(-)");
		minusB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setP(view.getP()-1);
			}
		});
		minusB.setEnabled(false);
		cP.add(minusB);

		xyzC = new Choice();
		xyzC.add("+z");
		xyzC.add("+y");
		xyzC.add("+x");
		xyzC.add("-z");
		xyzC.add("-y");
		xyzC.add("-x");
		xyzC.select(0);
		xyzC.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				int xyz = xyzC.getSelectedIndex();
				view.setXYZ(xyz);
				view.repaint();
			}
		});
		cP.add(xyzC);

		trB = new Button("transp.");
		trB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.setTransparent(!view.isTransparent());
				updatePanel();
				view.repaint();
			}
		});
		cP.add(trB);

		
		touchesCB = new Checkbox("touches");
		touchesCB.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				view.setTouches(e.getStateChange()==ItemEvent.SELECTED);
				updatePanel();
				view.repaint();
			}
		});
		cP.add(touchesCB);
		

		//Panel cP1 = new Panel(new FlowLayout());

		symC = new Choice();
		symC.add("(0 Symmetries)");
		symC.select(0);
		symC.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				view.setS(symC.getSelectedIndex());
				updatePanel();
				updateInfoLabel();
				view.repaint();
			}
		});
		symC.setEnabled(false);
		cP.add(symC);

		symB = new Button("0");
		symB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				view.setOp(view.getOp()+1);
				updatePanel();
				view.repaint();
			}
		});
		symB.setEnabled(false);
		cP.add(symB);

		add(infoL, BorderLayout.NORTH);
		add(view, BorderLayout.CENTER);
		add(cP,BorderLayout.SOUTH);
		updatePanel();
	}

	private void updateInfoLabel() {
		PackingConf conf = confs[view.getN()-1];
		String text = "p:"+view.getP()+
		" radius:"+conf.radius+
		" reqired volume:"+conf.requiredVolume+
		" density:"+conf.density;
		if(conf.symlist.size()>0 && view.getS()>0) {
			text += " eps:"+ ((Symmetry) conf.symlist.get(view.getS()-1)).eps;
		}

		infoL.setText(text);
	}

	private void apply() {
		int n=Integer.parseInt(nField.getText());
		view.setN(n);
		view.setS(0);
		updatePanel();
		updateInfoLabel();
		view.repaint();
	}

	private void updatePanel() {
		symC.removeAll();
		PackingConf conf = confs[view.getN()-1];
		int symc = conf.symlist.size();
		symC.add("("+symc+" symmetr" + (symc==1?"y":"ies") + ")");
		symC.setEnabled(symc>0);
		for (Iterator itr = conf.symlist.iterator(); itr.hasNext();) {
			Symmetry sym = (Symmetry) itr.next();
			symC.add(sym.description);
		}
		symC.select(view.getS());

		symB.setLabel(String.valueOf(view.getOp()));
		symB.setEnabled(symc>0);

		plusB.setEnabled(view.getP()<view.getN());
		minusB.setEnabled(view.getP()>1);
		allB.setEnabled(view.getP()<view.getN());

		nField.setText(String.valueOf(view.getN()));
		
		trB.setLabel(view.isTransparent()?"opaque":"transp.");
		touchesCB.setState(view.isTouches());
		xyzC.select(view.getXYZ());
	}

	private void setP(int p) {
		view.setP(p);
		updatePanel();
		updateInfoLabel();
		view.repaint();
	}

	public String getParameters() {
		StringBuffer buf = new StringBuffer("?");
		buf.append("n=").append(view.getN()).append("&");
		buf.append("p=").append(view.getP()).append("&");
		buf.append("xyz=").append(view.getXYZ()).append("&");
		buf.append("s=").append(view.getS()).append("&");
		buf.append("op=").append(view.getOp());
		if(view.isTransparent()) buf.append("&").append("tr=").append(view.isTransparent());
		if(view.isTouches()) buf.append("&").append("tc=").append(view.isTransparent());
		if(view.isMoved()) {
			buf.append("&").append("ax=").append(view.getAngle_x());
			buf.append("&").append("ay=").append(view.getAngle_y());
			buf.append("&").append("az=").append(view.getAngle_z());
			buf.append("&").append("zf=").append(view.getZoomf());
		}
		return buf.toString();
	}

	public String getAppletParameters() {
		StringBuffer buf = new StringBuffer();
		buf.append("<param name=\"n\" value=\"").append(view.getN()).append("\">\n");
		buf.append("<param name=\"p\" value=\"").append(view.getP()).append("\">\n");
		buf.append("<param name=\"xyz\" value=\"").append(view.getXYZ()).append("\">\n");
		buf.append("<param name=\"s\" value=\"").append(view.getS()).append("\">\n");
		buf.append("<param name=\"op\" value=\"").append(view.getOp()).append("\">\n");
		if(view.isTransparent()) buf.append("<param name=\"tr\" value=\"").append(view.isTransparent()).append("\">\n");
		if(view.isTouches()) buf.append("<param name=\"tc\" value=\"").append(view.isTransparent()).append("\">\n");
		if(view.isMoved()) {
			buf.append("<param name=\"ax\" value=\"").append(view.getAngle_x()).append("\">\n");
			buf.append("<param name=\"ay\" value=\"").append(view.getAngle_y()).append("\">\n");
			buf.append("<param name=\"az\" value=\"").append(view.getAngle_z()).append("\">\n");
			buf.append("<param name=\"zf\" value=\"").append(view.getZoomf()).append("\">\n");
		}
		return buf.toString();
	}

	public PackingView createView() {
		PackingView view = null;
		int n=1, p=1, xyz=0, s=0, op=0;
		boolean tr=false, tc=false;
		int ax=0,ay=0,az=0; double zf=1.0; boolean r=false;
		if(getParameter("n")!=null) { n=Integer.parseInt(getParameter("n"));	}
		if(getParameter("p")!=null) { p=Integer.parseInt(getParameter("p")); }
		if(getParameter("xyz")!=null) { xyz=Integer.parseInt(getParameter("n")); }
		if(getParameter("s")!=null) { s=Integer.parseInt(getParameter("s")); }
		if(getParameter("op")!=null) { op=Integer.parseInt(getParameter("op")); }
		if(getParameter("tr")!=null) { tr=new Boolean(getParameter("tr")).booleanValue(); }
		if(getParameter("tc")!=null) { tc=new Boolean(getParameter("tc")).booleanValue(); }
		if(getParameter("zf")!=null) { zf=Double.parseDouble(getParameter("zf")); r=true; }
		if(getParameter("ax")!=null) { ax=Integer.parseInt(getParameter("ax")); r=true; }
		if(getParameter("ay")!=null) { ay=Integer.parseInt(getParameter("ay")); r=true; }
		if(getParameter("az")!=null) { az=Integer.parseInt(getParameter("az")); r=true; }
		view = new PackingView(n, p, xyz, s, op, tr, tc);
		if(r) {
			view.setZoomf(zf);
			view.rotate(ax,ay,az);
		}
		return view;
	}

	public void setParameters(String params) {
		if(params.length()==0 || !(params.charAt(0)=='?')) return;
		params=params.substring(1);
		List entrys = split(params,"&");
		int n=1, p=1, xyz=0, s=0, op=0;
		boolean tr=false, tc=false;
		int ax=0,ay=0,az=0; double zf=1.0; boolean r=false;
		for (Iterator itr = entrys.iterator(); itr.hasNext();) {
			String e = (String) itr.next();
			List kv = split(e,"=");
			String key = (String) kv.get(0);
			if(kv.size()>1) {
				String value = (String) kv.get(1);
				if("n".equals(key)) n=Integer.parseInt(value);
				else if("p".equals(key)) p=Integer.parseInt(value);
				else if("xyz".equals(key)) xyz=Integer.parseInt(value);
				else if("s".equals(key)) s=Integer.parseInt(value);
				else if("op".equals(key)) op=Integer.parseInt(value);
				else if("tr".equals(key)) tr=new Boolean(value).booleanValue();
				else if("tc".equals(key)) tc=new Boolean(value).booleanValue();
				else if("zf".equals(key)) { zf=Double.parseDouble(value); r=true; }
				else if("ax".equals(key)) { ax=Integer.parseInt(value); r=true; }
				else if("ay".equals(key)) { ay=Integer.parseInt(value); r=true; }
				else if("az".equals(key)) { az=Integer.parseInt(value); r=true; }
			}
		}
		view.reSet(n, p, xyz, s, op, tr, tc);
		
		if(r) {
			view.setZoomf(zf);
			view.rotate(ax,ay,az);
		}
		updatePanel();
		updateInfoLabel();
		view.repaint();
	}

	private void readData() throws IOException {
		String seqFile = getParameter("sequences");
		InputStream inputStream = seqFile==null
			? this.getClass().getResourceAsStream("sequences.txt")
			: new URL(getCodeBase(),seqFile).openStream();
		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		String line=null;
		String t=null; //single token
		//read maxN
		int ln=1;
		while((line=in.readLine())!=null) {
			line = line.trim();
			if(line.equals("--")) break;
			if(line.length()>0) {
				confs = new PackingConf[Integer.parseInt(line)]; 
			}
			ln++;
		}
		//read n, radius, requiredVolume, density
		while((line=in.readLine())!=null) {
			line = line.trim();
			if(line.equals("--")) break;
			if(line.length()>0) try {
				StringTokenizer tok = new StringTokenizer(line);
				if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
				int n=Integer.parseInt(t);
				confs[n-1] = new PackingConf(n);
				if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
				confs[n-1].radius = Double.parseDouble(t);
				if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
				confs[n-1].requiredVolume = Double.parseDouble(t);
				if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
				double Z=confs[n-1].density=Double.parseDouble(t);
			} catch(Exception e) {
				System.err.println("Error at line "+ln+" "+e);
			}
			ln++;
		}
		int n=0;
		while((line=in.readLine())!=null) {
			line = line.trim();
			if(line.equals("--")) break;
			if(line.length()>0) try {
				StringTokenizer tok = new StringTokenizer(line);
				if(n==0) { //first line not read
					if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
					n=Integer.parseInt(t);
				} else {
					if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
					int p=Integer.parseInt(t);
					for(int i=0; i<3;i++) { //read coordinate
						if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
						confs[n-1].coords[p-1][i]=Double.parseDouble(t);
					}
					if(p==n) {
						//Arrays.sort(confs[n-1].coords, view); //sort for default view
						for(int i=0; i<confs[n-1].coords.length; i++) {
							confs[n-1].coords[i][3]=i+1; //store original index
						}
						n=0; //ready for that n
					}
				}
			} catch(Exception e) {
				System.err.println("Error at line "+ln+" "+e);
				e.printStackTrace();
				break;
			}
			ln++;
		}

		in.close();
	}

	private void readSymData() throws IOException {
		String seqFile = getParameter("sphincubsyms");
		InputStream inputStream = seqFile==null
			? this.getClass().getResourceAsStream("sphincubsyms.txt")
			: new URL(getCodeBase(),seqFile).openStream();
		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		String line=null;
		String t=null; //single token
		HashMap symMap = new HashMap();
		int ln=1;
		while((line=in.readLine())!=null) {
			Symmetry sym=null;
			if(line.startsWith(" ")) {
				StringTokenizer tok = new StringTokenizer(line);
				if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
				double eps=Double.parseDouble(t);
				if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
				if((t=tok.nextToken())==null) throw new IOException("Syntax Error at line "+ln);
				int n = Integer.parseInt(t);
				sym = new Symmetry(eps);
				List ops = new ArrayList();
				while((line=in.readLine())!=null) {
					line=line.trim();
					if(line.length()>0 && line.charAt(0)=='(' && line.charAt(line.length()-1)==')') {
						line=line.substring(1,line.length()-1);
						List op = split(line,")(");
						if(op.size()<n) {
							int col=1;
							int[] oa=new int[n+1];
							for (Iterator itr = op.iterator(); itr.hasNext();) {
								String opls = (String) itr.next();
								StringTokenizer tok1 = new StringTokenizer(opls);
								List opList = new LinkedList();
								while(tok1.hasMoreTokens()) {
									opList.add(new Integer(tok1.nextToken()));
								}
								boolean colchange=false;
								for (Iterator itr1 = opList.iterator();	itr1.hasNext();) {
									Integer I = (Integer) itr1.next();
									if(opList.size()==1) {
										oa[I.intValue()-1] = 0;
									} else {
										oa[I.intValue()-1] = col;
										colchange=true;
									}
								}
								if(colchange) {
									col++;
									//if(col%6==0) col++;
								}
							}
							oa[n]=col--;//store maxcolor in n
							sym.oplist.add(oa);
						}
					} else {
						if(sym!=null) {
							if(line.length()>0) sym.description=line;
							confs[n-1].symlist.add(sym);
						}
						break;
					}
					ln++;
				}
			}
			ln++;
		}
	}

}
