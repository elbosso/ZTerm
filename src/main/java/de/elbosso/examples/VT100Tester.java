/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.elbosso.examples;

import org.zhouer.protocol.Protocol;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;

/**
 *
 * @author elbosso
 */
public class VT100Tester extends javax.swing.JPanel implements org.zhouer.vt.Application
,java.beans.PropertyChangeListener
,java.awt.event.ActionListener
{
//	private final static java.util.ResourceBundle i18n=java.util.ResourceBundle.getBundle("de.netsysit.dataflowframework.i18n",java.util.Locale.getDefault());
	private javax.swing.JFrame frame;
	private org.zhouer.zterm.Session vt100;
	private org.zhouer.zterm.Resource resource;
	private javax.swing.JComboBox fontcb;
	private javax.swing.Action connectAction;
	private javax.swing.Action disConnectAction;
	private javax.swing.Action testAction;

	public void showMessage(String msg)
	{
		javax.swing.JOptionPane.showMessageDialog(this, msg);
	}

	public void showPopup(int x, int y)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void openExternalBrowser(String url)
	{
		de.netsysit.util.lowlevel.BareBonesBrowserLaunch.openURL(url);
	}

	public boolean isConnected()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isClosed()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public boolean isTabForeground()
	{
		return true;
	}

	public Dimension getSize()
	{
		return frame.getSize();
	}

	public void bell()
	{
		Toolkit.getDefaultToolkit().beep();
	}

	public void paste()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void colorPaste()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void copy()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void colorCopy()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setIconName(String name)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setWindowTitle(String title)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void scroll(int lines)
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int readBytes(byte[] buf)
	{
		return vt100.readBytes(buf);
	}

	public void writeByte(byte b)
	{
		vt100.writeByte(b);
	}

	public void writeBytes(byte[] buf, int offset, int len)
	{
		vt100.writeBytes(buf, offset, len);
	}

	public void writeChar(char c)
	{
		vt100.writeChar(c);
	}

	public void writeChars(char[] buf, int offset, int len)
	{
		vt100.writeChars(buf, offset, len);
	}

	public VT100Tester()
	{
		super(new BorderLayout());
		frame=new javax.swing.JFrame();
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		resource=new org.zhouer.zterm.Resource();
		Font f=new Font("Monospaced", Font.PLAIN,17);
		resource.setValue(resource.FONT_FAMILY, f.getFamily());
		resource.setValue(resource.TERMINAL_ROWS, 600/(Toolkit.getDefaultToolkit().getFontMetrics(f).getAscent()+Toolkit.getDefaultToolkit().getFontMetrics(f).getDescent()));
//		Toolkit.getDefaultToolkit().getFontMetrics(f).charWidth('M');
//		Graphics2D gfx =
//		gfx.setFont(f); // select your preferred font, monospaced or otherwise
//		FontMetrics metrics = gfx.getFontMetrics();
		resource.setValue(resource.TERMINAL_COLUMNS, 817/Toolkit.getDefaultToolkit().getFontMetrics(f).charWidth('M'));

		vt100 =new org.zhouer.zterm.Session(null, resource, new org.zhouer.utils.Convertor(), null, this);
		vt100.addCommandListener(this);
		System.out.println(vt100.getPreferredSize());
		System.out.println(vt100.getSize());
		System.out.println(resource.getIntValue(resource.TERMINAL_COLUMNS));
		System.out.println(f.getSize()+" "+Toolkit.getDefaultToolkit().getFontMetrics(f).getAscent()+" "+Toolkit.getDefaultToolkit().getFontMetrics(f).getDescent());
//		vt100.setInteractiveCallback(new DefaultAuthenticator());
		frame.setContentPane(this);
		add(vt100);
		javax.swing.JToolBar toolbar=new javax.swing.JToolBar();
		createActions();
		toolbar.setFloatable(false);
		add(toolbar, BorderLayout.NORTH);
		toolbar.add(connectAction);
		toolbar.add(disConnectAction);
		toolbar.add(testAction);
		fontcb=new javax.swing.JComboBox(new de.elbosso.model.combobox.Fonts(true));
		fontcb.setSelectedItem(resource.getStringValue(resource.FONT_FAMILY));
		toolbar.add(fontcb);
		fontcb.addActionListener(this);
		fontcb.setRenderer(new de.elbosso.ui.renderer.list.ComboBoxFontRenderer());
		vt100.setEnabled(false);
		vt100.addPropertyChangeListener(this);
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args)
	{
		new VT100Tester();
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if(evt.getPropertyName().equals("command"))
		{
			
		}
		else if(evt.getPropertyName().equals("state"))
		{
			disConnectAction.setEnabled(vt100.getState()==vt100.STATE_CONNECTED);
			connectAction.setEnabled(!disConnectAction.isEnabled());
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource()==fontcb)
		{
			if(fontcb.getSelectedItem()!=null)
			{
				Font f=new Font(fontcb.getSelectedItem().toString(), Font.PLAIN,12);
				resource.setValue(resource.FONT_FAMILY, fontcb.getSelectedItem().toString());
				resource.setValue(resource.TERMINAL_ROWS, vt100.getPreferredSize().height/f.getSize());
				resource.setValue(resource.TERMINAL_COLUMNS, vt100.getPreferredSize().width/Toolkit.getDefaultToolkit().getFontMetrics(f).charWidth('M'));
				vt100.updateSize();
				getParent().invalidate();
				getParent().validate();
				getParent().doLayout();
				getParent().repaint();
			}
		}
	}

	private void createActions()
	{
		connectAction=new javax.swing.AbstractAction("disconnect",de.netsysit.util.ResourceLoader.getIcon("de/netsysit/ressources/gfx/ca/verbindung_herstellen_48.png")) {

			public void actionPerformed(ActionEvent e)
			{
				org.zhouer.zterm.Site site=new org.zhouer.zterm.Site("huhu", "pirxhome.fritz.box", 22, Protocol.STDINOUT);
				site.encoding=java.nio.charset.StandardCharsets.UTF_8.name();
				vt100.connect(site);
//				disConnectAction.setEnabled(true);
//				setEnabled(false);				
			}
		};
		disConnectAction=new javax.swing.AbstractAction("connect",de.netsysit.util.ResourceLoader.getIcon("de/netsysit/ressources/gfx/ca/verbindung_trennen_48.png")) {

			public void actionPerformed(ActionEvent e)
			{
				vt100.disConnect();
//				connectAction.setEnabled(true);
//				setEnabled(false);				
			}
		};
		disConnectAction.setEnabled(false);
		testAction=new javax.swing.AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					System.out.println("ÄÖÜ");
					System.out.print("äöü");
					System.out.println(this);
					System.out.append('ß');
					System.out.append("Ä");
					System.out.append("123456789", 3, 6);
					System.out.println();
					System.out.print(1234);
					System.out.println();
					System.out.println(1234);
					System.out.println((Object) null);
					System.out.format("Duke's Birthday: %1$tm %1$te,%1$tY%n", java.util.Calendar.getInstance());
					System.out.println();
					System.out.println("\u001B[36m" + "Menu option" + "\u001B[0m");
//					System.out.println(new BufferedReader(new InputStreamReader(System.in)).readLine());
/*					new java.lang.Thread()
					{
						@Override
						public void run()
						{
							InputStreamReader isr=new InputStreamReader(System.in);
							while(true)
							{
								try
								{
									System.out.println("read: "+isr.read());
								} catch (IOException ioException)
								{
									ioException.printStackTrace();
									break;
								}

							}
						}
					}.start();
*/

				}
				catch(Throwable t)
				{
					t.printStackTrace();
				}
			}
		};
		testAction.putValue(javax.swing.Action.SMALL_ICON,de.netsysit.util.ResourceLoader.getIcon("de/netsysit/ressources/gfx/ca/Konfigurieren_48.png"));
	}
	
	
}