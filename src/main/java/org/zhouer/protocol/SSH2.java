package org.zhouer.protocol;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import com.trilead.ssh2.InteractiveCallback;

class SSH2InputStream extends InputStream
{
	private SSH2 ssh2;
	
	public int read() throws IOException {
		return ssh2.readByte();
	}
	
	public int read( byte[] buf ) throws IOException
	{
		return ssh2.readBytes( buf );
	}

	public int read( byte[] buf, int offset, int length ) throws IOException
	{
		return ssh2.readBytes( buf, offset, length );
	}
	
	public SSH2InputStream( SSH2 s )
	{
		ssh2 = s;
	}
}

class SSH2OutputStream extends OutputStream
{
	private SSH2 ssh2;
	
	public void write( int b ) throws IOException
	{
		ssh2.writeByte( (byte)b );
	}

	public void write( byte[] b ) throws IOException
	{
		ssh2.writeBytes( b );
	}
	
	public void write( byte[] b, int offset, int length ) throws IOException
	{
		ssh2.writeBytes( b, offset, length );
	}
	
	public SSH2OutputStream( SSH2 s )
	{
		ssh2 = s;
	}
}

class PasswordDialog extends JDialog implements ActionListener
{
	private static final long serialVersionUID = 475389458121763833L;

	JLabel passLabel;
	JPasswordField passField;
	
	public void actionPerformed(ActionEvent e)
	{
		if( e.getSource() == passField ) {
			dispose();
		}
	}
	
	public String getPassword()
	{
		return new String( passField.getPassword() );
	}
	
	public PasswordDialog( Frame owner, String title, String prompt, boolean modal )
	{
		super( owner, title, modal );
		
		passLabel = new JLabel( prompt );
		passField = new JPasswordField( 10 );
		passField.addActionListener( this );
		
		getContentPane().setLayout( new FlowLayout() );
		getContentPane().add( passLabel );
		getContentPane().add( passField );
		pack();
		setLocationRelativeTo( null );
		setVisible( true );
	}
}

class Auth implements InteractiveCallback {
	
	public static String getUsername()
	{
		return JOptionPane.showInputDialog( null, "Nutzername：", "Bitte eingeben!", JOptionPane.QUESTION_MESSAGE);
	}

	public static String getPasword( String title, String prompt )
	{
		JPasswordField passwordField = new JPasswordField(10);
		JOptionPane.showConfirmDialog( null, passwordField, "Bitte eingeben!", JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE);
		return new String(passwordField.getPassword());
	}
	
	public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws Exception {
		
		String[] ret = new String[numPrompts];
		
		// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("name: " + name );
		// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("instruction: " + instruction );
		
		for( int i = 0; i < numPrompts; i++) {
			if( echo[i] ) {
				ret[i] = JOptionPane.showInputDialog( null, prompt[i], "Challenge", JOptionPane.QUESTION_MESSAGE);
			} else {
				ret[i] = getPasword( "Challenge", prompt[i] );
			}
		}
		
		return ret;
	}
}

public class SSH2 implements Protocol
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(SSH2.class);
	private String host;
	private int port;
	private String username;
	private String terminalType;
	
	private InputStream is;
	private OutputStream os;
	
	private boolean authenticated;
	private boolean connected, closed;
	
	private com.trilead.ssh2.Connection conn;
	private com.trilead.ssh2.Session sess;

	
	public boolean connect()
	{
		try {
			// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("SSH2 to host: " + host + ", port: " + port + " ..." );
			conn = new com.trilead.ssh2.Connection( host, port );
			// FIXME: magic number, 應可自行設定 timeout
			conn.connect( null, 5000, 60000 );
			conn.setTCPNoDelay( true );
			connected = true;
			
			de.elbosso.ui.dialog.SSHAuthDialog dlg=de.elbosso.ui.dialog.SSHAuthDialog.create(null);
			
/*			// 若 SSH2 constructor 未指定 username 則用 Auth.getUsername() 取得
			if( username == null || username.length() == 0 )
				username = Auth.getUsername();
			
			if( username == null ) {
				disconnect();
				return false;
			}
			
			String[] methods = conn.getRemainingAuthMethods( username );
			for(int i=0;i<methods.length;++i)
				if(CLASS_LOGGER.isEnabledFor(org.apache.log4j.Level.ERROR))CLASS_LOGGER.error(methods[i]);
			if( methods.length == 0 ) {
//				authenticated = conn.authenticateWithNone( username );
				 if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace( "SSH2: no authentication is needed" );
			} else {
				// for(int i = 0; i < methods.length; i++) {
				// 	if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace( methods[i] );
				// }
 if( conn.isAuthMethodAvailable( username, "publickey" ) ) {
					authenticated=conn.authenticateWithPublicKey(username, new java.io.File("/home/elbosso/.ssh/id_rsa"), "");
				} else {
					if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("unknown SSH2 authentication method.");
				}
			}
			
			if( authenticated == false ) {
				 if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("authentication failed.");
				disconnect();
				return false;
			} else {
				// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("authentication success.");
			}
			
			sess = conn.openSession();
*/			sess=dlg.showDialog(conn);
			if(sess==null)
			{
				authenticated=false;
				disconnect();
				return false;				
			}
			sess.requestPTY( terminalType, 80, 24, 0, 0, null );
			sess.startShell();
			is = sess.getStdout();
			os = sess.getStdin();

			return true;
			
		} catch (IOException e) {
			 e.printStackTrace();
			if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("Caught IOException in SSH2::connect()");
			connected = false;
			disconnect();
			return false;
		}
	}
	
	public void disconnect()
	{
		// 身份認證成功則需要 close
		if( authenticated ) {
			try {
				is.close();
				os.close();
				sess.close();
			} catch (IOException e) {
				// e.printStackTrace();
				if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace( "Caught IOException in SSH2::disconnect()" );
			}
		}
		
		// 不論是否連線成功都要 close
		conn.close();
		
		closed = true;
	}

	public boolean isConnected()
	{
		return connected;
	}
	
	public boolean isClosed()
	{
		return closed;
	}
	
	public int readByte() throws IOException
	{
		int r;

		r = is.read();
		if( r == -1 ) {
			// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("read -1 (EOF), disconnect().");
			throw new IOException();
		}
		return r;
	}

	public int readBytes(byte[] b) throws IOException
	{
		int r;

		r =  is.read( b );
		
		if( r == -1 ) {
			// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("read -1 (EOF), disconnect().");
			throw new IOException();
		}
		
		return r;
	}

	public int readBytes( byte[] b, int offset, int length ) throws IOException
	{
		int r;
		
		r = is.read( b, offset, length );
		
		if( r == -1 ) {
			// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("read -1 (EOF), disconnect().");
			throw new IOException();
		}
		
		return r;
	}
	
	public void writeByte(byte b) throws IOException
	{
		os.write( b );
		os.flush();
	}

	public void writeBytes( byte[] buf ) throws IOException
	{
		os.write( buf );
		os.flush();
	}
	
	public void writeBytes(byte[] buf, int offset, int size) throws IOException
	{
		os.write( buf, offset, size );
		os.flush();
	}
	
	public InputStream getInputStream()
	{
		return new SSH2InputStream( this );
	}
	
	public OutputStream getOutputStream()
	{
		return new SSH2OutputStream( this );
	}
	
	public void setTerminalType( String tt )
	{
		// TODO: 現在還不能動態改變，只有連線前就設定好才有用。
		terminalType = tt;
	}
	
	public String getTerminalType()
	{
		return terminalType;
	}
	
	public SSH2( String h, int p, String n )
	{
		host = h;
		port = p;
		username = n;
		
		connected = false;
		authenticated = false;
		closed = false;
	}
}
