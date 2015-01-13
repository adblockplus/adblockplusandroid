/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-2015 Eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.adblockplus;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * ChunkedOutputStream implements chunked HTTP transfer encoding wrapper for
 * OutputStream.
 */
public class ChunkedOutputStream extends FilterOutputStream
{
  private static final int MAX_CHUNK_SIZE = 2048;

  private static final byte[] CRLF = {'\r', '\n'};
  private static final byte[] FINAL_CHUNK = new byte[] {'0', '\r', '\n', '\r', '\n'};
  private boolean wroteFinalChunk = false;

  public ChunkedOutputStream(final OutputStream out)
  {
    super(out);
  }

  @Override
  public void close() throws IOException
  {
    if (!wroteFinalChunk)
      writeFinalChunk();
    super.close();
  }

  @Override
  public void write(final byte[] buffer, final int offset, final int length) throws IOException
  {
    writeChunk(buffer, offset, length);
  }

  @Override
  public void write(final byte[] buffer) throws IOException
  {
    int offset = 0;
    int remain = buffer.length;
    while (remain > 0)
    {
      int size = MAX_CHUNK_SIZE;
      if (size > remain)
        size = remain;
      writeChunk(buffer, offset, size);
      offset += size;
      remain -= size;
    }
  }

  @Override
  public void write(final int oneByte) throws IOException
  {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void writeFinalChunk() throws IOException
  {
    out.write(FINAL_CHUNK);
    out.flush();
    wroteFinalChunk = true;
  }

  private void writeChunk(final byte buffer[], final int offset, final int length) throws IOException
  {
    // Zero sized buffers are ok on slow connections but not in our case - zero
    // chunk is used to indicate the end of transfer.
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

  private void writeHex(final int i) throws IOException
  {
    out.write(Integer.toHexString(i).getBytes());
    out.write(CRLF);
  }
}
