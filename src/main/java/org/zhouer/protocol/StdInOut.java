package org.zhouer.protocol;

import com.trilead.ssh2.InteractiveCallback;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

class StdInOutInputStream extends InputStream
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(StdInOutInputStream.class);
	private StdInOut stdInOut;

	public int read() throws IOException
	{
		return stdInOut.readByte();
	}

	public int read( byte[] buf ) throws IOException
	{
		return stdInOut.readBytes( buf );
	}

	public int read( byte[]buf, int offset, int length ) throws IOException
	{
		return stdInOut.readBytes( buf, offset, length );
	}

	public StdInOutInputStream( StdInOut tel )
	{
		stdInOut = tel;
	}
}

class StdInOutOutputStream extends OutputStream
{
	private StdInOut stdInOut;

	public void write( int b ) throws IOException
	{
		// java doc: The 24 high-order bits of b are ignored.
		stdInOut.writeByte( (byte)b );
	}

	public void write( byte[] buf ) throws IOException
	{
		stdInOut.writeBytes( buf );
	}

	public void write( byte[] buf, int offset, int length ) throws IOException
	{
		stdInOut.writeBytes( buf, offset, length );
	}

	public StdInOutOutputStream( StdInOut tel )
	{
		stdInOut = tel;
	}
}

public class StdInOut implements Protocol
{
	private final static org.apache.log4j.Logger CLASS_LOGGER = org.apache.log4j.Logger.getLogger(StdInOut.class);
	private String host;
	private int port;
	private String username;
	private String terminalType;

	private InputStream is;
	private InputStream ise;
	private OutputStream os;

	private java.io.InputStream stdin;
	private java.io.PrintStream stdout;
	private java.io.PrintStream stderr;

	private boolean connected;

	public boolean connect()
	{
		stdin = System.in;
		stdout = System.out;
		stderr = System.err;
		is=stdin;
		os=stdout;
		final int PIPE_BUFFER = 2048;

		try
		{
			PipedInputStream inPipe = new PipedInputStream(PIPE_BUFFER);
			PipedOutputStream outPipe = new PipedOutputStream(inPipe);
			System.setOut(new StdErrOutPrintStream(outPipe));
			is=inPipe;
			PipedInputStream inePipe = new PipedInputStream(PIPE_BUFFER);
			PipedOutputStream outePipe = new PipedOutputStream(inePipe);
			System.setErr(new StdErrOutPrintStream(outePipe));
			ise=inePipe;

		} catch (IOException e)
		{
			e.printStackTrace();
		}
		System.setIn(stdin);
		try
		{
// 2 -Create PipedInputStream with the buffer
			PipedInputStream inPipe = new PipedInputStream(PIPE_BUFFER);

// 3 -Create PipedOutputStream and bound it to the PipedInputStream object
			PipedOutputStream outPipe = new PipedOutputStream(inPipe);
			System.setIn(inPipe);
			os=outPipe;

		} catch (IOException e)
		{
			e.printStackTrace();
		}
		connected=true;
		return true;
	}
	
	public void disconnect()
	{
		java.io.InputStream is=System.in;
		java.io.OutputStream os=System.out;
		System.setIn(stdin);
		System.setOut(stdout);
		System.setErr(stderr);
		try
		{
			is.close();
			os.close();
		}
		catch(java.io.IOException exp)
		{
			exp.printStackTrace();
		}
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
			// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("read -1 (EOF), disconnect().");
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
			// if(CLASS_LOGGER.isTraceEnabled())CLASS_LOGGER.trace("read -1 (EOF), disconnect().");
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
		return new StdInOutInputStream( this );
	}
	
	public OutputStream getOutputStream()
	{
		return new StdInOutOutputStream( this );
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
	
	public StdInOut()
	{
		host = "localhost";
		port = -1;
		username = System.getProperty("user.name");
		
		connected = false;
	}
}
