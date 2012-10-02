package org.adblockplus;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * ChunkedOutputStream implements chunked HTTP transfer encoding wrapper for OutputStream.
 */
public class ChunkedOutputStream extends FilterOutputStream
{
	private static final byte[] CRLF = {'\r', '\n'};
	private static final byte[] FINAL_CHUNK = new byte[] {'0', '\r', '\n', '\r', '\n'};
	private boolean wroteFinalChunk = false;

	public ChunkedOutputStream(OutputStream out)
	{
		super(out);
	}

	@Override
	public void close() throws IOException
	{
		if (! wroteFinalChunk)
			writeFinalChunk();
		super.close();
	}

	@Override
	public void write(byte[] buffer, int offset, int length) throws IOException
	{
		writeChunk(buffer, offset, length);
	}

	@Override
	public void write(byte[] buffer) throws IOException
	{
		writeChunk(buffer, 0, buffer.length);
	}

	@Override
	public void write(int oneByte) throws IOException
	{
		throw new UnsupportedOperationException("Not implemented");
	}

	public void writeFinalChunk() throws IOException
	{
		out.write(FINAL_CHUNK);
		wroteFinalChunk = true;
	}
	
	private void writeChunk(byte buffer[], int offset, int length) throws IOException
	{
	    // Zero sized buffers are ok for slow connections but not in our case - zero chunk is used to indicate end of transfer.
	    if (length > 0)
	    {
	        // Write the chunk length as a hex number
	        writeHex(length);
	        // Write the data
            out.write(buffer, offset, length);
	        // Write a CRLF
	        out.write(CRLF);
	        // Flush the underlying stream
	        out.flush();
	    }
	}

	private void writeHex(int i) throws IOException
	{
		this.out.write(Integer.toHexString(i).getBytes());
		this.out.write(CRLF);
	}
}
