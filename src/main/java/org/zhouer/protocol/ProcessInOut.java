package org.zhouer.protocol;

import java.io.*;

class ProcessInOutInputStream extends InputStream
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(ProcessInOutInputStream.class);
	private ProcessInOut processInOut;

	public int read() throws IOException
	{
		return processInOut.readByte();
	}

	public int read( byte[] buf ) throws IOException
	{
		return processInOut.readBytes( buf );
	}

	public int read( byte[]buf, int offset, int length ) throws IOException
	{
		return processInOut.readBytes( buf, offset, length );
	}

	public ProcessInOutInputStream( ProcessInOut tel )
	{
		processInOut = tel;
	}
}

class ProcessInOutOutputStream extends OutputStream
{
	private ProcessInOut processInOut;

	public void write( int b ) throws IOException
	{
		// java doc: The 24 high-order bits of b are ignored.
		processInOut.writeByte( (byte)b );
	}

	public void write( byte[] buf ) throws IOException
	{
		processInOut.writeBytes( buf );
	}

	public void write( byte[] buf, int offset, int length ) throws IOException
	{
		processInOut.writeBytes( buf, offset, length );
	}

	public ProcessInOutOutputStream( ProcessInOut tel )
	{
		processInOut = tel;
	}
}

public class ProcessInOut implements Protocol
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(ProcessInOut.class);
	private String host;
	private int port;
	private String username;
	private String terminalType;

	private InputStream is;
	private InputStream ise;
	private OutputStream os;

	private boolean connected;
	private com.pty4j.PtyProcess pty;

	public boolean connect()
	{
		boolean rv=false;
		try
		{
//			ProcessBuilder pb = new ProcessBuilder("ls","-l","--color=tty");
//			ProcessBuilder pb = new ProcessBuilder("top");
//			ProcessBuilder pb = new ProcessBuilder("/bin/sh","-e");
//			java.util.Map<String, String> env = pb.environment();
//			env.put("TERM", "xterm");
//			Process proc = pb.start();
//			is = proc.getInputStream();
//			ise = proc.getErrorStream();
//			os = proc.getOutputStream();
			// The command to run in a PTY...
			String[] cmd = { "/usr/bin/top" };
// The initial environment to pass to the PTY child process...
			String[] env = { "TERM=xterm","HOME="+System.getProperty("user.home") };

			pty = com.pty4j.PtyProcess.exec(cmd, env);

			os = pty.getOutputStream();
			is = pty.getInputStream();
			ise=pty.getErrorStream();
			connected = true;
			rv = true;
		}
		catch(java.io.IOException exp)
		{
			exp.printStackTrace();
		}
		return rv;
	}
	
	public void disconnect()
	{
		pty.destroyForcibly();
		connected=false;
	}

	public boolean isConnected()
	{
		return connected;
	}
	
	public boolean isClosed()
	{
		return !connected;
	}
	
	public int readByte() throws IOException
	{
		int r;

		if(ise.available()>0)
			r=ise.read();
		else
			r = is.read();
		if( r == -1 ) {
			// System.out.println("read -1 (EOF), disconnect().");
			throw new IOException();
		}
		return r;
	}

	public int readBytes(byte[] b) throws IOException
	{
		int r;

		if(ise.available()>0)
			r=ise.read(b);
		else
			r =  is.read( b );
		
		if( r == -1 ) {
			// System.out.println("read -1 (EOF), disconnect().");
			throw new IOException();
		}
		
		return r;
	}

	public int readBytes( byte[] b, int offset, int length ) throws IOException
	{
		int r;

		if(ise.available()>0)
			r=ise.read(b,offset,length);
		else
			r = is.read( b, offset, length );
		
		if( r == -1 ) {
			// System.out.println("read -1 (EOF), disconnect().");
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
		return new ProcessInOutInputStream( this );
	}
	
	public OutputStream getOutputStream()
	{
		return new ProcessInOutOutputStream( this );
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
	
	public ProcessInOut()
	{
		host = "localhost";
		port = -1;
		username = System.getProperty("user.name");
		
		connected = false;
	}
}
