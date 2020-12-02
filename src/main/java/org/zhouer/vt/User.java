package org.zhouer.vt;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeListener;

public class User extends java.lang.Object implements KeyListener, MouseListener, MouseMotionListener, de.netsysit.util.beans.PropertyChangeSender
{

	private Application parent;
	private VT100 vt;
	private Config config;
	private int pressX, pressY, dragX, dragY;
	private boolean isDefaultCursor;
	private java.lang.StringBuffer commandBuilder;
	private java.lang.String command;
	private int cursorPosition;
	private boolean overwrite;
	private java.beans.PropertyChangeSupport pcs;

	public void keyPressed(KeyEvent e)
	{
		if (vt.isEnabled())
		{
			int len;
			byte[] buf = new byte[4];

			/*
			 if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace( "key presses: " + e );
			 if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace( "key modifier: " + e.getModifiers() );
			 */

			// 其他功能鍵
			switch (e.getKeyCode())
			{
				case KeyEvent.VK_UP:
					if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD)
					{
						buf[0] = 0x1b;
						buf[1] = 0x5b;
						buf[2] = 'A';
						len = 3;
					}
					else
					{
						buf[0] = 0x1b;
						buf[1] = 0x4f;
						buf[2] = 'A';
						len = 3;
					}
					break;
				case KeyEvent.VK_DOWN:
					if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD)
					{
						buf[0] = 0x1b;
						buf[1] = 0x5b;
						buf[2] = 'B';
						len = 3;
					}
					else
					{
						buf[0] = 0x1b;
						buf[1] = 0x4f;
						buf[2] = 'B';
						len = 3;
					}
					break;
				case KeyEvent.VK_RIGHT:
					if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD)
					{
						buf[0] = 0x1b;
						buf[1] = 0x5b;
						buf[2] = 'C';
						len = 3;
					}
					else
					{
						buf[0] = 0x1b;
						buf[1] = 0x4f;
						buf[2] = 'C';
						len = 3;
					}
					if (cursorPosition < commandBuilder.length())
					{
						++cursorPosition;
					}
					break;
				case KeyEvent.VK_LEFT:
					if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD)
					{
						buf[0] = 0x1b;
						buf[1] = 0x5b;
						buf[2] = 'D';
						len = 3;
					}
					else
					{
						buf[0] = 0x1b;
						buf[1] = 0x4f;
						buf[2] = 'D';
						len = 3;
					}
					if (cursorPosition > 0)
					{
						--cursorPosition;
					}
					break;
				case KeyEvent.VK_INSERT:
					buf[0] = 0x1b;
					buf[1] = 0x5b;
					buf[2] = '2';
					buf[3] = '~';
					len = 4;
					overwrite = !overwrite;
					break;
				case KeyEvent.VK_HOME:
					if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD)
					{
						buf[0] = 0x1b;
						buf[1] = 0x5b;
						buf[2] = '1';
						buf[3] = '~';
						len = 4;
					}
					else
					{
						buf[0] = 0x1b;
						buf[1] = 0x4f;
						buf[2] = 'H';
						len = 3;
					}
					cursorPosition = 0;
					break;
				case KeyEvent.VK_PAGE_UP:
					buf[0] = 0x1b;
					buf[1] = 0x5b;
					buf[2] = '5';
					buf[3] = '~';
					len = 4;
					break;
				case KeyEvent.VK_DELETE:
					buf[0] = 0x1b;
					buf[1] = 0x5b;
					buf[2] = '3';
					buf[3] = '~';
					len = 4;
					break;
				case KeyEvent.VK_END:
					if (vt.getKeypadMode() == VT100.NUMERIC_KEYPAD)
					{
						buf[0] = 0x1b;
						buf[1] = 0x5b;
						buf[2] = '4';
						buf[3] = '~';
						len = 4;
					}
					else
					{
						buf[0] = 0x1b;
						buf[1] = 0x4f;
						buf[2] = 'F';
						len = 3;
					}
					cursorPosition = commandBuilder.length();
					break;
				case KeyEvent.VK_PAGE_DOWN:
					buf[0] = 0x1b;
					buf[1] = 0x5b;
					buf[2] = '6';
					buf[3] = '~';
					len = 4;
					break;
				default:
					len = 0;
			}

			if (len != 0)
			{
				parent.writeBytes(buf, 0, len);
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				// XXX: 在 Mac 上 keyTyped 似乎收不到 esc
				parent.writeByte((byte) 0x1b);
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_ENTER)
			{
				// XXX: ptt 只吃 0x0d, 只送 0x0a 沒用
				parent.writeByte((byte) 0x0d);
				return;
			}

		}
	}

	public void keyReleased(KeyEvent e)
	{
		if (vt.isEnabled())
		{
		}
	}

	public void keyTyped(KeyEvent e)
	{
		if (vt.isEnabled())
		{
			// System.out.println( "key typed: " + e );

			// 功能鍵，不理會
			if (e.isAltDown() || e.isMetaDown())
			{
				return;
			}
			// delete, enter, esc 會在 keyPressed 被處理 
			if (e.getKeyChar() == KeyEvent.VK_ENTER)
			{
//			System.out.println(commandBuilder);
				java.lang.String old = getCommand();
				command = commandBuilder.toString();
				pcs.firePropertyChange("command", old, getCommand());
				commandBuilder = new java.lang.StringBuffer();
				cursorPosition = 0;
				return;
			}

			if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
			{
				return;
			}
			if (e.getKeyChar() == KeyEvent.VK_DELETE)
			{
				commandBuilder.deleteCharAt(cursorPosition);
				return;
			}
			if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE)
			{
				if (cursorPosition > 0)
				{
					--cursorPosition;
					commandBuilder.deleteCharAt(cursorPosition);
				}
//			return;
			}
			else
			{
				if (overwrite)
				{
					commandBuilder.setCharAt(cursorPosition, e.getKeyChar());
				}
				else
				{
					commandBuilder.insert(cursorPosition, e.getKeyChar());
					++cursorPosition;
				}
			}

			// 一般按鍵，直接送出
			parent.writeChar(e.getKeyChar());
		}
	}

	public String getCommand()
	{
		return command;
	}

	public void mouseClicked(MouseEvent e)
	{
		if (vt.isEnabled())
		{
			// System.out.println( e );

			do
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					// 左鍵
					if (vt.coverURL(e.getX(), e.getY()))
					{
						// click
						// 開啟瀏覽器
						String url = vt.getURL(e.getX(), e.getY());

						if (url.length() != 0)
						{
							parent.openExternalBrowser(url);
						}
						break;
					}
					else
					{
						if (e.getClickCount() == 2)
						{
							// double click
							// 選取連續字元
							vt.selectConsequtive(e.getX(), e.getY());
							vt.repaint();
							break;
						}
						else
						{
							if (e.getClickCount() == 3)
							{
								// triple click
								// 選取整行
								vt.selectEntireLine(e.getX(), e.getY());
								vt.repaint();
								break;
							}
						}
					}
				}
				else
				{
					if (e.getButton() == MouseEvent.BUTTON2)
					{
						// 中鍵
						// 貼上
						if (e.isControlDown())
						{
							// 按下 ctrl 則彩色貼上
							parent.colorPaste();
						}
						else
						{
							parent.paste();
						}
						break;
					}
					else
					{
						if (e.getButton() == MouseEvent.BUTTON3)
						{
							// 右鍵
							// 跳出 popup menu
							parent.showPopup(e.getX(), e.getY());
							break;
						}
					}
				}

				vt.requestFocus();
				vt.resetSelected();
				vt.repaint();

			}
			while (false);
		}
	}

	public void mousePressed(MouseEvent e)
	{
		if (vt.isEnabled())
		{
			pressX = e.getX();
			pressY = e.getY();
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		if (vt.isEnabled())
		{
			boolean meta, ctrl;

			// 只處理左鍵
			if (e.getButton() != MouseEvent.BUTTON1)
			{
				return;
			}

			meta = e.isAltDown() || e.isMetaDown();
			ctrl = e.isControlDown();

			// select 時按住 meta 表反向，即：
			// 若有 copy on select 則按住 meta 代表不複製，若沒有 copy on select 則按住 meta 代表要複製。
			if (config.getBooleanValue(Config.COPY_ON_SELECT) == meta)
			{
				return;
			}

			// ctrl 代表複製時包含色彩。
			if (ctrl)
			{
				parent.colorCopy();
			}
			else
			{
				parent.copy();
			}
		}
	}

	public void mouseEntered(MouseEvent e)
	{
		if (vt.isEnabled())
		{
		}
	}

	public void mouseExited(MouseEvent e)
	{
		if (vt.isEnabled())
		{
		}
	}

	public void mouseMoved(MouseEvent e)
	{
		if (vt.isEnabled())
		{
			boolean cover = vt.coverURL(e.getX(), e.getY());

			// 只有滑鼠游標需改變時才 setCursor
			if (isDefaultCursor && cover)
			{
				vt.setCursor(new Cursor(Cursor.HAND_CURSOR));
				isDefaultCursor = false;
			}
			else
			{
				if (!isDefaultCursor && !cover)
				{
					vt.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					isDefaultCursor = true;
				}
			}
		}
	}

	public void mouseDragged(MouseEvent e)
	{
		if (vt.isEnabled())
		{
			dragX = e.getX();
			dragY = e.getY();

			vt.setSelected(pressX, pressY, dragX, dragY);
			vt.repaint();
		}
	}

	public User(Application p, VT100 v, Config c)
	{
		parent = p;
		vt = v;
		config = c;
		isDefaultCursor = true;
		commandBuilder = new java.lang.StringBuffer();
		pcs = new java.beans.PropertyChangeSupport(this);
	}

	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		pcs.addPropertyChangeListener(l);
	}

	public void addPropertyChangeListener(String name, PropertyChangeListener l)
	{
		pcs.addPropertyChangeListener(name, l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l)
	{
		pcs.removePropertyChangeListener(l);
	}

	public void removePropertyChangeListener(String name, PropertyChangeListener l)
	{
		pcs.removePropertyChangeListener(name, l);
	}
}
