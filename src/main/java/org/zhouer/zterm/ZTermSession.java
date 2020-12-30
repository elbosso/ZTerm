/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.zhouer.zterm;

import java.util.Date;

/**
 *
 * @author elbosso
 */
public class ZTermSession extends Session
{
	public ZTermSession( Site s, Resource r, org.zhouer.utils.Convertor c, java.awt.image.BufferedImage bi, ZTerm pa )
	{
		super(s, r, c, bi, pa);
	}
	public void setState( int s )
	{
		super.setState(s);
		((ZTerm)parent).updateTabState( s, this );
	}
	
	public boolean isTabForeground()
	{
		return ((ZTerm)parent).isTabForeground( this );
	}
	
	public void bell()
	{
		((ZTerm)parent).bell( this );
	}

	public void setIconName( String in )
	{
		super.setIconName(in);
		((ZTerm)parent).updateTab();
	}
	
	public void setWindowTitle( String wt )
	{
		super.setWindowTitle(wt);
		((ZTerm)parent).updateTab();
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
		boolean autoreconnect = resource.getBooleanValue( Resource.AUTO_RECONNECT );
		if( autoreconnect && fromRemote ) {	
			long reopenTime = resource.getIntValue( Resource.AUTO_RECONNECT_TIME );
			long reopenInterval = resource.getIntValue( Resource.AUTO_RECONNECT_INTERVAL );
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
					((ZTerm)parent).reopenSession( this );	
			}
		}
	}
	
	public void showPopup( int x, int y )
	{
		java.awt.Point p = vt.getLocationOnScreen();
		String link = vt.getURL(x, y);

		((ZTerm)parent).showPopup( p.x + x, p.y + y, link );
	}
	
}
