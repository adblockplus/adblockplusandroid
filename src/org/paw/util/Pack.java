package org.paw.util;

import java.io.*;
import java.util.zip.*;

public class Pack {
	public static final byte[] zipData(byte[] data) {
		ByteArrayOutputStream baos = null;
		GZIPOutputStream gos = null;
		byte[] out = null;
		try {
			baos = new ByteArrayOutputStream();
			gos = new GZIPOutputStream(baos);
			gos.write(data);
			gos.finish();
			out = baos.toByteArray();
		}// of try
		catch (IOException ioe) {

			out = null;
			ioe.printStackTrace();
		} finally {
			try {
				if (gos != null)
					gos.close();
				if (baos != null)
					baos.close();
			} catch (Exception e) {
			}
			;
		}// of finally
		return out;
	}// of zipData method

	public static final byte[] unzipData(byte[] in) {
		ByteArrayInputStream bais = null;
		GZIPInputStream gis = null;
		ByteArrayOutputStream baos = null;
		byte[] buffer = new byte[1024];// for example only

		try {
			bais = new ByteArrayInputStream(in);
			gis = new GZIPInputStream(bais);
			baos = new ByteArrayOutputStream();

			int read = 0;

			while ((read = gis.read(buffer)) != -1)
				baos.write(buffer, 0, read);
		}// of try
		catch (Exception ioe) {
			buffer = null;
			ioe.printStackTrace();
			baos = null;

		} finally {
			try {
				if (gis != null)
					gis.close();
				if (bais != null)
					bais.close();
				if (baos != null)
					baos.close();
			} catch (Exception e) {}	;
		}

		return baos.toByteArray();
	}

	public static final byte[] compressData(byte[] uncompressedData) {

		// Create the compressor with highest level of compression
		Deflater compressor = new Deflater();
		compressor.setLevel(Deflater.BEST_COMPRESSION);

		// Give the compressor the data to compress
		compressor.setInput(uncompressedData);
		compressor.finish();

		// Create an expandable byte array to hold the compressed data.
		// You cannot use an array that's the same size as the orginal because
		// there is no guarantee that the compressed data will be smaller than
		// the uncompressed data.
		ByteArrayOutputStream bos = new ByteArrayOutputStream(
				uncompressedData.length);

		// Compress the data
		byte[] buf = new byte[1024];
		while (!compressor.finished()) {
			int count = compressor.deflate(buf);
			bos.write(buf, 0, count);
		}
		try {
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Get the compressed data
		byte[] compressedData = bos.toByteArray();

		return compressedData;

	}

	public static final byte[] deCompressData(byte[] compressedData) {
		// Create the decompressor and give it the data to compress
		Inflater decompressor = new Inflater();
		decompressor.setInput(compressedData);

		// Create an expandable byte array to hold the decompressed data
		ByteArrayOutputStream bos = new ByteArrayOutputStream(
				compressedData.length);

		// Decompress the data
		byte[] buf = new byte[1024];
		while (!decompressor.finished()) {
			try {
				int count = decompressor.inflate(buf);
				bos.write(buf, 0, count);
			} catch (DataFormatException e) {
				e.printStackTrace();
				break;
			}
		}
		try {
			bos.close();
		} catch (IOException e) {
		}

		// Get the decompressed data
		byte[] decompressedData = bos.toByteArray();

		return (decompressedData);

	}
}
