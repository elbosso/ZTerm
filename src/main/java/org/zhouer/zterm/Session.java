package org.zhouer.zterm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.Timer;

import org.zhouer.protocol.*;
import org.zhouer.utils.Convertor;
import org.zhouer.utils.TextUtils;
import org.zhouer.vt.Application;
import org.zhouer.vt.Config;
import org.zhouer.vt.VT100;

public class Session extends JPanel implements Runnable, Application, AdjustmentListener, MouseWheelListener
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(Session.class);
	private static final long serialVersionUID = 2180544188833033537L;

	protected Application parent;
	protected Resource resource;
	private Convertor conv;
	private Site site;
	
	private String socks_host;
	private int socks_port;
	
	protected VT100 vt;
	private JScrollBar scrollbar;
	
	private String iconname;
	private String windowtitle;
	
	// 捲頁緩衝區的行數
	private int scrolllines;
	
	// 與遠端溝通用的物件
	protected Protocol protocol;
	private InputStream is;
	private OutputStream os;
	
	// 自動重連用
	protected long startTime;
	
	// 防閒置用
	private boolean antiidle;
	protected Timer ti;
	private long lastInputTime, antiIdleInterval;
	
	// 連線狀態
	public int state;
	
	// 連線狀態常數
	public static final int STATE_TRYING = 1;
	public static final int STATE_CONNECTED = 2;
	public static final int STATE_CLOSED = 3;
	public static final int STATE_ALERT = 4;
	
	// 這個 session 是否擁有一個 tab, 可能 session 還沒結束，但 tab 已被關閉。
	protected boolean hasTab;

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		if(vt!=null)
			vt.setEnabled(enabled);
	}
	
	/*
	 * 送往上層的
	 */
	
	public void setState( int s )
	{
		int old=getState();
		state = s;
		setEnabled(state==STATE_CONNECTED);
		firePropertyChange("state", old, getState());
//		parent.updateTabState( s, this );
	}

	public int getState()
	{
		return state;
	}
	
	public boolean isTabForeground()
	{
		return parent.isTabForeground(  );
	}
	
	public void bell()
	{
		parent.bell(  );
	}
	
	public void copy()
	{
		parent.copy();
	}
	
	public void colorCopy()
	{
		parent.colorCopy();
	}
	
	public void paste()
	{
		parent.paste();
	}
	
	public void colorPaste()
	{
		parent.colorPaste();
	}
	
	/*
	 * 送往下層的
	 */
	
	public void updateSize()
	{
		vt.updateSize();
	}
	
	public void updateScreen()
	{
		vt.updateScreen();
	}
	
	public void updateImage( BufferedImage bi )
	{
		vt.updateImage( bi );
	}
	
	public void resetSelected()
	{
		vt.resetSelected();
	}
	
	public String getSelectedText()
	{
		return vt.getSelectedText();
	}
	
	public String getSelectedColorText()
	{
		return vt.getSelectedColorText();
	}
	
	public void pasteText( String str )
	{
		vt.pasteText( str );
	}
	
	public void pasteColorText( String str )
	{
		vt.pasteColorText( str );
	}
	
	public boolean requestFocusInWindow()
	{
		return vt.requestFocusInWindow();
	}
	
	/*
	 * 送到 network 的 
	 */
	
	public int readBytes( byte[] buf )
	{
		try {
			return is.read( buf );
		} catch (IOException e) {
			// e.printStackTrace();
			// 可能是正常中斷，也可能是異常中斷，在下層沒有區分
			close( true );
			return -1;
		}
	}
	
	public void writeByte( byte b )
	{
		lastInputTime = new Date().getTime();
		try {
			os.write( b );
		} catch (IOException e) {
			// e.printStackTrace();
			if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace( "Caught IOException in Session::writeByte(...)" );
			close( true );
		}
	}
	
	public void writeBytes( byte[] buf, int offset, int len )
	{
		lastInputTime = new Date().getTime();
		try {
			os.write( buf, offset, len );
		} catch (IOException e) {
			// e.printStackTrace();
			if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace( "Caught IOException in Session::writeBytes(...)" );
			close( true );
		}
	}
	
	public void writeChar( char c )
	{
		byte[] buf;

		buf = conv.charToBytes( c, site.encoding );
		
		writeBytes( buf, 0, buf.length );
	}
	
	public void writeChars( char[] buf, int offset, int len )
	{
		int count = 0;
		// FIXME: magic number
		byte[] tmp = new byte[len * 4];
		byte[] tmp2;
		
		for(int i = 0; i < len; i++) {
			tmp2 = conv.charToBytes( buf[offset + i], site.encoding );
			for(int j = 0; j < tmp2.length; j++ ) {
				tmp[count++] = tmp2[j];
			}
		}
		
		writeBytes( tmp, 0, count );
	}
	
	public boolean isConnected()
	{
		// 如果 network 尚未建立則也當成尚未 connect.
		if( protocol == null ) {
			return false;
		}
		return protocol.isConnected();
	}
	
	public boolean isClosed()
	{
		// 如果 network 尚未建立則也當成 closed.
		if( protocol == null ) {
			return true;
		}
		return protocol.isClosed();
	}
	
	/*
	 * 自己的
	 */
	
	public Site getSite()
	{
		return site;
	}
	
	public void setEncoding( String enc )
	{
		site.encoding = enc;
		vt.setEncoding( site.encoding );
	}
	
	public void setEmulation( String emu )
	{
		site.emulation = emu;
		
		// 通知遠端 terminal type 已改變
		protocol.setTerminalType( emu );
		
		vt.setEmulation( emu );
	}
	
	public String getEmulation()
	{
		return site.emulation;
	}
	
	public String getURL()
	{
		return site.getURL();
	}
	
	public void setIconName( String in )
	{
		// TODO: 未完成
		iconname = in;
		//parent.updateTab();
	}
	
	public void setWindowTitle( String wt )
	{
		// TODO: 未完成
		windowtitle = wt;
		//parent.updateTab();
	}
	
	public String getIconName()
	{
		return iconname;
	}
	
	public String getWindowTitle()
	{
		return windowtitle;
	}
	
	public void close( boolean fromRemote )
	{
		if( isClosed() ) {
			return;
		}
		
		// 移除 listener
		removeMouseWheelListener( this );
		
		// 中斷連線
		protocol.disconnect();
		
		// 停止防閒置用的 timer
		if( ti != null ) {
			ti.stop();
		}
		
		// 通知 vt 停止運作
		if( vt != null ) {
			vt.close();
		}
		
		// 將連線狀態改為斷線
		setState( STATE_CLOSED );
		
		// 若遠端 server 主動斷線則判斷是否需要重連
		boolean autoreconnect = resource!=null?resource.getBooleanValue( Resource.AUTO_RECONNECT ):false;
		if( autoreconnect && fromRemote ) {	
			long reopenTime = resource!=null?resource.getIntValue( Resource.AUTO_RECONNECT_TIME ):5l;
			long reopenInterval = resource!=null?resource.getIntValue( Resource.AUTO_RECONNECT_INTERVAL ):1000l;
			long now = new Date().getTime();
			
			// 判斷連線時間距現在時間是否超過自動重連時間
			// 若設定自動重連時間為 0 則總是自動重連
			if( (now - startTime <= reopenTime * 1000) || reopenTime == 0 ) {
				try {
					Thread.sleep( reopenInterval );
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// 有可能在 sleep 時分頁被使用者手動關掉，只有當分頁仍存在時才重連
				if( hasTab )
					;//parent.reopenSession( this );	
			}
		}
	}
	
	public void remove()
	{
		// 設定分頁被移除了
		hasTab = false;
	}
	
	public void updateAntiIdleTime()
	{
		// 更新是否需要啟動防閒置
		antiidle = resource!=null?resource.getBooleanValue( Resource.ANTI_IDLE ):false;
		
		// 防閒置的作法是定時檢查距上次輸入是否超過 interval,
		// 所以這裡只要設定 antiIdleTime 就自動套用新的值了。
		antiIdleInterval = (resource!=null?resource.getIntValue( Resource.ANTI_IDLE_INTERVAL ):1) * 1000 ;
	}
	
	public void showMessage( String msg )
	{
		// 當分頁仍存在時才會顯示訊息
		if( hasTab ) {
			parent.showMessage( msg );
		}
	}
	
	public void showPopup( int x, int y )
	{
		Point p = vt.getLocationOnScreen();
		String link = vt.getURL(x, y);

		//parent.showPopup( p.x + x, p.y + y, link );
	}
	
	public void openExternalBrowser( String url )
	{
		parent.openExternalBrowser( url );
	}

	public void disConnect()
	{
		if(protocol !=null)
			protocol.disconnect();
		setState(STATE_CLOSED);
		setEnabled(false);
	}

	public void connect(Site site)
	{
		disConnect();
		this.site=site;
		if(this.site!=null)
		{
			windowtitle = this.site.host;
			iconname = this.site.host;
			vt.setEncoding( this.site.encoding );
			vt.setEmulation( this.site.emulation );
			vt.initBuf();
			new Thread( this ).start();
		}		
	}
	
	class AntiIdleTask implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{	
			// 如果超過 antiIdelInterval milliseconds 沒有送出東西，
			// lastInputTime 在 writeByte, writeBytes 會被更新。
			long now = new Date().getTime();
			if( antiidle && isConnected() && (now - lastInputTime > antiIdleInterval) ) {
				// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace( "Sent antiidle char" );
				// TODO: 設定 antiidle 送出的字元
				if( site.protocol.equalsIgnoreCase( Protocol.TELNET ) ) {
										
					String buf = TextUtils.BSStringToString( resource.getStringValue(Resource.ANTI_IDLE_STRING) );
					char[] ca = buf.toCharArray();
					writeChars(ca, 0, ca.length);

					// 較正規的防閒置方式
					// writeByte( Telnet.IAC );
					// writeByte( Telnet.NOP );
				}
			}
		}
	}
	
	public void adjustmentValueChanged( AdjustmentEvent ae )
	{
		vt.setScrollUp( scrollbar.getMaximum() - scrollbar.getValue() - scrollbar.getVisibleAmount() );
	}
	
	public void mouseWheelMoved( MouseWheelEvent arg0 )
	{
		scroll( arg0.getWheelRotation() );
	}
	
	public void scroll( int amount )
	{
		scrollbar.setValue( scrollbar.getValue() + amount );
	}

	public void run()
	{
		// 設定連線狀態為 trying
		setState( STATE_TRYING );
		
		// 新建連線
		if( site.protocol.equalsIgnoreCase( Protocol.TELNET ) )
		{
			if( (resource!=null?resource.getBooleanValue( Resource.USING_SOCKS ):false) && site.usesocks ) {
				socks_host = resource.getStringValue( Resource.SOCKS_HOST );
				socks_port = resource.getIntValue( Resource.SOCKS_PORT );
				protocol = new Telnet( site.host, site.port, socks_host, socks_port );
			} else {
				protocol = new Telnet( site.host, site.port );
			}
			protocol.setTerminalType( site.emulation );
		}
		else if( site.protocol.equalsIgnoreCase( Protocol.SSH ) )
		{
			protocol = new SSH2( site.host, site.port, site.username );
//			((SSH2)network).setInteractiveCallback(getInteractiveCallback());
			protocol.setTerminalType( site.emulation );
		}
		else if( site.protocol.equalsIgnoreCase( Protocol.STDINOUT ) )
		{
			protocol =new StdInOut();
			protocol.setTerminalType(site.emulation);
		}
		else if( site.protocol.equalsIgnoreCase( Protocol.PROCESSINOUT ) )
		{
			protocol=new ProcessInOut(resource.getArray(Config.CMD_LINE),resource.getArray(Config.ENV_MAP));
			protocol.setTerminalType(site.emulation);
		}
		else if( site.protocol.equalsIgnoreCase( Protocol.PTYINOUT ) )
		{
			if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("TYINOUT");
			protocol=new PtyInOut(resource.getArray(Config.CMD_LINE),resource.getArray(Config.ENV_MAP));
			protocol.setTerminalType(site.emulation);
		}
		else
		{
			if(CLASS_LOGGER.isEnabledFor(org.apache.log4j.Level.ERROR))CLASS_LOGGER.error( "Unknown protocol: " + site.protocol );
		}

		// 連線失敗
		if( protocol.connect() == false ) {
			//  設定連線狀態為 closed
			setState( STATE_CLOSED );
//			showMessage( "連線失敗！" );
			return;
		}
		
		is = protocol.getInputStream();
		os = protocol.getOutputStream();
		System.err.println(is);
		System.err.println(os);

		// TODO: 如果需要 input filter or trigger 可以在這邊套上
		
		//  設定連線狀態為 connected
		setState( STATE_CONNECTED );
		
		// 連線成功，更新或新增連線紀錄
		if(resource!=null)
			resource.addFavorite( site );
		//parent.updateFavoriteMenu();
		
		// 防閒置用的 Timer
		updateAntiIdleTime();
		lastInputTime = new Date().getTime();
		ti = new Timer( 1000, new AntiIdleTask() );
		ti.start();
		
		// 記錄連線開始的時間
		startTime = new Date().getTime();
		try
		{
			vt.run();
		}
		catch(java.lang.Throwable t)
		{
			t.printStackTrace();
		}
//		if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("terminating");
		setState(STATE_CLOSED);
	}
	
	public Session( Site s, Resource r, Convertor c, BufferedImage bi, Application pa )
	{
		super();
		
		site = s;
		resource = r;
		conv = c;
		parent = pa;
		
		// 設定擁有一個分頁
		hasTab = true;
		
		// FIXME: 預設成 host
		// FIXME: magic number
		setBackground( Color.BLACK );
		
		// VT100
		vt = new VT100( this, resource, conv, bi );
		
		// FIXME: 是否應該在這邊設定？
		connect(site);
		// 設定 layout 並把 vt 及 scrollbar 放進去，
		setLayout( new BorderLayout() );
		add( vt, BorderLayout.CENTER );
		
		// chitsaou.070726: 顯示捲軸
		if(( resource==null)||(resource.getBooleanValue( Resource.SHOW_SCROLL_BAR ) ))
		{
			scrolllines = resource!=null?resource.getIntValue( Config.TERMINAL_SCROLLS ):200;
			// FIXME: magic number
			scrollbar = new JScrollBar( JScrollBar.VERTICAL, scrolllines - 1, 24, 0, scrolllines + 23 );
			scrollbar.addAdjustmentListener( this );
			
			add( scrollbar, BorderLayout.EAST );
			
			// 處理滑鼠滾輪事件
			addMouseWheelListener( this );
		}
		
	}
	public void addCommandListener(PropertyChangeListener l)
	{
		vt.addCommandListener(l);
	}
	public void removeCommandListener(PropertyChangeListener l)
	{
		vt.removeCommandListener(l);
	}
}
